

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
    implementation("biz.aQute.bnd:biz.aQute.bndlib:$bndlib")
    implementation("com.google.code.findbugs:jsr305:$findbugs")
    implementation("com.sparkjava:spark-core:$spark")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttp")
    implementation("commons-io:commons-io:2.6")
    implementation("io.reactivex:rxjava:1.3.3")
    implementation("javax.inject:javax.inject:$inject")
    implementation("javax.xml.bind:jaxb-api:$jaxb")
    implementation("org.apache.commons:commons-compress:1.15")
    implementation("org.apache.maven:maven-aether-provider:3.3.9")
    implementation("org.apache.maven.resolver:maven-resolver-api:$mavenResolver")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:$mavenResolver")
    implementation("org.apache.maven.resolver:maven-resolver-impl:$mavenResolver")
    implementation("org.apache.maven.resolver:maven-resolver-spi:$mavenResolver")
    implementation("org.apache.maven.resolver:maven-resolver-transport-file:$mavenResolver")
    implementation("org.apache.maven.resolver:maven-resolver-transport-http:$mavenResolver")
    implementation("org.apache.maven.resolver:maven-resolver-util:$mavenResolver")
    implementation("org.codehaus.groovy:groovy:$groovy")
    implementation("org.eclipse.jgit:org.eclipse.jgit:4.9.0.201710071750-r")
    implementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiter")
    implementation("org.junit.platform:junit-platform-console:$junitPlatform")
    implementation("org.junit.platform:junit-platform-engine:$junitPlatform")
    implementation("org.junit.platform:junit-platform-runner:$junitPlatform")
    implementation("org.junit.platform:junit-platform-surefire-provider:$junitPlatform")
    implementation("org.junit.vintage:junit-vintage-engine:$junitJupiter")
    implementation("org.slf4j:slf4j-simple:$slf4j")
    implementation("org.testng:testng:$testng")
    implementation("org.testng.testng-remote:testng-remote:1.3.0")
    implementation("com.beust:jcommander:$jcommander")
    implementation("com.google.code.gson:gson:$gson")
    implementation("com.google.inject:guice:$guice")
    implementation("com.google.inject.extensions:guice-assistedinject:$guice")
    implementation("com.squareup.okio:okio:$okio")
    implementation("com.squareup.retrofit2:converter-gson:$retrofit")
    implementation("com.squareup.retrofit2:retrofit:$retrofit")
    implementation("org.apache.maven:maven-model:$maven")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin")
}

tasks.test {
    useTestNG {
        preserveOrder = true
    }
}

