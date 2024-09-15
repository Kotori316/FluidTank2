plugins {
    id("com.kotori316.common")
    id("com.kotori316.dg")
    alias(libs.plugins.fabric.loom)
}

loom {
    knownIndyBsms.add("scala/runtime/LambdaDeserialize")
    knownIndyBsms.add("java/lang/runtime/SwitchBootstraps/typeSwitch")
}

tasks.named("remapJar") {
    enabled = false
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        val parchmentMC = project.property("parchment_mapping_mc")
        val parchmentDate = project.property("parchment_mapping_version")
        parchment("org.parchmentmc.data:parchment-$parchmentMC:$parchmentDate@zip")
    })
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modLocalRuntime("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")

    testImplementation(
        group = "org.mockito",
        name = "mockito-core",
        version = project.property("mockitoCoreVersion").toString()
    )
    testImplementation(
        group = "org.mockito",
        name = "mockito-inline",
        version = project.property("mockitoInlineVersion").toString()
    )
}
