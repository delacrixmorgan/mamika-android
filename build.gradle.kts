buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.11")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://maven.google.com")
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}