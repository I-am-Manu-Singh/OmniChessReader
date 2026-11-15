package com.creativeblisslabs.omnichessreader.feature.reader.chess

import kotlinx.serialization.Serializable

@Serializable
data class StudyAnchor(
    val page: Int,
    val fen: String? = null,
    val bookFen: String? = null,
    val moveId: String? = null,
    val diagramId: String? = null,
    val bookPage: Int? = null,
    val label: String? = null
)

enum class StudyEntryKind {
    NOTE,
    POSITION,
    COACH
}

@Serializable
data class StudyNotebookEntry(
    val id: String,
    val kind: StudyEntryKind,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
    val anchor: StudyAnchor,
    val question: String? = null,
    val model: String? = null,
    val highlightBoxes: List<PdfBox> = emptyList(),
    val wasMissed: Boolean = false
)

@Serializable
data class StudyNotebook(
    val schemaVersion: Int = 1,
    val bookHash: String = "",
    val entries: List<StudyNotebookEntry> = emptyList(),
    val solvedDiagrams: List<String> = emptyList()
)

object LivingChessNotebook {

    fun samePosition(a: String?, b: String?): Boolean {
        if (a == null || b == null) return false
        val partsA = a.split(" ").take(4).joinToString(" ")
        val partsB = b.split(" ").take(4).joinToString(" ")
        return partsA == partsB
    }

    fun addEntry(notebook: StudyNotebook, entry: StudyNotebookEntry): StudyNotebook {
        return notebook.copy(entries = notebook.entries + entry)
    }

    fun removeEntry(notebook: StudyNotebook, id: String): StudyNotebook {
        return notebook.copy(entries = notebook.entries.filter { it.id != id })
    }

    fun markDiagramSolved(notebook: StudyNotebook, diagramKey: String): StudyNotebook {
        if (notebook.solvedDiagrams.contains(diagramKey)) return notebook
        return notebook.copy(solvedDiagrams = notebook.solvedDiagrams + diagramKey)
    }

    /**
     * Exports notebook notes and positions into clean study Markdown.
     */
    fun exportToMarkdown(notebook: StudyNotebook, bookTitle: String): String {
        val sb = StringBuilder()
        sb.appendLine("# Chess Study Notebook - $bookTitle")
        sb.appendLine()

        val sorted = notebook.entries.sortedWith(compareBy({ it.anchor.page }, { it.createdAt }))
        for (entry in sorted) {
            val pageNum = entry.anchor.page + 1
            val label = entry.anchor.label ?: "Position"
            val isCustomMoves = entry.anchor.fen != null && !samePosition(entry.anchor.fen, entry.anchor.bookFen)
            val subheader = listOfNotNull(
                "Page $pageNum",
                label,
                if (isCustomMoves) "(Tried Own Moves)" else null,
                if (entry.kind == StudyEntryKind.POSITION) "★ Saved Position" else null
            ).joinToString(" · ")

            sb.appendLine("## $subheader")
            sb.appendLine()
            if (entry.anchor.fen != null) {
                sb.appendLine("`FEN: ${entry.anchor.fen}`")
                sb.appendLine()
            }
            if (entry.kind == StudyEntryKind.COACH) {
                if (!entry.question.isNullOrBlank()) {
                    sb.appendLine("**Question:** ${entry.question}")
                    sb.appendLine()
                }
                sb.appendLine("**Coach:** ${entry.text}")
            } else if (entry.text.isNotBlank()) {
                sb.appendLine(entry.text)
            }
            sb.appendLine()
        }
        return sb.toString()
    }
}

