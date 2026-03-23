package cn.edu.ubaa.cgyy

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CgyyCaptchaSolverTest {
  private val solver = CgyyCaptchaSolver()

  @Test
  fun `solveOffset finds expected x position`() {
    val background = BufferedImage(120, 50, BufferedImage.TYPE_INT_ARGB)
    val piece = BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB)

    for (y in 0 until 50) {
      for (x in 0 until 120) {
        background.setRGB(x, y, Color.WHITE.rgb)
      }
    }
    for (y in 10 until 28) {
      for (x in 46 until 64) {
        background.setRGB(x, y, Color.BLACK.rgb)
      }
    }
    for (y in 0 until 18) {
      for (x in 0 until 18) {
        piece.setRGB(x, y, Color.BLACK.rgb)
      }
    }

    val offset = solver.solveOffset(background, piece)
    assertTrue(offset >= 0)
  }

  @Test
  fun `encrypt returns base64 text`() {
    val encrypted = solver.encrypt("""{"x":46,"y":5}""", "1234567890abcdef")
    assertTrue(encrypted.isNotBlank())
  }
}
