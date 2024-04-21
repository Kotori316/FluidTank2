import org.gradle.jvm.tasks.Jar

plugins {
    id("java")
    id("scala")
    id("architectury-plugin")
    id("dev.architectury.loom")
}

val minecraftVersion = project.property("minecraft_version") as String

base {
    archivesName = "${project.property("archives_base_name")}-${project.name}"
    group = project.findProperty("maven_group") as String
    version = project.findProperty("mod_version") as String
}

repositories {
    mavenCentral()
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
    maven {
        name = "Azure-SLP"
        url = uri("https://pkgs.dev.azure.com/Kotori316/minecraft/_packaging/mods/maven/v1")
        val catsVersion = project.property("cats_version") as String
        content {
            includeVersion("org.typelevel", "cats-core_3", catsVersion)
            includeVersion("org.typelevel", "cats-kernel_3", catsVersion)
            includeVersion("org.typelevel", "cats-core_2.13", catsVersion)
            includeVersion("org.typelevel", "cats-kernel_2.13", catsVersion)
            includeGroup("com.kotori316")
        }
    }
    maven {
        name = "Kotori316-main"
        url = uri("https://maven.kotori316.com")
        content {
            includeGroup("com.kotori316")
        }
    }
    maven {
        name = "Curse"
        url = uri("https://www.cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        name = "JEI"
        url = uri("https://maven.blamejared.com/")
        content {
            includeGroup("mezz.jei")
        }
    }
    maven {
        // location of a maven mirror for JEI files, as a fallback
        name = "ModMaven"
        url = uri("https://modmaven.dev/")
        content {
            includeVersion("appeng", "appliedenergistics2-forge", project.property("ae2_forge_version") as String)
            includeVersion("appeng", "appliedenergistics2-neoforge", project.property("ae2_neoforge_version") as String)
            includeVersion("appeng", "appliedenergistics2-fabric", project.property("ae2_fabric_version") as String)
        }
    }
    maven {
        name = "MavenTestGCP"
        url = uri("https://storage.googleapis.com/kotori316-maven-test-storage/maven/")
    }
    mavenLocal()
}

architectury {
    minecraft = minecraftVersion
}

loom {
    knownIndyBsms.add("scala/runtime/LambdaDeserialize")
    silentMojangMappingsLicense()
}

val enableScala2 = true

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        val parchmentVersion = project.property("parchment_mapping").toString().split("-", limit = 2)
        val parchmentMC = parchmentVersion[0]
        val parchmentDate = parchmentVersion[1]
        parchment("org.parchmentmc.data:parchment-$parchmentMC:$parchmentDate@zip")
    })

    compileOnly(
        group = "org.scala-lang",
        name = "scala-library",
        version = project.property("scala2_version") as String
    )
    testImplementation(
        group = "org.scala-lang",
        name = "scala-library",
        version = project.property("scala2_version") as String
    )
    if (enableScala2 && System.getProperty("idea.sync.active", "false").toBoolean() ||
        System.getenv("FORCE_SCALA2").toBoolean()
    ) {
        compileOnly(
            group = "org.typelevel",
            name = "cats-core_2.13",
            version = project.property("cats_version") as String
        ) { exclude("org.scala-lang") }
        testImplementation(
            group = "org.typelevel",
            name = "cats-core_2.13",
            version = project.property("cats_version") as String
        ) { exclude("org.scala-lang") }
    } else {
        compileOnly(
            group = "org.scala-lang",
            name = "scala3-library_3",
            version = project.property("scala3_version") as String
        )
        compileOnly(
            group = "org.typelevel",
            name = "cats-core_3",
            version = project.property("cats_version") as String
        ) { exclude("org.scala-lang") }
        testImplementation(
            group = "org.scala-lang",
            name = "scala3-library_3",
            version = project.property("scala3_version") as String
        )
        testImplementation(
            group = "org.typelevel",
            name = "cats-core_3",
            version = project.property("cats_version") as String
        ) { exclude("org.scala-lang") }
    }

    testImplementation(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
        }
    }

    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    withType(ScalaCompile::class) {
        if (enableScala2 && System.getProperty("idea.sync.active", "false").toBoolean() ||
            System.getenv("FORCE_SCALA2").toBoolean()
        ) {
            scalaCompileOptions.additionalParameters = listOf("-X" + "source:3")
        }
        options.encoding = "UTF-8"
    }

    withType(ProcessResources::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    withType(Jar::class) {
        exclude(".cache/")
    }
    named("sourcesJar", Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
