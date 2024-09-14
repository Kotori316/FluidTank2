plugins {
    id("java")
    id("scala")
}

sourceSets {
    create("dataGen") {
        val s = this
        java {
            srcDir("src/dataGen/java")
        }
        scala {
            srcDir("src/dataGen/scala")
        }
        resources {
            srcDir("src/dataGen/resources")
        }
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

configurations {
    create("dataGenRuntime")
}

tasks.named("processDataGenResources", ProcessResources::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    "dataGenImplementation"(project.sourceSets.main.get().output)
    if (project.name != "common") {
        "dataGenImplementation"(project.project(":common").sourceSets["dataGen"].output)
    }
}
