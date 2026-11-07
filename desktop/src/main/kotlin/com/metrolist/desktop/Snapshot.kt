/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import javax.swing.SwingUtilities

/**
 * Developer aid: renders [content] off-screen for [seconds], letting network content load, then writes a PNG.
 * Run with `--snapshot <file.png>` to check the desktop UI without opening a window.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun renderSnapshot(
    output: File,
    seconds: Int = 20,
    content: @Composable () -> Unit,
) {
    // Compose and navigation expect the Swing thread; sleeping happens off it so main-dispatched work can run.
    lateinit var scene: ImageComposeScene
    lateinit var image: Image
    val start = System.nanoTime()
    SwingUtilities.invokeAndWait {
        scene = ImageComposeScene(width = 1100, height = 760, density = Density(1f), content = content)
        image = scene.render(0)
    }
    repeat(seconds * 4) {
        Thread.sleep(250)
        SwingUtilities.invokeAndWait { image = scene.render(System.nanoTime() - start) }
    }
    output.writeBytes(checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
    SwingUtilities.invokeAndWait { scene.close() }
}
