// The stable contract addon jars compile against.
//
// Addons must declare this `compileOnly` and NEVER shade it: the host loads
// addons with a classloader whose parent is the host's, so these classes must
// resolve to the host's class objects. A shaded copy = a different class with
// the same name = `isAssignableFrom` fails and the addon won't load.
//
// The `maven-publish` plugin and the GitHub Packages repo are applied from the
// root build (see `apiModules` there).

dependencies {
    // `api` so AddonSchedulers can extend MenuSchedulers — addons pass
    // context.schedulers straight into a Menu with no cast, and get the GUI
    // framework transitively. This is why gui must be published too.
    api(project(":gui"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "folia-template-addon-api"
        }
    }
}
