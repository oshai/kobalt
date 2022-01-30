val bndlib: String by project
val findbugs: String by project
val groovy: String by project
val gson: String by project
val guice: String by project
val inject: String by project
val jaxb: String by project
val jcommander: String by project
val kotlin: String by project
val maven: String by project
val mavenResolver: String by project
val okhttp: String by project
val okio: String by project
val retrofit: String by project
val slf4j: String by project
val spark: String by project
val testng: String by project
val junit: String by project
val junitJupiter: String by project
val junitPlatform: String by project

dependencies {
    implementation(project(":wrapper"))
    implementation(project(":kobalt-plugin-api"))
    implementation("biz.aQute.bnd:biz.aQute.bndlib:$bndlib")
    implementation("com.github.spullara.mustache.java:compiler:0.9.5")
    implementation("com.google.code.findbugs:jsr305:$findbugs")
    implementation("com.sparkjava:spark-core:$spark")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttp")
    implementation("com.sun.activation:javax.activation:1.2.0")
    implementation("com.sun.xml.bind:jaxb-core:$jaxb")
    implementation("com.sun.xml.bind:jaxb-impl:$jaxb")
    implementation("javax.inject:javax.inject:$inject")
    implementation("javax.xml.bind:jaxb-api:$jaxb")
    implementation("org.apache.maven.resolver:maven-resolver-spi:$mavenResolver")
    implementation("org.codehaus.groovy:groovy:$groovy")
    implementation("com.beust:jcommander:$jcommander")
    implementation("com.google.code.gson:gson:$gson")
    implementation("com.google.inject:guice:$guice")
    implementation("com.google.inject.extensions:guice-assistedinject:$guice")
    implementation("com.squareup.retrofit2:converter-gson:$retrofit")
    implementation("com.squareup.retrofit2:retrofit:$retrofit")
    implementation("org.apache.maven:maven-model:$maven")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin")
    testImplementation("org.assertj:assertj-core:3.8.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin")
    testImplementation("org.testng:testng:$testng")
}


tasks.test {
    useTestNG {
        preserveOrder = true
    }
}