pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.0.21"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")

include(":federation-common")
project(":federation-common").projectDir = file("federated-rd-engine/federation-common")

include(":dataset-provider-service")
project(":dataset-provider-service").projectDir = file("federated-rd-engine/dataset-provider-service")

include(":hospital-node-service")
project(":hospital-node-service").projectDir = file("federated-rd-engine/hospital-node-service")
