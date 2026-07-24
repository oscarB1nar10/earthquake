pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // PREFER_SETTINGS (not FAIL_ON_PROJECT_REPOS): a machine-level init script
    // (~/.gradle/nexus-gradle7.gradle) injects MavenLocal into every build;
    // this mode keeps the repos below authoritative and ignores injected ones
    // instead of failing the build.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EarthquakeAlarm"
include(":app")
