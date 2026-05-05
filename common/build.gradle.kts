plugins {
    id("com.kotori316.common")
    id("com.kotori316.dg")
    alias(libs.plugins.fabric.loom)
}

loom {
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    compileOnly("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")

    testImplementation(
        "org.mockito:mockito-core:${project.property("mockitoCoreVersion")}"
    )
    testImplementation(
        "org.mockito:mockito-inline:${project.property("mockitoInlineVersion")}"
    )
}
