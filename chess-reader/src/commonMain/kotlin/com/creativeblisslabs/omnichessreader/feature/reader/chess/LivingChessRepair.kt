package com.creativeblisslabs.omnichessreader.feature.reader.chess

/**
 * LivingChessRepair — ported from living-chess-book (src/core/repair.ts).
 * Finds illegible or broken move sequences in parsed chess books and prepares
 * targeted re-reading / repair jobs for LLM or vision re-analysis.
 */
data class JobRegion(
    val page: Int,
    val box: PdfBox
)

data class JobToken(
    val to: String?,
    val castle: String?, // "k", "q", or null
    val page: Int,
    val box: PdfBox
)

data class RepairJob(
    val id: String,
    val diagramId: String?,
    val label: String?,
    val startFens: List<String>,
    val regions: List<JobRegion>,
    val tokens: List<JobToken>,
    val moveIds: List<String>
)

data class FoundJobs(
    val jobs: List<RepairJob>,
    val droppedAcrossPages: Int = 0
)

object LivingChessRepair {
    const val MIN_JOB_TOKENS = 3
    const val MAX_REGIONS_PER_JOB = 3
    const val REGION_MARGIN = 8f

    fun findBrokenJobs(
        overlay: LivingBookOverlay,
        brokenMoveIds: Set<String>
    ): FoundJobs {
        if (brokenMoveIds.isEmpty()) return FoundJobs(emptyList(), 0)

        val jobs = mutableListOf<RepairJob>()
        var droppedCount = 0

        // Cluster consecutive broken moves into repair jobs
        val jobNodes = overlay.moves.filter { brokenMoveIds.contains(it.id) }
        if (jobNodes.isEmpty()) return FoundJobs(emptyList(), 0)

        val clustered = mutableListOf<MutableList<BookMoveNode>>()
        var currentCluster = mutableListOf<BookMoveNode>()

        jobNodes.forEach { node ->
            if (currentCluster.isEmpty()) {
                currentCluster.add(node)
            } else {
                val last = currentCluster.last()
                if (node.page == last.page || node.page == last.page + 1) {
                    currentCluster.add(node)
                } else {
                    clustered.add(currentCluster)
                    currentCluster = mutableListOf(node)
                }
            }
        }
        if (currentCluster.isNotEmpty()) clustered.add(currentCluster)

        clustered.forEachIndexed { idx, cluster ->
            val pages = cluster.map { it.page }.distinct()
            if (pages.size > MAX_REGIONS_PER_JOB) {
                droppedCount++
                return@forEachIndexed
            }

            val regions = pages.map { p ->
                val nodesOnPage = cluster.filter { it.page == p }
                val minX = nodesOnPage.minOf { it.box.x0 } - REGION_MARGIN
                val minY = nodesOnPage.minOf { it.box.y0 } - REGION_MARGIN
                val maxX = nodesOnPage.maxOf { it.box.x1 } + REGION_MARGIN
                val maxY = nodesOnPage.maxOf { it.box.y1 } + REGION_MARGIN
                JobRegion(p, PdfBox(minX.coerceAtLeast(0f), minY.coerceAtLeast(0f), maxX, maxY))
            }

            val tokens = cluster.map { node ->
                JobToken(
                    to = node.san.takeLast(2),
                    castle = if (node.san.contains("O-O-O")) "q" else if (node.san.contains("O-O")) "k" else null,
                    page = node.page,
                    box = node.box
                )
            }

            jobs.add(
                RepairJob(
                    id = "repair_job_$idx",
                    diagramId = null,
                    label = "Line repair at page ${pages.first()}",
                    startFens = listOf(cluster.first().fen),
                    regions = regions,
                    tokens = tokens,
                    moveIds = cluster.map { it.id }
                )
            )
        }

        return FoundJobs(jobs, droppedCount)
    }
}

