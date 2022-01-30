plugins {
    kotlin("jvm")
}


allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
}

subprojects {

    apply(plugin = "kotlin")
    configurations.all {
        resolutionStrategy {
            // force log4j version to avoid cve
            force("org.apache.logging.log4j:log4j-core:2.17.0")
        }
    }
}