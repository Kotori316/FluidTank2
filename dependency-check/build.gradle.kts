plugins {
    id("com.kotori316.common")
}

dependencies {
    // JEI
    implementation(
        group = "mezz.jei",
        name = "jei-${project.property("jei_forge_repo_version")}-forge",
        version = project.property("jei_forge_version").toString()
    )
    implementation(
        group = "mezz.jei",
        name = "jei-${project.property("jei_fabric_repo_version")}-fabric",
        version = project.property("jei_fabric_version").toString()
    )
    implementation(
        group = "mezz.jei",
        name = "jei-${project.property("jei_neoforge_repo_version")}-neoforge",
        version = project.property("jei_neoforge_version").toString()
    )
    // Parchment
    implementation(
        group = "org.parchmentmc.data",
        name = "parchment-${project.property("parchment_mapping_mc")}",
        version = project.property("parchment_mapping_version").toString(),
        ext = "zip"
    )
}
