
[![Latest Version](https://img.shields.io/clojars/v/com.github.johnnyjayjay/lein-licenses)](https://clojars.org/com.github.johnnyjayjay/lein-licenses/) [![Open Issues](https://img.shields.io/github/issues/JohnnyJayJay/lein-licenses.svg)](https://github.com/JohnnyJayJay/lein-licenses/issues) [![License](https://img.shields.io/github/license/JohnnyJayJay/lein-licenses.svg)](https://github.com/JohnnyJayJay/lein-licenses/blob/main/LICENSE)


# lein-licenses

A [Leiningen](http://leiningen.org) plugin related to dependency licenses. A fork of [tools-licenses](https://github.com/pmonks/tools-licenses), which in turn is inspired by the already existing (discontinued) [`lein-licenses`](https://github.com/technomancy/lein-licenses/) Leiningen plugin (we've come full circle). 
Similar to tools-licenses, this new plugin has the added benefit of license canonicalisation (leveraging the *excellent* [Software Package Data Exchange (SPDX)](https://spdx.dev/) standard) and the ability to check your project against the [Apache Software Foundation's 3rd Party License Policy](https://www.apache.org/legal/resolved.html).

## Tasks

1. `licenses` - attempt to display the licenses used by all transitive dependencies of the project
2. `check-asf-policy` - attempt to check your project's compliance with the ASF's 3rd Party License Policy

## Using the library

### Documentation

[FAQ is available here](https://github.com/pmonks/tools-licenses/wiki/FAQ).

### Dependency

Add the following to your `project.clj` `:plugins` list:

```edn
[com.github.johnnyjayjay/lein-licenses "0.1.0"]
```

### Use the build tasks

#### `licenses` task

Example summary output:

```
$ lein licenses
This project: Apache-2.0

License                                  Number of Deps
---------------------------------------- --------------
Apache-2.0                               72
BSD-3-Clause                             1
CDDL-1.0                                 1
EPL-1.0                                  35
GPL-2.0-with-classpath-exception         2
LGPL-2.1                                 2
MIT                                      6
NON-SPDX-Public-Domain                   1
```

Use `lein licenses :detailed` to get detailed, per-dependency output (too long to reasonably include here).

If you see `NON-SPDX-Unknown` license identifiers, and/or the task displays a list of dependencies with unknown licenses, **[please raise an issue here](https://github.com/pmonks/lice-comb/issues/new?assignees=pmonks&labels=unknown+licenses&template=Unknown_licenses_tools.md)**.

#### `check-asf-policy` task

Example summary output:

```
$ lein check-asf-policy
Category                       Number of Deps
------------------------------ --------------
Category A                     79
Category A (with caveats)      1
Category B                     32
Creative Commons Licenses      0
Category X                     0
Uncategorised                  0

For more information, please see https://github.com/pmonks/tools-licenses/wiki/FAQ
```

Use `lein check-asf-policy :detailed` to get detailed, per-dependency output (too long to reasonably include here).

## Contributor Information

[Contributor Guidelines](https://github.com/JohnnyJayJay/lein-licenses/blob/main/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/JohnnyJayJay/lein-licenses/issues)

[Code of Conduct](https://github.com/JohnnyJayJay/lein-licenses/blob/main/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

This project uses the [git-flow branching strategy](https://nvie.com/posts/a-successful-git-branching-model/), with the caveat that the permanent branches are called `main` and `dev`, and any changes to the `main` branch are considered a release and auto-deployed (JARs to Clojars, API docs to GitHub Pages, etc.).

For this reason, **all development must occur either in branch `dev`, or (preferably) in temporary branches off of `dev`.**  All PRs from forked repos must also be submitted against `dev`; the `main` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `main` will be rejected.

## License

Copyright © 2021 Peter Monks

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
