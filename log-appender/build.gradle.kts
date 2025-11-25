import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
    id ("com.github.johnrengelman.shadow")
}

dependencies {
    implementation("ch.qos.logback:logback-classic")
    implementation ("org.apache.kafka:kafka-clients")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("logAppender")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}
