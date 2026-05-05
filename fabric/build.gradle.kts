plugins {
    id("com.kotori316.common")
    id("com.kotori316.publish")
    id("com.kotori316.subprojects")
    id("com.kotori316.dg")
    alias(libs.plugins.fabric.loom)
}

sourceSets {
    create("clientTest") {
        val s = this
        project.configurations {
            named(s.compileClasspathConfigurationName) {
                extendsFrom(project.configurations.compileClasspath.get())
                extendsFrom(project.configurations.testCompileClasspath.get())
            }
            named(s.runtimeClasspathConfigurationName) {
                extendsFrom(project.configurations.runtimeClasspath.get())
                extendsFrom(project.configurations.testRuntimeClasspath.get())
            }
        }
    }
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
            property("mixin.debug.export", "true")
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

        create("gameTestClient") {
            client()
            name("Fabric Client GameTest")
            property("fabric.client.gametest")
            property("fabric.client.gametest.disableNetworkSynchronizer", "true")
            runDir = "run-client"
            source(sourceSets["clientTest"])
            // val openalLib = System.getenv("OPENAL_LIB")
            // if (!openalLib.isNullOrEmpty()) {
            //     property("org.lwjgl.openal.libname", openalLib)
            //     environmentVariables["LD_PRELOAD"] = openalLib
            // }
        }
    }
}

repositories {

}

val minecraftVersion = project.property("minecraft_version") as String

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")

    implementation(
        "net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}"
    )
    implementation(
        "net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}"
    )

    runtimeOnly(
        "com.kotori316:scalable-cats-force-fabric:${project.property("slp_fabric_version")}:dev"
    ) { isTransitive = false }

    // Other mods
    implementation(
        "curse.maven:jade-324717:${project.property("jade_fabric_id")}"
    )
    /*runtimeOnly(
        group = "mezz.jei",
        name = "jei-${project.property("jei_fabric_repo_version")}-fabric",
        version = project.property("jei_fabric_version").toString()
    )*/
    //noinspection SpellCheckingInspection
    implementation(
        "teamreborn:energy:${project.property("fabric_energy_version")}"
    )
    implementation("com.kotori316:debug-utility-fabric:${project.property("debug_util_version")}") {
        exclude("net.fabricmc.fabric-api", "fabric-api")
    }
    implementation("com.kotori316:VersionCheckerMod:${project.property("automatic_potato_version")}") {
        isTransitive = false
    }

    testImplementation("net.fabricmc:fabric-loader-junit:${project.property("fabric_loader_version")}")
    testImplementation(project(":gameTest:commonTest"))

    "clientTestImplementation"(project.sourceSets.main.get().output)
}

tasks {
    /*val jksSignJar = register("jksSignJar", JarSignTask::class) {
        jarTask = project.tasks.jar
    }
    jar.configure {
        finalizedBy(jksSignJar)
    }*/
}
