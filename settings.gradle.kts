rootProject.name = "tcp-server-client"

include("tcp-client")

include("tcp-server-blocked-demo")
include("tcp-server")
include("tcp-server-demo")
include("tcp-client-stress-test")
include("log-server")
include("log-appender")
include("log-appender-demo")
include("hw-server")

pluginManagement {
    plugins {
        id("fr.brouillard.oss.gradle.jgitver") version "0.10.0-rc03"
        id("io.spring.dependency-management") version "1.1.7"
        id("org.springframework.boot") version "3.4.5"
        id("com.github.johnrengelman.shadow") version "8.1.1"
        id("name.remal.sonarlint") version "6.0.0-rc-2"
        id("com.diffplug.spotless") version "8.0.0"
    }
}