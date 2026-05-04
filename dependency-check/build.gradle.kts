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
    // Scala 3
    implementation(
        group = "org.scala-lang",
        name = "scala3-library_3",
        version = project.property("scala3_version") as String
    )
}
