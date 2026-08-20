package com.kidsexplore.app

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The theme drawables are generated from the SVGs in `icons-src` by
 * `tools/svg2vd.py`.
 * Nothing stops someone editing a generated file by hand, or updating an SVG and
 * forgetting to re-run the converter — and either leaves the committed artwork
 * quietly disagreeing with its source.
 *
 * A plain JVM test rather than an instrumented one, so it runs in `./gradlew
 * build` without a device.
 */
class IconsInSyncTest {

    // Unit tests run with the module directory as the working directory.
    private val repoRoot = File("..").canonicalFile

    @Test
    fun generatedDrawablesMatchTheirSourceSvgs() {
        val script = File(repoRoot, "tools/svg2vd.py")
        assumeTrue("converter not present", script.isFile)
        assumeTrue("python3 not on PATH", which("python3") != null)

        val process = ProcessBuilder(
            "python3", script.path, "icons-src", "app/src/main/res/drawable", "--check",
        ).directory(repoRoot).redirectErrorStream(true).start()

        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(2, TimeUnit.MINUTES)

        assertEquals(
            "Drawables are out of sync with icons-src. Re-run:\n" +
                "  python3 tools/svg2vd.py icons-src app/src/main/res/drawable\n\n$output",
            0,
            process.exitValue(),
        )
    }

    private fun which(cmd: String): File? =
        System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .map { File(it, cmd) }
            .firstOrNull { it.canExecute() }
}
