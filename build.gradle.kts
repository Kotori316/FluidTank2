plugins {
    alias(libs.plugins.github.release)
    alias(libs.plugins.idea.ext)
}

tasks.named("wrapper", Wrapper::class) {
    gradleVersion = "8.8"
    distributionType = Wrapper.DistributionType.BIN
}

githubRelease {
    owner = "Kotori316"
    repo = "FluidTank2"
    token(project.findProperty("githubToken") as? String ?: System.getenv("REPO_TOKEN") ?: "")
    targetCommitish = "1.21"
    tagName = "v${project.findProperty("mod_version")}"
    releaseName = "v${project.findProperty("mod_version")} for ${project.findProperty("minecraft_version")}"
    body = createChangelog()
    prerelease = (project.findProperty("mod_version") as String).contains("SNAPSHOT")

    val buildDirectories = listOf(
        findProject(":forge")?.layout?.buildDirectory?.dir("libs"),
        findProject(":fabric")?.layout?.buildDirectory?.dir("libs"),
        findProject(":neoforge")?.layout?.buildDirectory?.dir("libs"),
    )
    releaseAssets = files(
        *buildDirectories.filterNotNull().map {
            fileTree(it) {
                include("*.jar")
            }
        }.toTypedArray()
    )
    dryRun = (System.getenv("RELEASE_DEBUG") ?: "true").toBoolean()
    overwrite = false
    allowUploadToExisting = false
}

fun createChangelog(): String {
    val base = """
        # Large Fluid Tank
        
        | Dependency | Version |
        | -- | -- |
        | Minecraft | ${project.property("minecraft_version")} |
        | Forge | ${project.property("forge_version")} |
        | Fabric | ${project.property("fabric_api_version")} |
        | NeoForge | ${project.property("neoforge_version")} |
        """.trimIndent()
    val fromFile = rootProject.file(project.property("changelog_file")!!).readText()
    val shortFormat = fromFile.split("---", limit = 2)[0]
    return base + System.lineSeparator() + shortFormat
}
