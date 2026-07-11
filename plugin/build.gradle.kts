plugins {
    id("com.gradleup.shadow")
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

val foliaApiVersion = rootProject.property("foliaApiVersion") as String
val minecraftVersion = foliaApiVersion.substringBefore("-R")

dependencies {
    // `api` (not implementation) so these modules' classes are exported to the
    // addon classloaders — addons must see the SAME Addon/Menu classes the host
    // uses, or `instanceof Addon` fails despite matching names.
    api(project(":addon-api"))
    api(project(":gui"))

    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(21)
}

paper {
    name = "FoliaTemplate"
    main = "com.example.foliatemplate.FoliaTemplatePlugin"
    apiVersion = "1.21"
    foliaSupported = true // REQUIRED — Folia won't load the plugin without it
    authors = listOf("YourName")
    description = "Folia Kotlin template with an addon system and GUI framework."
}

tasks {
    shadowJar {
        archiveBaseName.set("FoliaTemplate")
        archiveClassifier.set("")
        // NOTE: do NOT relocate the addon-api or gui packages. Addons compile
        // against those exact package names; relocating would rename them at
        // runtime and every addon would fail to load.
        //
        // Relocating the Kotlin runtime is fine (it isn't part of the addon API
        // surface) — but if your addons are written in Kotlin and rely on the
        // host's stdlib, leave it alone. Safest default: leave it unrelocated.
    }
    build { dependsOn(shadowJar) }
    jar { enabled = false }

    runServer { minecraftVersion(minecraftVersion) }
}

runPaper {
    folia {
        registerTask() // ./gradlew :plugin:runFolia
    }
}
