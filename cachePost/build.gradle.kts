import wing.GroupIdMavenCentral
import wing.publishJavaMavenCentral

plugins {
    alias(vcl.plugins.kotlin.jvm)
}

buildscript {
    dependencies {
        classpath(vcl.gene.conventions)
    }
}


group = GroupIdMavenCentral
version = libs.versions.gene.cache.post.get()

publishJavaMavenCentral("okhttp cache post request")

dependencies {
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp)
}