package com.example.detector

object NavitelDisplayDetector {

    private const val NAVITEL_SHORT_COMPONENT = "com.navitel/.app.MainActivity"
    private const val NAVITEL_FULL_COMPONENT = "com.navitel/com.navitel.app.MainActivity"

    // Regex to match Display headers in dumpsys output, e.g.:
    // "Display #21 (zone=...):"
    // "Display #23:"
    // "* Display #0"
    private val DISPLAY_HEADER_REGEX = Regex("""Display\s+#(\d+)""", RegexOption.IGNORE_CASE)

    /**
     * Parses the output of `dumpsys activity activities` to locate the display ID
     * currently hosting Navitel (either com.navitel/.app.MainActivity or com.navitel/com.navitel.app.MainActivity).
     *
     * Rules:
     * - Reads sequentially line by line.
     * - Updates currentDisplay whenever a "Display #N" line is reached.
     * - Returns currentDisplay as soon as a line matches Navitel's component.
     * - Does NOT guess or offset display IDs (no currentDisplay + 1).
     * - Ignores other applications like YouTube Car (com.github.slashmax.aabrowser).
     * - Returns null if the component is not found.
     */
    fun findNavitelDisplay(dumpsysOutput: String): Int? {
        if (dumpsysOutput.isBlank()) return null

        var currentDisplay: Int? = null

        val lines = dumpsysOutput.lineSequence()
        for (line in lines) {
            val displayMatch = DISPLAY_HEADER_REGEX.find(line)
            if (displayMatch != null) {
                currentDisplay = displayMatch.groupValues[1].toIntOrNull()
            }

            if (isNavitelComponentLine(line)) {
                if (currentDisplay != null) {
                    return currentDisplay
                }
            }
        }

        return null
    }

    /**
     * Checks if a line contains any representation of the Navitel Main Activity component.
     * This handles lines containing baseIntent, topActivity, mActivityComponent, realActivity, or Task Info.
     */
    fun isNavitelComponentLine(line: CharSequence): Boolean {
        return line.contains(NAVITEL_SHORT_COMPONENT) || line.contains(NAVITEL_FULL_COMPONENT)
    }

    /**
     * Helper to extract a summary of detected displays and their primary tasks for diagnostic logs.
     */
    fun extractDisplaySummary(dumpsysOutput: String): Map<Int, List<String>> {
        val result = mutableMapOf<Int, MutableList<String>>()
        var currentDisplay: Int? = null

        for (line in dumpsysOutput.lineSequence()) {
            val displayMatch = DISPLAY_HEADER_REGEX.find(line)
            if (displayMatch != null) {
                currentDisplay = displayMatch.groupValues[1].toIntOrNull()
                if (currentDisplay != null && !result.containsKey(currentDisplay)) {
                    result[currentDisplay] = mutableListOf()
                }
            }

            if (currentDisplay != null) {
                if (line.contains("cmp=") || line.contains("realActivity=") || line.contains("topActivity=")) {
                    val trimmed = line.trim()
                    if (result[currentDisplay]?.size ?: 0 < 5) {
                        result[currentDisplay]?.add(trimmed)
                    }
                }
            }
        }

        return result
    }
}
