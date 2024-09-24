plugins {
    alias(libs.plugins.publish.all)
    alias(libs.plugins.idea.ext)
}

version = project.findProperty("mod_version") as String
val releaseDebug = (System.getenv("RELEASE_DEBUG") ?: "true").toBoolean()

tasks.named("wrapper", Wrapper::class) {
    gradleVersion = "8.10.2"
    distributionType = Wrapper.DistributionType.BIN
}

publishMods {
    dryRun = releaseDebug
    github {
        repository = "Kotori316/FluidTank2"
        accessToken = project.findProperty("githubToken") as? String ?: System.getenv("REPO_TOKEN") ?: ""
        commitish = "1.21"
        tagName = "v${project.findProperty("mod_version")}"
        displayName = "v${project.findProperty("mod_version")} for ${project.findProperty("minecraft_version")}"
        changelog = createChangelog()
        type = if ((project.findProperty("mod_version") as String).contains("SNAPSHOT")) BETA else STABLE

        allowEmptyFiles = true
    }

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
