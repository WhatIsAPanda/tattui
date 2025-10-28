plugins {
    application
    id("org.openjfx.javafxplugin") version "0.0.14"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val jfxVersion = "21.0.2"

javafx {
    version = jfxVersion
    modules("javafx.controls", "javafx.graphics", "javafx.fxml")
}

dependencies {
    implementation("de.javagl:obj:0.4.0")
}

application {
    mainClass.set("app.ModelWorkspaceApp")
}

tasks.named<JavaExec>("run") {
    jvmArgs(
        "--add-opens", "javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED",
        "--add-opens", "javafx.graphics/com.sun.javafx.util=ALL-UNNAMED"
    )
}
