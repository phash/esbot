package de.phash

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.*

/**
 * Generate an QR image for the given text.
 *
 * @param text
 * @param width
 * @param height
 * @return
 * @throws WriterException
 */
@Throws(WriterException::class)
fun createQrImage(text: String, width: Int, height: Int): BufferedImage {
    val hintMap = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8")
    hintMap.put(EncodeHintType.MARGIN, 2)
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L)

    val writer = QRCodeWriter()
    val matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hintMap)
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    image.createGraphics()

    val graphics = image.graphics as Graphics2D
    graphics.color = Color.WHITE
    graphics.fillRect(0, 0, width, height)
    graphics.color = Color.BLACK

    for (i in 0 until width) {
        for (j in 0 until width) {
            if (matrix.get(i, j)) {
                graphics.fillRect(i, j, 1, 1)
            }
        }
    }

    return image
}