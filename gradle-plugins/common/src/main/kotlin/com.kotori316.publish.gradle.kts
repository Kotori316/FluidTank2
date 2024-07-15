import com.kotori316.plugin.cf.CallVersionCheckFunctionTask
import com.kotori316.plugin.cf.CallVersionFunctionTask

plugins {
    id("java")
    id("scala")
    id("maven-publish")
    id("signing")
    id("com.kotori316.plugin.cf")
    id("me.modmuss50.mod-publish-plugin")
}

val minecraftVersion = project.property("minecraft_version") as String
val releaseDebug = (System.getenv("RELEASE_DEBUG") ?: "true").toBoolean()

signing {
    sign(publishing.publications)
}

val hasGpgSignature = project.hasProperty("signing.keyId") &&
        project.hasProperty("signing.password") &&
        project.hasProperty("signing.secretKeyRingFile")

val jarTask = tasks.jar

tasks {
    val jksSignJar = register("jksSignJar") {
        dependsOn(jarTask)
        onlyIf {
            project.hasProperty("jarSign.keyAlias") &&
                    project.hasProperty("jarSign.keyLocation") &&
                    project.hasProperty("jarSign.storePass")
        }
        doLast {
            ant.withGroovyBuilder {
                "signjar"(
                    "jar" to jarTask.flatMap { it.archiveFile }.get(),
                    "alias" to project.findProperty("jarSign.keyAlias"),
                    "keystore" to project.findProperty("jarSign.keyLocation"),
                    "storepass" to project.findProperty("jarSign.storePass"),
                    "sigalg" to "Ed25519",
                    "digestalg" to "SHA-256",
                    "tsaurl" to "http://timestamp.digicert.com",
                )
            }
        }
    }
    jarTask.configure {
        finalizedBy(jksSignJar)
    }
    withType(Sign::class) {
        onlyIf { hasGpgSignature }
    }
    withType(AbstractPublishToMaven::class) {
    }

    val baseName = project.findProperty("maven_base_name") as String

    register("registerVersion", CallVersionFunctionTask::class) {
        functionEndpoint = CallVersionFunctionTask.readVersionFunctionEndpoint(project)
        gameVersion = minecraftVersion
        platform = project.name
        platformVersion = when (project.name) {
            "forge" -> project.property("forge_version").toString()
            "fabric" -> project.property("fabric_api_version").toString()
            "neoforge" -> project.property("neoforge_version").toString()
            else -> throw IllegalArgumentException("Unknown platform ${project.name}")
        }
        modName = baseName
        changelog = cfChangelog()
        homepage = "https://modrinth.com/mod/large-fluid-tank"
        isDryRun = releaseDebug
    }
    register("checkReleaseVersion", CallVersionCheckFunctionTask::class) {
        gameVersion = minecraftVersion
        platform = project.name
        modName = baseName
        version = project.version.toString()
        failIfExists = !releaseDebug
    }
}

fun cfChangelog(): String {
    return rootProject
        .file(project.property("changelog_file") as String)
        .useLines {
            it.joinToString(System.lineSeparator())
                .split("---", limit = 2)[0]
                .lines()
                .filterNot { t -> t.startsWith("## ") }
                .filter { t -> t.isNotBlank() }
                .joinToString(System.lineSeparator())
        }
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }

    repositories {
        val u = project.findProperty("maven_username") as? String ?: System.getenv("MAVEN_USERNAME") ?: ""
        val p = project.findProperty("maven_password") as? String ?: System.getenv("MAVEN_PASSWORD") ?: ""
        if (u != "" && p != "") {
            maven {
                name = "kotori316-maven"
                // For users: Use https://maven.kotori316.com to get artifacts
                url = uri("https://maven2.kotori316.com/production/maven")
                credentials {
                    username = u
                    password = p
                }
            }
        }
    }
}

fun mapPlatformToCamel(platform: String): String {
    return when (platform) {
        "forge" -> "Forge"
        "fabric" -> "Fabric"
        "neoforge" -> "NeoForge"
        else -> throw IllegalArgumentException("Unknown platform $platform")
    }
}

fun curseChangelog(): String {
    if (!ext.has("changelogHeader")) {
        throw IllegalStateException("No changelogHeader in curseChangelog for project(${project.name})")
    }
    val header = ext.get("changelogHeader").toString()
    val fromFile = rootProject
        .file(project.property("changelog_file") as String)
        .readText()
    return header + System.lineSeparator() + fromFile
}

fun curseProjectId(platform: String): String {
    return when (platform) {
        "forge" -> "291006"
        "fabric" -> "411564"
        "neoforge" -> "291006"
        else -> throw IllegalArgumentException("Unknown platform $platform")
    }
}

fun modrinthChangelog(): String {
    if (!ext.has("changelogHeader")) {
        throw IllegalStateException("No changelogHeader in modrinthChangelog for project(${project.name})")
    }
    val header = ext.get("changelogHeader").toString()
    val fromFile = rootProject
        .file(project.property("changelog_file") as String)
        .readText()
    val shortFormat = fromFile.split("---", limit = 2)[0]
    return header + System.lineSeparator() + shortFormat
}

publishMods {
    dryRun = releaseDebug
    type = STABLE
    file = jarTask.flatMap { it.archiveFile }
    modLoaders = listOf(project.name)
    displayName = "${project.version}-${project.name}"

    curseforge {
        accessToken = (
                project.findProperty("curseforge_additional-enchanted-miner_key")
                    ?: System.getenv("CURSE_TOKEN")
                    ?: "") as String
        projectId = curseProjectId(project.name)
        minecraftVersions = listOf(minecraftVersion)
        changelog = provider { curseChangelog() }
        requires {
            slug = "scalable-cats-force"
        }
        if (project.name == "fabric") {
            requires {
                slug = "automatic-potato"
            }
        }
    }

    modrinth {
        accessToken = (project.findProperty("modrinthToken") ?: System.getenv("MODRINTH_TOKEN") ?: "") as String
        projectId = "uMlJQMHT"
        minecraftVersions = listOf(minecraftVersion)
        changelog = provider { modrinthChangelog() }
        requires {
            slug = "scalable-cats-force"
        }
        if (project.name == "fabric") {
            requires {
                slug = "automatic-potato"
            }
        }
    }
}

afterEvaluate {
    rootProject.tasks.named("githubRelease") {
        dependsOn(tasks.assemble)
    }
}

tasks.register("checkChangelog") {
    doLast {
        listOf(
            "cfChangelog" to cfChangelog(),
            "curseChangelog" to curseChangelog(),
            "modrinthChangelog" to modrinthChangelog(),
        ).forEach { pair ->
            println("::group::${pair.first} in ${project.name}")
            println(pair.second)
            println("::EndGroup::".lowercase())
        }
    }
}
