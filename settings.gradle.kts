rootProject.name = "folia-template"

include("gui")        // menu framework (pagination, animation, chat prompts)
include("addon-api")  // the contract external addon jars compile against
include("plugin")     // the host server plugin (shades the modules above)

// NOTE: addons are no longer part of this build. They're a separate project —
// see the folia-addon-template. To build one against your local changes, run:
//   ./gradlew publishApiLocally
// then point the addon at mavenLocal().
