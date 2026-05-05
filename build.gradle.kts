import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
    `maven-publish`
    signing
}

group = "app.morphe"

// ============================================================================
// JVM / Kotlin Configuration
// ============================================================================
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_21
}

// ============================================================================
// Application Entry Point
// ============================================================================
// Shadow JAR reads this for Main-Class manifest attribute.
//
//   No args / double-click  →  GUI (Compose Desktop)
//   With args (terminal)    →  CLI (PicoCLI)
application {
    mainClass = "app.morphe.cli.command.MainCommandKt"
}

// ============================================================================
// Repositories
// ============================================================================
repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven {
        // A repository must be specified for some reason. "registry" is a dummy.
        url = uri("https://maven.pkg.github.com/MorpheApp/registry")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    // Obtain baksmali/smali from source builds - https://github.com/iBotPeaches/smali
    // Remove when official smali releases come out again.
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    api(libs.morphe.patcher)
    implementation(libs.arsclib)
    implementation(libs.morphe.library)
    implementation(libs.picocli)

    // -- Async / Serialization ---------------------------------------------
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // -- Testing -----------------------------------------------------------
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.params)
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "SKIPPED", "FAILED")
        }
    }

    processResources {
        filesMatching("**/*.properties") {
            expand("projectVersion" to project.version)
        }
        from(arrayOf(rootProject.file("NOTICE"), rootProject.file("LICENSE"))) {
            into("META-INF")
        }
    }

    shadowJar {
        exclude(
            "/prebuilt/linux/aapt",
            "/prebuilt/windows/aapt.exe",
            "/prebuilt/*/aapt_*",
        )
        minimize {
            exclude(dependency("org.bouncycastle:.*"))
            exclude(dependency("com.github.REAndroid:ARSCLib"))
            exclude(dependency("app.morphe:morphe-patcher"))
        }
    }
}

// ============================================================================
// Publishing / Signing
// ============================================================================
// Needed by gradle-semantic-release-plugin.
// Tracking: https://github.com/KengoTODA/gradle-semantic-release-plugin/issues/435

// The maven-publish is also necessary to make the signing plugin work.
publishing {
    repositories {
        mavenLocal()
    }

    publications {
        create<MavenPublication>("morphe-cli-publication") {
            from(components["java"])
        }
    }
}

signing {
    useGpgCmd()

    sign(publishing.publications["morphe-cli-publication"])
}
