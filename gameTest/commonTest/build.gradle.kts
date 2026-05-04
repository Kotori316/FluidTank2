plugins {
    id("com.kotori316.common")
    alias(libs.plugins.fabric.loom)
    id("signing")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")

    compileOnly("com.kotori316:debug-utility-common:${project.property("debug_util_version")}") {
        isTransitive = false
    }
    implementation(project(":common"))
    implementation(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    implementation("org.junit.jupiter:junit-jupiter")
}

val hasGpgSignature = project.hasProperty("signing.keyId") &&
        project.hasProperty("signing.password") &&
        project.hasProperty("signing.secretKeyRingFile")

signing {
    sign(tasks.jar.get())
}

tasks {
    val jksSignJar = register("jksSignJar", JarSignTask::class) {
        jarTask = jar
    }
    jar {
        destinationDirectory = project.layout.buildDirectory.dir("libs")
        archiveClassifier = ""
        finalizedBy(jksSignJar)
    }
    withType(Sign::class) {
        onlyIf { hasGpgSignature }
    }
}
