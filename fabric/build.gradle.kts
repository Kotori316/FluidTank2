plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    runs {
        named("client") {
            configName = "Fabric Client"
            runDir = "run"
            source(sourceSets["test"])
        }
        named("server") {
            configName = "Fabric Server"
            runDir = "run-server"
        }

        create("gameTest") {
            name("Fabric GameTest")
            server()
            vmArg("-ea")
            //noinspection SpellCheckingInspection
            property("fabric-api.gametest")
            //noinspection SpellCheckingInspection
            property("fabric-api.gametest.report-file", "game-test/test-results/game_test.xml")
            runDir = "game-test"
            source(sourceSets["test"])
        }
    }
}

repositories {

}

configurations {
    named("developmentFabric") {
        extendsFrom(common.get())
    }
}

dependencies {
    modImplementation(
        group = "net.fabricmc",
        name = "fabric-loader",
        version = project.property("fabric_loader_version").toString()
    )
    modApi(
        group = "net.fabricmc.fabric-api",
        name = "fabric-api",
        version = project.property("fabric_api_version").toString()
    )

    modRuntimeOnly(
        group = "com.kotori316",
        name = "scalable-cats-force-fabric",
        version = project.property("slp_fabric_version").toString(),
        classifier = "dev"
    ) { isTransitive = false }

    common(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":common", configuration = "transformProductionFabric")) { isTransitive = false }

    // Other mods
    modCompileOnly(
        group = "curse.maven",
        name = "jade-324717",
        version = project.property("jade_fabric_id").toString()
    )
    // modRuntimeOnly(group="mezz.jei", name="jei-1.20.2-fabric", version=project.jei_fabric_version)
    modCompileOnly(
        group = "appeng",
        name = "appliedenergistics2-fabric",
        version = project.property("ae2_fabric_version").toString()
    ) { isTransitive = false }
    //noinspection SpellCheckingInspection
    modImplementation(group = "teamreborn", name = "energy", version = "3.0.0")
    modImplementation("com.kotori316:test-utility-fabric:${project.property("test_util_version")}")
    modImplementation("com.kotori316:debug-utility-fabric:${project.property("debug_util_version")}")
    modImplementation("com.kotori316:VersionCheckerMod:${project.property("automatic_potato_version")}") {
        isTransitive = false
    }

    testImplementation("net.fabricmc:fabric-loader-junit:${project.property("fabric_loader_version")}")
}

ext {
    set(
        "changelogHeader", """
        # Large Fluid Tank for fabric
        
        | Dependency | Version |
        | -- | -- |
        | Minecraft | ${project.property("minecraft_version")} |
        | Fabric | ${project.property("fabric_api_version")} |
        | Fabric Loader | ${project.property("fabric_loader_version")} |
        | scalable-cats-force | ${project.property("slp_fabric_version")} |
        | Applied Energistics 2 | ${project.property("ae2_fabric_version")} |
        | Jade | File id: ${project.property("jade_fabric_id")} |
        """.trimIndent()
    )
}
