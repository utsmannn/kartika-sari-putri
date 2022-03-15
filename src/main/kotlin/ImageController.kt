import com.github.ajalt.clikt.output.TermUi
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.renderText
import com.jakewharton.picnic.table
import com.luciad.imageio.webp.WebPWriteParam
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

class ImageController {

    var images: List<File> = emptyList()
    fun resizeImage(isWebp: Boolean, output: String) {
        images.forEach { file ->
            try {
                val parentPath = file.parent
                val parentOutput = File(parentPath, output)
                val bufferSrc = ImageIO.read(file)
                TermUi.echo("\r > Process file: ${file.name}" + " ".repeat(20), trailingNewline = false)

                DpiPercentage.dpis.forEach { pair ->
                    val dpi = pair.first
                    val percentageTarget = pair.second
                    val percentage = percentageOf(bufferSrc.width, percentageTarget.toDouble())
                    val parentDpi = File(parentOutput, dpi)
                    parentDpi.mkdirs()

                    val filename = file.name
                    val bufferOut = Scalr.resize(bufferSrc, Scalr.Method.QUALITY, percentage)
                    val fileOutput = File(parentDpi, filename)
                    ImageIO.write(bufferOut, file.extension, fileOutput)

                    if (isWebp) {
                        convertToWebp(fileOutput)
                    } else {
                        resize(file, bufferOut)
                    }

                    bufferOut.flush()
                    bufferSrc.flush()
                }
            } catch (e: javax.imageio.IIOException) {
            } catch (e: java.lang.NullPointerException) {
            }
        }
    }

    private fun resize(file: File, buffer: BufferedImage) {
        val outputStream = file.outputStream()
        val writers = ImageIO.getImageWritersByFormatName(file.extension)
        if (!writers.hasNext()) {
            outputStream.close()
            return
        }
        val writer = writers.next()
        val imageOutputStream = ImageIO.createImageOutputStream(outputStream)
        writer.output = imageOutputStream

        val param = writer.defaultWriteParam
        param.run {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionType = compressionTypes[WebPWriteParam.LOSSY_COMPRESSION]
            compressionQuality = 0.75f
        }
        writer.write(null, IIOImage(buffer, null, null), param)

        writer.dispose()
        outputStream.close()
        imageOutputStream.close()
    }


    private fun convertToWebp(image: File) {
        val source = ImageIO.read(image)
        ImageIO.write(source, "webp", File(image.parent, "${image.nameWithoutExtension}.webp"))
        source.flush()
        image.delete()
    }

    fun printReport(parent: File) {
        val groupingFolder = DpiPercentage.dpis.map { it.first }
        val resultInfo = ResultInfo.fromFolder(parent, images)

        table {
            style {
                borderStyle = BorderStyle.Hidden
            }
            cellStyle {
                border = true
                alignment = TextAlignment.MiddleRight
                paddingLeft = 1
                paddingRight = 1
                borderLeft = true
                borderRight = true
            }

            header {
                cellStyle {
                    border = true
                    alignment = TextAlignment.BottomLeft
                }
                row {
                    cell("filename") {
                        alignment = TextAlignment.BottomRight
                    }

                    groupingFolder.forEach {
                        cell(it) {
                            alignment = TextAlignment.BottomCenter
                        }
                    }

                    cell("Original") {
                        alignment = TextAlignment.BottomCenter
                    }

                    cell("Saved size") {
                        alignment = TextAlignment.BottomCenter
                    }
                }
            }
            body {
                resultInfo.forEach {
                    row(it.name, it.ldpi, it.mdpi, it.ldpi, it.xhdpi, it.xxhdpi, it.xxxhdpi, it.original, it.diff)
                }
            }
        }.renderText().let {
            TermUi.echo("\r${" ".repeat(100)}\n$it\n\n\n", trailingNewline = false)
        }
    }

    object DpiPercentage {
        val LDPI = Pair("ldpi", 20)
        val MDPI = Pair("mdpi", 25)
        val HDPI = Pair("hdpi", 30)
        val XHDPI = Pair("xhdpi", 50)
        val XXHDPI = Pair("xxhdpi", 80)
        val XXXHDPI = Pair("xxxhdpi", 100)

        val dpis = listOf(LDPI, MDPI, HDPI, XHDPI, XXHDPI, XXXHDPI)
    }

    data class ResultInfo(
        var name: String = "",
        var ldpi: String = "",
        var mdpi: String = "",
        var hdpi: String = "",
        var xhdpi: String = "",
        var xxhdpi: String = "",
        var xxxhdpi: String = "",
        var original: String = "",
        var diff: String = ""
    ) {
        companion object {
            fun fromFolder(folder: File, originalFile: List<File>): List<ResultInfo> {
                val files = folder.walkTopDown().toList().filter { it.isFile }
                val groupByFolder = files.groupBy { it.parentFile.name }

                val ldpiGroup = groupByFolder[DpiPercentage.LDPI.first]
                val mdpiGroup = groupByFolder[DpiPercentage.MDPI.first]
                val hdpiGroup = groupByFolder[DpiPercentage.HDPI.first]
                val xhdpiGroup = groupByFolder[DpiPercentage.XHDPI.first]
                val xxhdpiGroup = groupByFolder[DpiPercentage.XXHDPI.first]
                val xxxhdpiGroup = groupByFolder[DpiPercentage.XXXHDPI.first]

                val infos = ldpiGroup?.mapIndexed { index, file ->
                    val originalSingle = originalFile.find { it.nameWithoutExtension == file.nameWithoutExtension }
                    val originalSize = originalSingle?.getFileSize()
                    val name = file.name.ellipsis(file.extension)

                    val ldpiSize = file.getFileSize()
                    val mdpiSize = mdpiGroup?.get(index)?.getFileSize()
                    val hdpiSize = hdpiGroup?.get(index)?.getFileSize()
                    val xhdpiSize = xhdpiGroup?.get(index)?.getFileSize()
                    val xxhdpiSize = xxhdpiGroup?.get(index)?.getFileSize()
                    val xxxhdpiSize = xxxhdpiGroup?.get(index)?.getFileSize()

                    val ldpi = ldpiSize.read + "\n(${file.getDimensionsString()})"
                    val mdpi = mdpiSize?.read + "\n(${mdpiGroup?.get(index)?.getDimensionsString()})"
                    val hdpi = hdpiSize?.read + "\n(${hdpiGroup?.get(index)?.getDimensionsString()})"
                    val xhdpi = xhdpiSize?.read + "\n(${xhdpiGroup?.get(index)?.getDimensionsString()})"
                    val xxhdpi = xxhdpiSize?.read + "\n(${xxhdpiGroup?.get(index)?.getDimensionsString()})"
                    val xxxhdpi = xxxhdpiSize?.read + "\n(${xxxhdpiGroup?.get(index)?.getDimensionsString()})"

                    val original = originalSize?.count?.toReadable().orEmpty()
                    val diff = originalSize?.min(xxxhdpiSize)
                    val diffRead = diff?.count?.toReadable().orEmpty()

                    ResultInfo(name, ldpi, mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi, original, diffRead)
                }.orEmpty()

                return infos
            }
        }

        data class FileSize(
            val count: Double,
            val read: String
        ) {

            fun plus(fileSize: FileSize?): FileSize {
                val totalCount = count + (fileSize?.count ?: 0.0)
                return FileSize(totalCount, totalCount.toReadable())
            }

            fun min(fileSize: FileSize?): FileSize {
                val totalCount = (count - (fileSize?.count ?: 0.0)).scaleRound()
                return FileSize(totalCount, totalCount.toReadable())
            }
        }
    }
}