plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
    id("maven-publish")
}

group = "com.maximjsx"
version = "1.0.1"

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    implementation("com.google.code.gson:gson:2.12.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveFileName.set("AddonLib-$version.jar")
    exclude(
        "DebugProbesKt.bin",
        "*.SF", "*.DSA", "*.RSA", "META-INF/**", "OSGI-INF/**",
        "deprecated.properties", "driver.properties", "mariadb.properties", "mozilla/public-suffix-list.txt",
        "org/slf4j/**", "org/apache/logging/slf4j/**", "org/apache/logging/log4j/**", "Log4j-*"
    )
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*"))
        exclude(dependency("org.checkerframework:.*"))
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("org.slf4j:.*"))
    }

    minimize();
}

