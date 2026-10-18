package com.creativeblisslabs.omnimind.chess.engine

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.creativeblisslabs.omnimind.core.data.ai.ModelPackManager
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelProfile
import com.creativeblisslabs.omnimind.core.chess.ChessLogic
import com.creativeblisslabs.omnimind.core.chess.EngineVersion
import com.creativeblisslabs.omnimind.core.chess.ChessEngine
import com.creativeblisslabs.omnimind.core.chess.EngineEvaluation
import com.creativeblisslabs.omnimind.core.chess.MultiPVLine
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

class StockfishEngine(
    private val context: Context,
    private val modelPackManager: ModelPackManager
) : ChessEngine {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var currentSkillLevel = 20
    private var currentEngineVersion = EngineVersion.SF17

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private val outputBuffer = mutableListOf<String>()
    private val engineMutex = Mutex()

    override fun switchVersion(version: EngineVersion) {
        currentEngineVersion = version
        scope.launch {
            engineMutex.withLock {
                if (process?.isAlive == true) {
                    stopInternal()
                    startInternal(version)
                }
            }
        }
    }

    override fun start() {
        scope.launch {
            engineMutex.withLock {
                startInternal(currentEngineVersion)
            }
        }
    }

    private suspend fun startInternal(version: EngineVersion) {
        currentEngineVersion = version
        if (process == null || process?.isAlive == false) {
            var engineFile: File? = null

            // 1. Check ModelPackManager packs
            val packs = modelPackManager.allPacks.first()
            val sfPack = packs.find { it.profile == LocalModelProfile.Stockfish17 }
            if (sfPack?.isDownloaded == true && sfPack.filePath.isNotEmpty()) {
                val f = File(sfPack.filePath)
                if (f.exists() && isElfBinary(f)) {
                    engineFile = f
                }
            }

            // 2. Fallback: Direct check of persistent models/Stockfish17.bin
            if (engineFile == null || !engineFile.exists() || !isElfBinary(engineFile)) {
                val modelsDir = context.filesDir.resolve("models")
                val directBin = File(modelsDir, "${LocalModelProfile.Stockfish17.name}.bin")
                if (directBin.exists() && isElfBinary(directBin)) {
                    engineFile = directBin
                } else if (modelsDir.exists()) {
                    engineFile = modelsDir.walkTopDown().find { !it.isDirectory && isElfBinary(it) }
                }
            }

            // 3. Fallback: Check codeCacheDir
            if (engineFile == null || !engineFile.exists() || !isElfBinary(engineFile)) {
                val codeCacheBin = context.codeCacheDir.resolve("Stockfish17.bin")
                if (codeCacheBin.exists() && isElfBinary(codeCacheBin)) {
                    engineFile = codeCacheBin
                }
            }

            // 4. Trigger auto refresh if binary still not found
            if (engineFile == null || !engineFile.exists()) {
                modelPackManager.refresh()
                val refreshedPacks = modelPackManager.allPacks.first()
                val refreshedSfPack = refreshedPacks.find { it.profile == LocalModelProfile.Stockfish17 }
                if (refreshedSfPack?.isDownloaded == true && refreshedSfPack.filePath.isNotEmpty()) {
                    val f = File(refreshedSfPack.filePath)
                    if (f.exists() && isElfBinary(f)) {
                        engineFile = f
                    }
                }
            }

            if (engineFile != null && engineFile.exists()) {
                withContext(Dispatchers.IO) {
                    try {
                        val targetExecutable = getExecutableBinaryFile(engineFile)
                        val proc = try {
                            ProcessBuilder(targetExecutable.absolutePath).redirectErrorStream(true).start()
                        } catch (e: Exception) {
                            android.util.Log.w("StockfishEngine", "Primary launch failed, attempting cacheDir fallback: ${e.message}")
                            val fallback = context.cacheDir.resolve("Stockfish17.bin")
                            engineFile.copyTo(fallback, overwrite = true)
                            fallback.setExecutable(true, false)
                            try { Runtime.getRuntime().exec(arrayOf("chmod", "755", fallback.absolutePath)).waitFor() } catch (_: Exception) {}
                            ProcessBuilder(fallback.absolutePath).redirectErrorStream(true).start()
                        }

                        process = proc
                        writer = proc.outputStream.bufferedWriter()
                        reader = proc.inputStream.bufferedReader()

                        // Reader loop for engine output
                        scope.launch(Dispatchers.IO) {
                            try {
                                while (isActive && process?.isAlive == true) {
                                    val line = reader?.readLine() ?: break
                                    synchronized(outputBuffer) {
                                        outputBuffer.add(line)
                                        if (outputBuffer.size > 500) outputBuffer.removeAt(0)
                                    }
                                }
                            } catch (e: Exception) { /* Process terminated */ }
                        }

                        sendCommandInternal("uci")
                        sendCommandInternal("setoption name Skill Level value $currentSkillLevel")
                        sendCommandInternal("isready")
                        android.util.Log.i("StockfishEngine", "Successfully launched Stockfish engine process: ${targetExecutable.absolutePath}")
                    } catch (e: Exception) {
                        android.util.Log.e("StockfishEngine", "Failed to launch Stockfish process: ${e.localizedMessage}", e)
                    }
                }
            } else {
                android.util.Log.w("StockfishEngine", "Stockfish 17 pack not downloaded or binary path missing")
            }
        }
    }

    override fun stop() {
        scope.launch {
            engineMutex.withLock {
                stopInternal()
            }
        }
    }

    private fun stopInternal() {
        try {
            sendCommandInternal("quit")
            writer?.close()
            reader?.close()
            process?.destroy()
        } catch (e: Exception) { /* ignore */ }
        finally {
            process = null
            writer = null
            reader = null
        }
    }

    override fun setSkillLevel(level: Int) {
        currentSkillLevel = level.coerceIn(0, 20)
        scope.launch {
            engineMutex.withLock {
                sendCommandInternal("setoption name Skill Level value $currentSkillLevel")
            }
        }
    }

    private var currentFen = ChessLogic.getInitialFen()

    override fun setPosition(fen: String) {
        currentFen = if (fen == "startpos") ChessLogic.getInitialFen() else fen
        scope.launch {
            if (fen == "startpos") {
                sendCommandInternal("position startpos")
            } else {
                sendCommandInternal("position fen $fen")
            }
        }
    }

    override fun analyze(depth: Int, onResult: (EngineEvaluation) -> Unit) {
        scope.launch {
            engineMutex.withLock {
                if (process?.isAlive != true) {
                    startInternal(currentEngineVersion)
                }

                if (process?.isAlive != true) {
                    withContext(Dispatchers.Main) {
                        onResult(EngineEvaluation(0.0, emptyList(), 0))
                    }
                    return@withLock
                }

                sendCommandInternal("stop")
                synchronized(outputBuffer) { outputBuffer.clear() }

                if (currentFen == "startpos" || currentFen == ChessLogic.getInitialFen()) {
                    sendCommandInternal("position startpos")
                } else {
                    sendCommandInternal("position fen $currentFen")
                }

                sendCommandInternal("setoption name MultiPV value 3")
                sendCommandInternal("go depth $depth")

                val lines = mutableListOf<String>()
                val startTime = System.currentTimeMillis()
                val maxWaitMs = (depth * 150).coerceIn(400, 2000)

                var latestPvMap = mutableMapOf<Int, MultiPVLine>()
                var latestScore = 0.0
                var latestMate: Int? = null
                var latestMainLine = listOf<String>()
                var currentMaxDepth = 1

                while (System.currentTimeMillis() - startTime < maxWaitMs) {
                    val newLines = synchronized(outputBuffer) {
                        val snapshot = outputBuffer.toList()
                        outputBuffer.clear()
                        snapshot
                    }

                    if (newLines.isNotEmpty()) {
                        lines.addAll(newLines)
                        newLines.forEach { line ->
                            if (line.startsWith("info") && line.contains(" pv ")) {
                                val depthMatch = Regex("depth (\\d+)").find(line)
                                val parsedDepth = depthMatch?.groupValues?.get(1)?.toIntOrNull() ?: currentMaxDepth
                                if (parsedDepth >= currentMaxDepth) {
                                    currentMaxDepth = parsedDepth
                                }

                                val pvIndex = line.indexOf(" pv ")
                                val pvMoves = line.substring(pvIndex + 4).split(" ").filter { it.isNotBlank() }

                                val scoreMatch = Regex("score (cp|mate) (-?\\d+)").find(line)
                                val scoreType = scoreMatch?.groupValues?.get(1)
                                val scoreVal = scoreMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

                                val currentScore = if (scoreType == "cp") scoreVal / 100.0 else 0.0
                                val currentMate = if (scoreType == "mate") scoreVal else null

                                val multiPvMatch = Regex("multipv (\\d+)").find(line)
                                val pvRank = multiPvMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

                                val pvLine = MultiPVLine(currentScore, pvMoves, currentMate)
                                latestPvMap[pvRank] = pvLine

                                if (pvRank == 1) {
                                    latestScore = currentScore
                                    latestMate = currentMate
                                    latestMainLine = pvMoves
                                }
                            }
                        }

                        if (latestPvMap.isNotEmpty()) {
                            val liveEval = EngineEvaluation(
                                score = latestScore,
                                bestLine = latestMainLine,
                                depth = currentMaxDepth,
                                mate = latestMate,
                                multiPV = latestPvMap.values.sortedByDescending { it.score }
                            )
                            withContext(Dispatchers.Main) {
                                onResult(liveEval)
                            }
                        }

                        if (newLines.any { it.startsWith("bestmove") }) break
                    } else {
                        delay(15)
                    }
                }

                if (latestPvMap.isEmpty()) {
                    val state = ChessLogic.parseFen(currentFen)
                    val result = ChessLogic.minimax(currentFen, 4, -1000000.0, 1000000.0, state.isWhiteTurn)
                    val fallbackLine = listOfNotNull(result.second)
                    withContext(Dispatchers.Main) {
                        onResult(EngineEvaluation(result.first / 10.0, fallbackLine, 4, null, listOf(MultiPVLine(result.first / 10.0, fallbackLine))))
                    }
                }
            }
        }
    }

    override fun getBestMove(depth: Int, skillLevel: Int, onMove: (String) -> Unit) {
        scope.launch {
            if (process?.isAlive != true) {
                startInternal(currentEngineVersion)
            }

            if (process?.isAlive == true) {
                try {
                    sendCommandInternal("stop")
                    synchronized(outputBuffer) { outputBuffer.clear() }

                    // Lichess StockfishLevel exact skill & MultiPV candidate pool mapping
                    val uciSkill = (skillLevel - 1).coerceIn(0, 20)
                    val multiPvPool = when {
                        skillLevel <= 2 -> 8
                        skillLevel <= 4 -> 6
                        skillLevel <= 6 -> 5
                        else -> 4
                    }
                    val timeLimitMs = when {
                        skillLevel <= 2 -> 350L
                        skillLevel <= 5 -> 500L
                        skillLevel <= 8 -> 750L
                        else -> 1000L
                    }

                    sendCommandInternal("setoption name Skill Level value $uciSkill")
                    sendCommandInternal("setoption name MultiPV value $multiPvPool")
                    if (currentFen == "startpos" || currentFen == ChessLogic.getInitialFen()) {
                        sendCommandInternal("position startpos")
                    } else {
                        sendCommandInternal("position fen $currentFen")
                    }
                    sendCommandInternal("go movetime $timeLimitMs")

                    var bestMove = ""
                    val startTime = System.currentTimeMillis()

                    while (System.currentTimeMillis() - startTime < timeLimitMs + 800) {
                        val line = synchronized(outputBuffer) {
                            if (outputBuffer.isNotEmpty()) outputBuffer.removeAt(0) else null
                        }
                        if (line?.startsWith("bestmove") == true) {
                            bestMove = line.split(" ").getOrElse(1) { "" }
                            break
                        }
                        if (line == null) delay(20)
                    }

                    if (bestMove.isNotEmpty() && bestMove != "(none)") {
                        withContext(Dispatchers.Main) { onMove(bestMove) }
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.e("StockfishEngine", "Process getBestMove error: ${e.message}")
                }
            }

            // Robust fallback via Kotlin minimax engine & random legal move generator
            val state = ChessLogic.parseFen(currentFen)
            val fallbackResult = ChessLogic.minimax(currentFen, 3, -100000.0, 100000.0, state.isWhiteTurn)
            val fallbackMove = fallbackResult.second ?: ChessLogic.generateMoves(currentFen).randomOrNull() ?: ""
            android.util.Log.i("StockfishEngine", "Generated fallback move for $currentFen: $fallbackMove")
            withContext(Dispatchers.Main) { onMove(fallbackMove) }
        }
    }

    private fun sendCommandInternal(cmd: String) {
        try {
            writer?.let {
                it.write("$cmd\n")
                it.flush()
            }
        } catch (e: Exception) {
            android.util.Log.e("StockfishEngine", "Failed to send command: $cmd", e)
        }
    }

    private fun getExecutableBinaryFile(sourceFile: File): File {
        val target = context.codeCacheDir.resolve("libstockfish.so")
        try {
            if (!target.exists() || target.length() != sourceFile.length()) {
                sourceFile.copyTo(target, overwrite = true)
            }
            target.setExecutable(true, false)
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", target.absolutePath)).waitFor()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            android.util.Log.w("StockfishEngine", "Failed copying binary to codeCacheDir: ${e.message}")
            sourceFile.setExecutable(true, false)
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "755", sourceFile.absolutePath)).waitFor()
            } catch (_: Exception) {}
            return sourceFile
        }
        return target
    }

    private fun isElfBinary(file: File?): Boolean {
        if (file == null || !file.exists() || file.isDirectory || file.length() < 10 * 1024) return false
        return try {
            file.inputStream().buffered().use { stream ->
                val header = ByteArray(4)
                val count = stream.read(header)
                count == 4 && header[0] == 0x7F.toByte() && header[1] == 'E'.toByte() && header[2] == 'L'.toByte() && header[3] == 'F'.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }
}
