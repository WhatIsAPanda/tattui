plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.14"
}

java {
    toolchain {
        // You are on Java 25 locally, so tell Gradle to compile/run with 25
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val jfxVersion = "21.0.2"

javafx {
    version = jfxVersion
    modules = listOf(
        "javafx.controls",
        "javafx.graphics",
        "javafx.fxml",
        "javafx.web",
        "javafx.swing"
    )
}

dependencies {
    implementation("de.javagl:obj:0.4.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")
    implementation("com.gluonhq:maps:2.0.0-ea+6")
    implementation("org.json:json:20240303")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
    mainClass.set("app.Main")
}

tasks.named<JavaExec>("run") {
    jvmArgs(
        "--add-opens", "javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED",
        "--add-opens", "javafx.graphics/com.sun.javafx.util=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
}
