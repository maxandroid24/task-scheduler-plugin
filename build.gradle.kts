plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.scheduler"
version = "1.0.4"

kotlin {
    jvmToolchain(17)
}


repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        intellijDependencies()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.3")
        bundledPlugin("org.jetbrains.plugins.terminal")
        instrumentationTools()
    }
}

// Configure IntelliJ Platform SDK Integration
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("233")
            untilBuild.set(provider { null })
        }
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

