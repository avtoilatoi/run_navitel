package com.example.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavitelDisplayDetectorTest {

    @Test
    fun testCase1_navitelAtDisplay21() {
        val dumpsys = """
            ACTIVITY MANAGER ACTIVITIES (dumpsys activity activities)
            Display #21 (zone=pip2):
              Stack #12:
                Task id #45
                  Task{3e89a45 #45 type=standard A=com.navitel U=0 visible=true mode=fullscreen}
                  * Hist #0: ActivityRecord{9b4221d u0 com.navitel/.app.MainActivity t45}
                    resultTo=null resultWho=null req=0
        """.trimIndent()

        val displayId = NavitelDisplayDetector.findNavitelDisplay(dumpsys)
        assertEquals(21, displayId)
    }

    @Test
    fun testCase2_navitelAtDisplay23() {
        val dumpsys = """
            Display #0 (main):
              Task id #1
                Task{1122334 #1 type=home}
            Display #23 (zone=car_pip_right):
              Task id #89
                mActivityComponent=com.navitel/.app.MainActivity
                topActivity=ComponentInfo{com.navitel/.app.MainActivity}
        """.trimIndent()

        val displayId = NavitelDisplayDetector.findNavitelDisplay(dumpsys)
        assertEquals(23, displayId)
    }

    @Test
    fun testCase3_youtubeAtDisplay22_and_navitelAtDisplay23() {
        val dumpsys = """
            Display #0 (main dashboard):
              Task id #10 cmp=com.leco.launcher/.MainActivity
            Display #22 (pip1):
              Task id #20
                realActivity=com.github.slashmax.aabrowser/.MainActivity
                baseIntent={act=android.intent.action.MAIN cmp=com.github.slashmax.aabrowser/.MainActivity}
            Display #23 (pip2):
              Task id #21
                realActivity=com.navitel/.app.MainActivity
                baseIntent={act=android.intent.action.MAIN cmp=com.navitel/.app.MainActivity}
        """.trimIndent()

        val displayId = NavitelDisplayDetector.findNavitelDisplay(dumpsys)
        assertEquals(23, displayId)
    }

    @Test
    fun testCase4_noNavitelTask() {
        val dumpsys = """
            Display #0:
              Task id #1 cmp=com.android.launcher3/.Launcher
            Display #21:
              Task id #2 cmp=com.github.slashmax.aabrowser/.MainActivity
            Display #22:
              Task id #3 cmp=com.google.android.maps/.MapsActivity
        """.trimIndent()

        val displayId = NavitelDisplayDetector.findNavitelDisplay(dumpsys)
        assertNull(displayId)
    }

    @Test
    fun testCase5_componentInShortForm() {
        val dumpsys = """
            Display #21 (pip_nav):
              Task id #50
                * Task{a87b1c #50 type=standard A=com.navitel U=0}
                  * Hist #0: ActivityRecord{452aa u0 com.navitel/.app.MainActivity t50}
        """.trimIndent()

        val displayId = NavitelDisplayDetector.findNavitelDisplay(dumpsys)
        assertEquals(21, displayId)
    }

    @Test
    fun testCase6_componentInFullForm() {
        val dumpsys = """
            Display #21 (pip_nav):
              Task id #51
                * Task{b98c2d #51 type=standard A=com.navitel U=0}
                  * Hist #0: ActivityRecord{763bb u0 com.navitel/com.navitel.app.MainActivity t51}
        """.trimIndent()

        val displayId = NavitelDisplayDetector.findNavitelDisplay(dumpsys)
        assertEquals(21, displayId)
    }

    @Test
    fun testCase7_multipleDisplaysAndMultipleTasks() {
        val dumpsys = """
            ACTIVITY MANAGER ACTIVITIES (dumpsys activity activities)
            Display #0 (type=INTERNAL):
              Stack #0:
                Task id #1 cmp=com.leco.launcher/.LauncherActivity
                Task id #2 cmp=com.android.settings/.Settings
            Display #19 (type=VIRTUAL):
              Stack #1:
                Task id #15 cmp=com.spotify.music/.MainActivity
            Display #22 (type=VIRTUAL):
              Stack #2:
                Task id #25 cmp=com.github.slashmax.aabrowser/.MainActivity
            Display #23 (type=VIRTUAL):
              Stack #3:
                Task id #30 cmp=com.navitel/com.navitel.app.MainActivity
            Display #25 (type=VIRTUAL):
              Stack #4:
                Task id #35 cmp=com.google.android.apps.maps/.MapsActivity
        """.trimIndent()

        val displayId = NavitelDisplayDetector.findNavitelDisplay(dumpsys)
        assertEquals(23, displayId)
    }

    @Test
    fun testCase8_navitelInBaseIntentTopActivityOrMActivityComponent() {
        val dumpsys1 = """
            Display #21:
              Task id #41
                baseIntent={act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10000000 cmp=com.navitel/.app.MainActivity}
        """.trimIndent()

        val dumpsys2 = """
            Display #21:
              Task id #42
                topActivity=ComponentInfo{com.navitel/com.navitel.app.MainActivity}
        """.trimIndent()

        val dumpsys3 = """
            Display #21:
              Task id #43
                mActivityComponent=com.navitel/.app.MainActivity
        """.trimIndent()

        assertEquals(21, NavitelDisplayDetector.findNavitelDisplay(dumpsys1))
        assertEquals(21, NavitelDisplayDetector.findNavitelDisplay(dumpsys2))
        assertEquals(21, NavitelDisplayDetector.findNavitelDisplay(dumpsys3))
    }
}
