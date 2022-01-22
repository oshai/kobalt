plugins {
    kotlin("jvm")
}


allprojects {
    apply(plugin = "kotlin")
    configurations.all {
        resolutionStrategy {
            // force log4j version to avoid cve
            force("org.apache.logging.log4j:log4j-core:2.17.0")
        }
    }
    repositories {
        mavenCentral()
        jcenter()
    }
}

subprojects {


}
