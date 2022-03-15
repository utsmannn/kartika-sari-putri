import java.io.File
import java.math.RoundingMode
import javax.imageio.ImageIO
import kotlin.math.roundToInt

fun Double.scaleRound(): Double {
    return toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
}

fun Double.toReadable(): String {
    return if (this >= 1024) {
        val rawResult = this / 1024
        val result = rawResult.scaleRound()
        "${result}MB"
    } else {
        "${this}KB"
    }
}

fun File.getSize(): String {
    val size = this.length().toDouble().orNol() / 1000.0 // Get size and convert bytes into KB.
    return size.toReadable()
}

fun File.getFileSize(): ImageController.ResultInfo.FileSize {
    val size = this.length().toDouble().orNol() / 1000.0 // Get size and convert bytes into KB.
    return if (size >= 1024) {
        val rawResult = size / 1024
        val result = rawResult.scaleRound()
        ImageController.ResultInfo.FileSize(size, "${result}MB")
    } else {
        ImageController.ResultInfo.FileSize(size, "${size}KB")
    }
}

fun File.getDimensionsString(): String {
    val buffer = ImageIO.read(this)
    val width = buffer.width
    val height = buffer.height
    return "${width}x${height}"
}

fun Long?.orNol() = this ?: 0L
fun Double?.orNol(): Double = this ?: 0.0

fun percentageOf(from: Int, to: Double): Int {
    return ((to / 100) * from.toDouble()).roundToInt()
}