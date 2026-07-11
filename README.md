# Folia 1.21.11 Kotlin Plugin Template — Addon System

A Folia plugin that generates an `addons/` folder and **dynamically loads addon jars** at runtime, exposing a small, stable API they compile against.

- Target: `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` (Java 21)
- Build: Gradle (Kotlin DSL), Shadow fat jar
- The API is published to GitHub Packages, so addon projects can depend on it

```bash
./gradlew build              # → plugin/build/libs/FoliaTemplate-1.0.0.jar
./gradlew :plugin:runFolia   # test server with the plugin loaded
./gradlew publishApiLocally  # publish the API to ~/.m2 for local addon dev
```

---

## Modules

```
folia-template/
├── addon-api/  → the small, stable contract addons compile against. PUBLISHED.
└── plugin/     → the plugin itself: addon loading, classloaders, commands.
```

Two modules for one reason: addon developers should compile against a *small, stable* API, not your whole plugin. `addon-api` is the only thing published.

Anything an addon **writes against** lives in `addon-api` (`Addon`, `AddonContext`, `AddonSchedulers`, the `@Command` annotations, `CommandContext`). The machinery that **registers** it stays in `plugin` (`AddonManager`, `CommandManager`, the classloaders).

> **Note:** the plugin jar builds to `plugin/build/libs/`, not the root `build/libs/` — worth knowing if you edit the CI paths.

---

## The addon system

The plugin creates `plugins/FoliaTemplate/addons/` on first run. Drop a jar in there and the host discovers, loads, and enables it on startup (or on `/addons reload`).

### What the host handles for you

- **Discovery + load order** — jars are scanned, each `addon.yml` is parsed, and addons are topologically sorted by `depends`, so a dependency always enables first. Cycles and missing deps are logged and skipped, not fatal.
- **Version gate** — an addon whose `api-version` doesn't match `HostApi.ADDON_API_VERSION` is refused with a clear message, rather than failing later with a cryptic `NoSuchMethodError`. Bump that constant when you break the API.
- **Cleanup on unload** — every listener, scheduled task, and service registered through `AddonContext` is tracked per-addon and undone on disable/reload. This matters on Folia: a leaked repeating task would keep running against a dead classloader.
- **Services** — `registerService<T>(impl)` / `service<T>()` lets addons expose and consume each other's APIs without hard-linking.
- **Commands** — `/addons` lists what's loaded; `/addons reload` hot-reloads them all.

> **Caveat:** addon-registered commands go into Bukkit's command map and aren't removed on reload, so a reloaded addon re-registers them (newest wins). Fine for dev iteration; restart for production changes.

### Writing an addon

An addon is a jar with an `addon.yml` and a class extending `AddonBase`:

```kotlin
class MyAddon : AddonBase() {
    override fun onEnable() {
        context.registerCommand(MyCommand(this))   // host's @Command annotations
        context.registerListener(MyListener())     // auto-unregistered on unload
        context.registerService<MyService>(impl)   // other addons can consume this
        context.schedulers.async { /* Folia-safe; auto-cancelled on unload */ }
        // context.dataFolder → plugins/FoliaTemplate/addons/MyAddon/
    }
}
```

Use the companion **folia-addon-template** to build one — it's pre-wired for all of this.

> **The API must be `compileOnly` in an addon, never shaded.** The host loads addons with a classloader whose *parent* is the host's, so `Addon`/`Menu`/etc. must resolve to the host's class objects. A shaded copy is a different class with the same name — `isAssignableFrom` fails and the addon won't load. (Same class-identity trap that bites people with Vault.)

### Publishing the API

Addon developers compile against your published API and never clone this repo.

**Set up once** — put your repo in `gradle.properties`:
```properties
githubRepo=YourName/folia-template
```

**Publish by pushing a tag:**
```bash
git tag v1.0.0 && git push origin v1.0.0
```

`.github/workflows/publish.yml` builds, tests, and publishes `addon-api` + `gui` to GitHub Packages, versioned from the tag. No secrets to configure — the built-in `GITHUB_TOKEN` covers it. (Both modules are published because `addon-api` exposes `gui` as an `api` dependency.)

Addons then depend on it:
```kotlin
compileOnly("com.example:folia-template-addon-api:1.0.0")
```

For local testing without publishing: `./gradlew publishApiLocally` (→ your `~/.m2`).

> Tell your addon devs: GitHub Packages needs a token **even for public packages** (a PAT with `read:packages`). The addon template documents this — it's the #1 source of confusion.

---

## Folia threading rules (the short version)

There is **no main thread**. Schedule onto whatever owns the thing you touch:

| Helper | Runs on | Use for |
|---|---|---|
| `schedulers.global { }` | global region | world-wide state, your own shared data |
| `schedulers.region(loc) { }` | region owning `loc` | blocks/world at a location |
| `schedulers.entity(e) { }` | region owning `e` | a player/entity, opening inventories |
| `schedulers.async { }` | background thread | HTTP/DB/disk — never world state |

Command and event handlers already run on the region owning the relevant player/block, so touching them there is safe. Shared plugin state is **not** automatically safe — confine it to one thread (usually global) or use concurrent types.
