package com.creativeblisslabs.omnichessreader.feature.reader.chess

import kotlin.math.abs
import kotlin.math.roundToInt

data class PixelBox(
    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int
) {
    val width: Int get() = maxOf(0, x1 - x0)
    val height: Int get() = maxOf(0, y1 - y0)
}

data class GrayImage(
    val width: Int,
    val height: Int,
    val data: ByteArray // 0 is black, 255 is white (unsigned byte value: data[i].toInt() and 0xFF)
) {
    fun pixel(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 255
        return data[y * width + x].toInt() and 0xFF
    }
}

object LivingChessVision {

    private const val DARK = 110
    private const val MIN_BOARD_FRACTION = 0.1f
    private const val MAX_BOARD_FRACTION = 0.9f
    private const val MIN_LINE_FILL = 0.85f
    private const val SQUARE_TOLERANCE = 0.08f
    private const val FRAME_FILL = 0.6f
    private const val MIN_INNER_CONTRAST = 0.05f
    private const val MAX_INNER_INK = 0.75f
    const val PIECE_SEED_SIZE = 20

    data class Segment(val y: Int, var x0: Int, var x1: Int)

    /**
     * Calculates ink density within a bounding box.
     */
    fun inkInBox(image: GrayImage, box: PixelBox, threshold: Int = 110): Float {
        val x0 = box.x0.coerceIn(0, image.width)
        val y0 = box.y0.coerceIn(0, image.height)
        val x1 = box.x1.coerceIn(0, image.width)
        val y1 = box.y1.coerceIn(0, image.height)
        if (x1 <= x0 || y1 <= y0) return 0f

        var dark = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                if (image.pixel(x, y) < threshold) dark++
            }
        }
        return dark.toFloat() / ((x1 - x0) * (y1 - y0)).toFloat()
    }

    private fun findHorizontalLines(image: GrayImage, minLength: Int): List<Segment> {
        val raw = mutableListOf<Segment>()
        for (y in 0 until image.height) {
            var runStart = -1
            var gap = 0
            var lastDark = -1
            for (x in 0..image.width) {
                val dark = x < image.width && image.pixel(x, y) < DARK
                if (dark) {
                    if (runStart < 0) runStart = x
                    lastDark = x
                    gap = 0
                } else if (runStart >= 0 && ++gap > 3) {
                    if (lastDark - runStart + 1 >= minLength) {
                        raw.add(Segment(y, runStart, lastDark + 1))
                    }
                    runStart = -1
                    gap = 0
                }
            }
        }

        val merged = mutableListOf<Segment>()
        for (line in raw) {
            val same = merged.find { m ->
                line.y - m.y <= 6 && abs(line.x0 - m.x0) <= 6 && abs(line.x1 - m.x1) <= 6
            }
            if (same != null) {
                same.x0 = minOf(same.x0, line.x0)
                same.x1 = maxOf(same.x1, line.x1)
            } else {
                merged.add(line.copy())
            }
        }
        return merged
    }

    private fun columnFill(image: GrayImage, x: Int, y0: Int, y1: Int): Float {
        var dark = 0
        for (y in y0 until y1) {
            if (image.pixel(x, y) < DARK) dark++
        }
        return dark.toFloat() / maxOf(1, y1 - y0).toFloat()
    }

    private fun hasVerticalEdge(image: GrayImage, x: Int, y0: Int, y1: Int): Boolean {
        for (dx in -4..4) {
            val col = x + dx
            if (col in 0 until image.width && columnFill(image, col, y0, y1) >= FRAME_FILL) return true
        }
        return false
    }

    private fun looksLikeBoard(image: GrayImage, box: PixelBox): Boolean {
        var dark = 0
        var total = 0
        val inset = ((box.x1 - box.x0) * 0.04f).roundToInt()
        for (y in (box.y0 + inset) until (box.y1 - inset) step 2) {
            for (x in (box.x0 + inset) until (box.x1 - inset) step 2) {
                if (image.pixel(x, y) < DARK) dark++
                total++
            }
        }
        val share = dark.toFloat() / maxOf(1, total).toFloat()
        return share in MIN_INNER_CONTRAST..MAX_INNER_INK
    }

    /**
     * Detects chess diagrams in a grayscale page rendering by identifying square frame borders.
     */
    fun detectBoards(image: GrayImage): List<PixelBox> {
        val shortSide = minOf(image.width, image.height)
        val minSize = (shortSide * MIN_BOARD_FRACTION).roundToInt()
        val maxSize = (shortSide * MAX_BOARD_FRACTION).roundToInt()
        val lines = findHorizontalLines(image, (minSize * MIN_LINE_FILL).roundToInt())

        val boards = mutableListOf<PixelBox>()
        for (i in lines.indices) {
            val top = lines[i]
            if (boards.any { top.y in it.y0..it.y1 && top.x0 < it.x1 && top.x1 > it.x0 }) continue
            val size = top.x1 - top.x0
            if (size < minSize || size > maxSize) continue

            for (j in (i + 1) until lines.size) {
                val bottom = lines[j]
                val height = bottom.y - top.y + 1
                if (height > size * (1 + SQUARE_TOLERANCE)) break
                if (height < size * (1 - SQUARE_TOLERANCE)) continue
                if (abs(bottom.x0 - top.x0) > size * 0.03f || abs(bottom.x1 - top.x1) > size * 0.03f) continue

                val box = PixelBox(top.x0, top.y, top.x1, bottom.y + 1)
                if (
                    hasVerticalEdge(image, box.x0, box.y0, box.y1) &&
                    hasVerticalEdge(image, box.x1 - 1, box.y0, box.y1) &&
                    looksLikeBoard(image, box)
                ) {
                    boards.add(box)
                    break
                }
            }
        }
        return boards
    }

    /**
     * Slices a detected board into 64 normalized 20x20 tile pixel arrays.
     */
    fun sliceBoard(image: GrayImage, box: PixelBox): List<FloatArray> {
        val inset = maxOf(3, ((box.x1 - box.x0) * 0.012f).roundToInt())
        val x0 = box.x0 + inset
        val y0 = box.y0 + inset
        val cellW = (box.x1 - box.x0 - inset * 2).toFloat() / 8f
        val cellH = (box.y1 - box.y0 - inset * 2).toFloat() / 8f

        val tiles = mutableListOf<FloatArray>()
        for (rank in 0 until 8) {
            for (file in 0 until 8) {
                val tile = FloatArray(PIECE_SEED_SIZE * PIECE_SEED_SIZE)
                val left = x0 + file * cellW
                val top = y0 + rank * cellH
                for (y in 0 until PIECE_SEED_SIZE) {
                    val fromY = (top + (y * cellH) / PIECE_SEED_SIZE).toInt()
                    val toY = maxOf(fromY + 1, (top + ((y + 1) * cellH) / PIECE_SEED_SIZE).toInt())
                    for (x in 0 until PIECE_SEED_SIZE) {
                        val fromX = (left + (x * cellW) / PIECE_SEED_SIZE).toInt()
                        val toX = maxOf(fromX + 1, (left + ((x + 1) * cellW) / PIECE_SEED_SIZE).toInt())
                        var sum = 0
                        var count = 0
                        for (py in fromY until minOf(toY, image.height)) {
                            for (px in fromX until minOf(toX, image.width)) {
                                sum += image.pixel(px, py)
                                count++
                            }
                        }
                        tile[y * PIECE_SEED_SIZE + x] = if (count > 0) sum.toFloat() / count.toFloat() else 255f
                    }
                }
                tiles.add(tile)
            }
        }
        return tiles
    }
}

