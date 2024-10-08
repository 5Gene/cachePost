import wing.publishJavaMavenCentral

plugins {
    alias(vcl.plugins.kotlin.jvm)
}

buildscript {
    dependencies {
        classpath(vcl.gene.conventions)
    }
}


group = "io.github.5gene"
version = "1.0"

publishJavaMavenCentral("okhttp cache post request")

dependencies {
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp)
}