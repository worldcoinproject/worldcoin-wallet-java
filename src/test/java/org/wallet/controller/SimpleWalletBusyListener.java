package org.wallet.controller;

import org.wallet.model.worldcoin.WalletBusyListener;

public class SimpleWalletBusyListener implements WalletBusyListener {
    boolean walletBusy = false;

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        walletBusy = newWalletIsBusy;
    }

    boolean isWalletBusy() {
        return walletBusy;
    }
}
