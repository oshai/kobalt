include(
  ":kobalt-plugin-api",
  ":wrapper",
  ":kobalt"
)

rootProject.name = "kobalt-project"
project(":kobalt-plugin-api").projectDir = file("modules/kobalt-plugin-api")
project(":wrapper").projectDir = file("modules/wrapper")
project(":kobalt").projectDir = file("modules/kobalt")

pluginManagement {
  plugins {
    kotlin("jvm") version "1.6.10"
  }
}