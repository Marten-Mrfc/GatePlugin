rootProject.name = "GatePlugin"

plugins {
    id("com.gradle.enterprise") version ("3.13.3")
}

includeBuild("D:\\projects\\MLib") {
    dependencySubstitution {
        substitute(module("mlib.api:MLib")).using(project(":"))
    }
}