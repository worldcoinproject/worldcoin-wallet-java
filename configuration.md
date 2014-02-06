### Worldcoin Wallet configuration options

All these options are set in the main control file `worldcoin-wallet.properties`. Worldcoin Wallet looks in a few places for this file in the following order:

1. Current working directory when launched. This is for backwards compatibility and for running from a USB drive.

2. (Mac OS X only) Four directory levels up. This is for running from a USB drive, but outside the OSX `.app` directory.
 
3. The operating system's standard application data directory:

#### Windows

* `System.getenv("APPDATA")/WorldcoinWallet`
* Example: `C:/Documents and Settings/Administrator/Application Data/WorldcoinWallet`

#### Mac OS X

* `System.getProperty("user.home")/Library/Application Support/WorldcoinWallet`
* Example: `/Users/jim/Library/Application Support/WorldcoinWallet`

#### Linux

* `System.getProperty("user.home")/WorldcoinWallet`
* Example: `/Users/jim/WorldcoinWallet`

Wherever this file is found, that directory is used as the application data directory for Multibit.


#### Connect to specific peers

If you want to connect to specific peers set `peers=<comma separated list of peers to connect to>`

The list of peers can be specified using domain names (`www.myNode.com`) or IP addresses. Example:

```
peers=173.242.119.177, 176.9.42.247, 217.79.19.226, 98.216.173.54
```

(The older property `singleNodeConnection` is still honoured but is now deprecated. Use 'peers' instead).

Note: ONLY these peers will be used for connections.

#### Testnet

To use Testnet set `testOrProductionNetwork=test`.

To use Testnet3 (recommended) set `testOrProductionNetwork=testnet3`.
