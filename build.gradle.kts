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
        "javafx.web"
    )
}

dependencies {
    implementation("de.javagl:obj:0.4.0")
    // implementation("mysql:mysql-connector-j:8.1.0")
}

application {
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
   // 👇 This is the class Gradle will launch for `./gradlew run`
    mainClass.set("app.Main")
}

tasks.named<JavaExec>("run") {
    // These --add-opens flags are sometimes needed by JavaFX internals
    jvmArgs(
        "--add-opens", "javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED",
        "--add-opens", "javafx.graphics/com.sun.javafx.util=ALL-UNNAMED"
    )
}
