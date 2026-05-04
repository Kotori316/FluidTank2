plugins {
    alias(libs.plugins.publish.all)
}

version = project.findProperty("mod_version") as String
val releaseDebug = (System.getenv("RELEASE_DEBUG") ?: "true").toBoolean()

publishMods {
    dryRun = releaseDebug
    github {
        repository = "Kotori316/FluidTank2"
        accessToken = project.findProperty("githubToken") as? String ?: System.getenv("REPO_TOKEN") ?: ""
        commitish = project.property("github_branch") as? String
        tagName = "v${project.findProperty("mod_version")}"
        displayName = "v${project.findProperty("mod_version")} for ${project.findProperty("minecraft_version")}"
        changelog = createChangelog()
        type = if (project.version.toString().contains("SNAPSHOT")) BETA else STABLE
        allowEmptyFiles = false

        file = rootProject.layout.projectDirectory.file(provider { project.property("changelog_file").toString() })
        additionalFiles.from(gatherArtifacts())
    }
}

fun createChangelog(): String {
    val base = """
        # Large Fluid Tank
        
        | Dependency | Version |
        | -- | -- |
        | Minecraft | ${project.property("minecraft_version")} |
        | Fabric | ${project.property("fabric_api_version")} |
        | NeoForge | ${project.property("neoforge_version")} |
        
        """.trimIndent()
    val fromFile = rootProject.file(project.property("changelog_file")!!).readText()
    val shortFormat = fromFile.split("---", limit = 2)[0]
    return base + System.lineSeparator() + shortFormat
}

fun gatherArtifacts(): List<Provider<RegularFile>> {
    val list = mutableListOf<Provider<RegularFile>>()
    if (!System.getenv("DISABLE_FABRIC").toBoolean()) {
        list.add(project(":fabric").tasks.named("jar", org.gradle.jvm.tasks.Jar::class).flatMap { it.archiveFile })
    }
    if (!System.getenv("DISABLE_NEOFORGE").toBoolean()) {
        list.add(project(":neoforge").tasks.named("jar", org.gradle.jvm.tasks.Jar::class).flatMap { it.archiveFile })
    }
    return list
}
