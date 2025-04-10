plugins {
    id("java")
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
}

sourceSets {
    main {
        java {
            srcDir("src")
        }
        resources {
            srcDir("res")
        }
    }
}


group = "gecko10000.backendservercommand"
val versionString = "0.2"
version = versionString

bukkit {
    name = "BackendServerCommand"
    main = "$group.$name"
    version = versionString
    author = "gecko10000"
    apiVersion = "1.13"
    permissions {
        register("backendservercommand.use")
        register("backendservercommand.use.other")
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.register("update") {
    dependsOn(tasks.build)
    doLast {
        exec {
            workingDir(".")
            commandLine("../../dot/local/bin/update.sh")
        }
    }
}
