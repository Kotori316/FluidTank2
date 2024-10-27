plugins {
    id("com.kotori316.common")
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        val parchmentMC = project.property("parchment_mapping_mc")
        val parchmentDate = project.property("parchment_mapping_version")
        parchment("org.parchmentmc.data:parchment-$parchmentMC:$parchmentDate@zip")
    })

    implementation(project(":common"))
    implementation(platform("org.junit:junit-bom:${project.property("jupiterVersion")}"))
    implementation("org.junit.jupiter:junit-jupiter")
}

tasks.remapJar {
    enabled = false
}

tasks.jar {
    destinationDirectory = project.layout.buildDirectory.dir("libs")
    archiveClassifier = ""
}
