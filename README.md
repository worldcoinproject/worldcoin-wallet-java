### Introduction

Worldcoin Wallet is a Simplified Payment Verification (SPV) Worldcoin desktop client.

Worldcoin Wallet relies on the following technologies:

* Maven as the build system, so the usual Maven processes apply. If you're not familiar
with Maven then [download it first](http://maven.apache.org) and follow their installation instructions.
* [ZXing ("Zebra Crossing")](https://code.google.com/p/zxing/) for QR codes
* [Worldcoinj](https://code.google.com/p/worldcoinj/) for access to the Worldcoin network
* IzPack for creating installers for Windows, Mac, Linux
* [Worldcoinj Enforcer Rules](https://github.com/gary-rowe/WorldcoinjEnforcerRules) to prevent dependency chain attacks
* [XChange](https://github.com/timmolter/XChange) for access to several Worldcoin exchanges

#### A note on the Worldcoinj dependency

Worldcoin Wallet depends on Worldcoinj for its Worldcoin support.
```
https://github.com/worldcoinproject/worldcoinj
```

Once cloned, you should then install the custom Worldcoinj library using

```
mvn clean install -DskipTests=true
```

note that the unit tests are not working right now since the branching from multibit. So to use maven, make sure you disable tests using -DskipTests=true

### Branching strategy

This follows the  [master-develop](http://nvie.com/posts/a-successful-git-branching-model/) pattern.

There are 2 main branches: `master` and `develop`. The `master` branch is exclusively for releases, while the `develop`
is exclusively for release candidates. The `develop` branch always has a Maven version of `develop-SNAPSHOT`.

Every GitHub Issue gets a branch off develop. When it is complete and code reviewed it is merged into `develop`.

When sufficient Issues are merged into `develop` to justify a release, a new branch off `develop` is created with the release number (e.g. `release-1.2.3`).
The Maven `pom.xml` is updated to reflect the snapshot version (e.g. `1.2.3-SNAPSHOT`).

Once the release has been tested and is ready to go live, the final act is to update the `pom.xml` to remove the SNAPSHOT suffix and merge it into `master`.

The `master` branch is then tagged with the release number. Tags are in the format `v1.2.3` to distinguish them from branch names.

An announcement is made on the Worldcoin Wallet website to alert everyone that a new version is available.

### Maven build targets

The important targets are:

```
mvn clean package -DskipTests=true
```

which will package the Worldcoin Wallet project into `worldcoin-wallet-x.y.z.jar` where `x.y.z` is the current version
number. This is suitable for local development work.

If you want to generate a complete set of multi-platform installers (Windows, Mac and Linux) you 
use the following command

```
maven clean install -DskipTests=true
```

After some processing, you will have the following artifacts in the target directory:

* an executable jar = worldcoin-wallet-exe.jar
* a Mac application bundle = Worldcoin Wallet.app
* a Mac DMG file = worldcoin-wallet-x.y.z.dmg
* an installer for Windows = worldcoin-wallet-x.y.z-windows.exe
* an installer for Linux = worldcoin-wallet-x.y.z-linux.jar

To run Worldcoin Wallet from these artifacts you can follow the instructions [provided on the main Worldcoin Wallet
website](https://worldcoin-wallet.org/help.html)

### Worldcoin Wallet contains cut down JREs so is a large clone

The Worldcoin Wallet installers contain cut down JREs so the project clone is quite large.
(100 to 200 MB).

### Custom configuration

Worldcoin Wallet is quite flexible and has several features only accessible to power users through the configuration file. This
is discussed in more detail in [configuration.md](configuration.md)

### Contributing

Worldcoin Wallet is a very active project and if you would like to contribute please feel free to get in touch using [jim@worldcoin-wallet.org](mailto:jim@worldcoin-wallet.org).
We are particularly looking for developers with the following skills to contribute:

* Experienced Java programmers
* Web designers [for the website](https://github.com/jim618/worldcoin-wallet-website)
* Translators using the [Crowdin site](http://translate.worldcoin-wallet.org/)
* Beta testers for checking the latest pre-release

All contributors must be OK with releasing their work under the MIT license.