// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.roborazzi) apply false
}

// Proprietary Google components are not welcome in this build. firebase-ai
// was pulled in unused and dragged play-services-base, play-services-basement
// and play-services-tasks along with it, which is exactly the kind of thing
// that reappears quietly during a dependency bump. Fail the build instead.
val bannedDependencyGroups = listOf(
  "com.google.android.gms",
  "com.google.firebase",
  "com.google.android.play",
  "com.google.android.datatransport",
  "com.android.installreferrer",
)

subprojects {
  configurations.configureEach {
    resolutionStrategy.eachDependency {
      val group = requested.group
      if (bannedDependencyGroups.any { group == it || group.startsWith("$it.") }) {
        throw GradleException(
          "Blocked proprietary Google dependency: ${requested.group}:${requested.name}. " +
            "This build ships without Google Play Services. If a library pulls this in " +
            "transitively, either exclude it or find a replacement."
        )
      }
    }
  }
}
