import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
    id ("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":tcp-server"))
    implementation("ch.qos.logback:logback-classic")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("tcpServerDemo")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        manifest {
            attributes(mapOf("Main-Class" to "ru.tcp.demo.TcpServerDemo"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
