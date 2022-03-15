plugins {
    kotlin("jvm") version "1.6.10"
}

group = "org.kartika.putri"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ajalt.clikt:clikt:3.4.0")
    implementation("org.imgscalr:imgscalr-lib:4.2")
    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("com.jakewharton.picnic:picnic:0.6.0")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ApplicationKt"
    }

    tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.register<Copy>("buildScript") {
    dependsOn("build")
    val jarFile = "$buildDir/libs/${project.name}-${project.version}.jar"
    val intoFile = "$rootDir/dist/libs"
    from(jarFile)
    into(intoFile)

    doLast {
        val file = File("$rootDir/dist", project.name)
        file.writeText("""
            #/usr/bin
            java -jar ${projectDir.absolutePath}/dist/libs/${project.name}-${project.version}.jar ${'$'}@
        """.trimIndent())

        exec {
            commandLine("chmod", "+x", file.absolutePath)
        }
    }

    val scriptLocation = "${projectDir.absolutePath}/dist/${project.name}"
    System.out.println("Script location created on $scriptLocation")
}