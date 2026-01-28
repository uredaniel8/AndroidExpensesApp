pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        // Microsoft Duo SDK feed (REQUIRED for display-mask)
        maven {
            url = uri("https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")
            name = "Duo-SDK-Feed"
        }

        google()
        mavenCentral()
    }
}

rootProject.name = "AndroidExpensesApp"
include(":app")
