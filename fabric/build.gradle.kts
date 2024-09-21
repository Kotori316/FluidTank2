plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
    id("com.kotori316.dg")
    alias(libs.plugins.fabric.loom)
}

fabricApi {
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

        create("gameTestServer") {
            name("Fabric GameTest")
            server()
            vmArg("-ea")
            property("fabric-api.GameTest".lowercase())
            property("fabric-api.GameTest.report-file".lowercase(), "game-test/test-results/game_test.xml")
            property("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
            runDir = "game-test"
            source(sourceSets["test"])
        }

        create("data") {
            client()
            configName = "Data"
            runDir = "build/dataGen"
            property("fabric-api.DataGen".lowercase())
            property("fabric-api.DataGen.output-dir".lowercase(), "${file("src/generated/resources")}")
            property("fabric-api.DataGen.strict-validation".lowercase())
            property("fabric-api.DataGen.ModId".lowercase(), "fluidtank_data")

            isIdeConfigGenerated = true
            source(sourceSets["dataGen"])
        }
    }
    knownIndyBsms.add("scala/runtime/LambdaDeserialize")
    knownIndyBsms.add("java/lang/runtime/SwitchBootstraps/typeSwitch")
}

repositories {

}

val minecraftVersion = project.property("minecraft_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        val parchmentMC = project.property("parchment_mapping_mc")
        val parchmentDate = project.property("parchment_mapping_version")
        parchment("org.parchmentmc.data:parchment-$parchmentMC:$parchmentDate@zip")
    })

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

    // Other mods
    modCompileOnly(
        group = "curse.maven",
        name = "jade-324717",
        version = project.property("jade_fabric_id").toString()
    )
    /*modRuntimeOnly(
        group = "mezz.jei",
        name = "jei-1.21-fabric",
        version = project.property("jei_fabric_version").toString()
    )*/
    modCompileOnly(
        group = "appeng",
        name = "appliedenergistics2-fabric",
        version = project.property("ae2_fabric_version").toString()
    ) { isTransitive = false }
    //noinspection SpellCheckingInspection
    modImplementation(group = "teamreborn", name = "energy", version = "3.0.0")
    modImplementation("com.kotori316:debug-utility-fabric:${project.property("debug_util_version")}") {
        exclude("net.fabricmc.fabric-api", "fabric-api")
    }
    modImplementation("com.kotori316:VersionCheckerMod:${project.property("automatic_potato_version")}") {
        isTransitive = false
    }

    testImplementation("net.fabricmc:fabric-loader-junit:${project.property("fabric_loader_version")}")
}

tasks {
    val jksSignRemapJar = register("jksSignRemapJar", JarSignTask::class) {
        jarTask = project.tasks.remapJar
    }
    remapJar.configure {
        finalizedBy(jksSignRemapJar)
    }
}
