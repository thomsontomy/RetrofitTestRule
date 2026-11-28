plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

/**
 * Pin Kotlin and Java to the same JVM target. Without an explicit toolchain the Kotlin plugin
 * inherits the JVM running Gradle (17+), which does not match `sourceCompatibility = 1.8`.
 */
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Public API surface: these types appear in the rule's signatures, so consumers need them
    // on their compile classpath as well as at runtime.
    api("junit:junit:4.13.2")
    api("com.squareup.okhttp3:okhttp:4.11.0")
    api("com.squareup.okhttp3:mockwebserver:4.11.0")
    api("com.squareup.retrofit2:retrofit:2.9.0")

    // Implementation detail: exposed only through the default converter factories and the
    // built-in structural JSON body matcher.
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.15.0")

    // Optional: the JUnit 5 extension is only loaded when a consumer already has JUnit 5.
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.10.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.S01)
    signAllPublications()
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            // Must be a valid package-ish name: spaces produce broken output directories.
            moduleName.set("RetrofitTestRule")
            includes.from("Module.md")
        }
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(File(rootProject.rootDir, "docs"))
}
