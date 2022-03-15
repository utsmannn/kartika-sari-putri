import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString

class Application : CliktCommand() {
    private val file by argument().file().multiple()
    private val webp by option().flag(default = false)

    override fun run() {
        val imageController = ImageController()

        val parentDir = getCurrentDir()

        val outputLocation = UUID.randomUUID().toString()

        val validateImages = if (file.map { it.name }.contains(".")) {
            val currentDir = parentDir.absolutePath
            val currentDirFile = File(currentDir)
            currentDirFile.walkBottomUp().apply {
                maxDepth(1)
            }.filter { it.isFile }.toList()
        } else {
            file
        }

        imageController.images = validateImages
        imageController.resizeImage(webp, outputLocation)

        val outputDirectory = File(parentDir, outputLocation)
        imageController.printReport(outputDirectory)
        if (TermUi.confirm("Open output folder?") == true) {
            ProcessBuilder("open", outputDirectory.absolutePath)
                .start()
        } else {
            echo("Directory output: $outputDirectory")
        }
    }

    private fun getCurrentDir(): File {
        return File(Paths.get("").absolutePathString())
    }
}

fun main(args: Array<String>) {
    return Application().main(args)
}