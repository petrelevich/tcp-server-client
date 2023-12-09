import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
    id ("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":log-appender"))
    implementation("ch.qos.logback:logback-classic")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("stressClient")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        manifest {
            attributes(mapOf("Main-Class" to "ru.tcp.LogAppenderDemo"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
