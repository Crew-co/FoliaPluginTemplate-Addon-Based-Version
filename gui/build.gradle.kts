// Standalone menu framework: depends only on the Folia API (from the root build),
// so the host plugin AND external addons can both use it.
//
// Published because addon-api exposes it as an `api` dependency — an external
// addon resolving folia-template-addon-api must be able to resolve this too.

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "folia-template-gui"
        }
    }
}
