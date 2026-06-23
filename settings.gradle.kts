import java.io.File
import java.util.Properties

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
}

// GitHub Packages credentials for the FastPix upload SDK — read from the
// gitignored github.properties (env-var fallback for CI). Never committed.
val githubProperties = Properties().apply {
    val f = File(settingsDir, "github.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val githubUser: String = githubProperties.getProperty("gpr.user") ?: System.getenv("GPR_USER") ?: ""
val githubKey: String = githubProperties.getProperty("gpr.key") ?: System.getenv("GPR_KEY") ?: ""

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://dl.cloudsmith.io/public/cometchat/cometchat/maven/")
        maven("https://maven.pkg.github.com/FastPix/android-uploads-sdk") {
            credentials {
                username = githubUser
                password = githubKey
            }
        }
    }
}

rootProject.name = "amatyma"
include(":app")
