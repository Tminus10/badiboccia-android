package swiss.reutener.boccia

import org.junit.Assert.assertEquals
import org.junit.Test
import swiss.reutener.boccia.net.TeamPhoto

/** Like the iOS app's photo test: uploads are center-cropped squares, at most 1000 pixels, never upscaled. */
class TeamPhotoTest {
    @Test
    fun landscapePhotoIsScaledAndCroppedToTheCenter() {
        val crop = TeamPhoto.squareCrop(4000, 3000, side = 1000)
        assertEquals(1000, crop.size)
        assertEquals(1333, crop.scaledWidth)
        assertEquals(1000, crop.scaledHeight)
        assertEquals(166, crop.left)
        assertEquals(0, crop.top)
    }

    @Test
    fun portraitPhotoIsCroppedVertically() {
        val crop = TeamPhoto.squareCrop(3000, 4000, side = 1000)
        assertEquals(0, crop.left)
        assertEquals(166, crop.top)
    }

    @Test
    fun smallPhotoIsNotUpscaled() {
        val crop = TeamPhoto.squareCrop(600, 800, side = 1000)
        assertEquals(600, crop.size)
        assertEquals(600, crop.scaledWidth)
        assertEquals(800, crop.scaledHeight)
        assertEquals(100, crop.top)
    }
}
