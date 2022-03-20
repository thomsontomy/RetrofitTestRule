plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("junit:junit:4.13.2")
    implementation("com.squareup.okhttp3:mockwebserver:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}