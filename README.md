# Folia 1.21.11 Kotlin Template — Addons + GUI Framework

A multi-module Folia plugin template with a **runtime addon system** (third-party jars hook into your plugin) and a **GUI framework** (pagination, live/animated items, chat prompts, confirm dialogs).

- Target: `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` (Java 21)
- Build: Gradle (Kotlin DSL), multi-module, Shadow fat jar

```bash
./gradlew build                    # builds everything
./gradlew :plugin:runFolia         # test server with the plugin loaded
./gradlew publishApiLocally        # publish addon-api + gui for addon developers
```

---

## Modules

```
folia-template/
├── gui/        → menu framework. Standalone: depends only on the Folia API.
├── addon-api/  → the public contract: Addon/AddonContext, the @Command
│                 annotations + CommandContext, and (via gui) the menu classes.
└── plugin/     → the host server plugin. Shades gui + addon-api, and holds the
                  internals (CommandManager, AddonManager, classloading).
```

The split is deliberate: anything an addon *writes against* lives in `addon-api`, while the machinery that *registers* it stays in `plugin`. That's why `@Command`/`CommandContext` are in the API but `CommandManager` isn't.

Dependency direction is one-way (`gui` ← `addon-api` ← `plugin`), so the GUI framework and the addon API can each be reused without dragging the plugin along.

**Addons are a separate project.** See the companion **folia-addon-template** — addon developers build against your *published* `addon-api` artifact and never need to clone this repo.

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

## The GUI framework

All menus are **holder-based**: a `Menu` *is* its inventory's `InventoryHolder`, so `MenuListener` finds it with `topInventory.holder is Menu`. There's no global open-menus registry to synchronize — which removes the biggest source of GUI race conditions on Folia by construction.

Create **one instance per player per open**; `open()`/`refresh()`/clicks all run on that player's region thread, so a menu is effectively single-threaded and needs no locks.

### Basic menu
```kotlin
class MyMenu(plugin: FoliaTemplatePlugin) : Menu(plugin.schedulers, mm("<gold>Title"), rows = 3) {
    override fun build(player: Player) {
        button(13, icon(Material.DIAMOND, "<aqua>Click me", "<gray>lore line")) { ctx ->
            ctx.reply("<green>Clicked!")
            ctx.close()
        }
        border(icon(Material.GRAY_STAINED_GLASS_PANE, " "))
    }
}
MyMenu(plugin).open(player)
```

### Pagination
Extend `PaginatedMenu<T>` — nav buttons, page state, and clamping are handled:
```kotlin
class KitsMenu(plugin: FoliaTemplatePlugin, val kits: List<Kit>) :
    PaginatedMenu<Kit>(plugin.schedulers, mm("<gold>Kits"), rows = 6) {
    override fun items() = kits
    override fun render(item: Kit) = icon(item.material, "<yellow>${item.name}")
    override fun onClick(item: Kit, context: ClickContext) { item.give(context.player) }
    override fun decorate(player: Player) { /* extra nav-row buttons */ }
}
```

### Live / animated items
`refresh()` redraws **in place** — the inventory stays open, so there's no flicker and no cursor reset (unlike re-opening). `animate(periodTicks)` does it on a timer, on the viewer's region thread, and stops itself when the menu closes:
```kotlin
override fun onOpen(player: Player) {
    animate(periodTicks = 5L) { frame -> spinnerFrame = frame }  // redraws 4x/sec
}
```

### Chat input prompts
```kotlin
ChatPrompt.ask(
    plugin.prompts, ctx.player,
    prompt = "<yellow>Type an amount (or 'cancel'):",
    onInput = { player, text -> /* ... */ MyMenu(plugin).open(player) },
    onCancel = { player -> MyMenu(plugin).open(player) },
)
```
Chat fires **async** on Folia, so pending prompts live in a `ConcurrentHashMap` and the callback is hopped back onto the player's region thread before it runs — meaning you can safely open menus and touch the player inside `onInput`.

### Confirm dialogs
```kotlin
ConfirmMenu(plugin.schedulers, mm("<red>Are you sure?"),
    onConfirm = { p -> doIt(p) },
    onDeny = { p -> MyMenu(plugin).open(p) },
).open(player)
```

`/menu` opens `DemoMenu`, which exercises all four features at once.

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
