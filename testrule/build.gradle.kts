plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("junit:junit:4.13.2")
    implementation("com.squareup.okhttp3:mockwebserver:4.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.S01)
    signAllPublications()
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            moduleName.set("Retrofit Test Rule")
            includes.from("Module.md")
        }
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(File(rootProject.rootDir, "docs"))
}