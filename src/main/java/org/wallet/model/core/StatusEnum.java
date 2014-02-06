package org.wallet.model.core;

public enum StatusEnum {
    ONLINE("worldcoinWalletFrame.onlineText"),
    CONNECTING("worldcoinWalletFrame.offlineText"),
    ERROR("worldcoinWalletFrame.errorText");
    
    private String localisationKey;
    
    private StatusEnum(String localisationKey) {
        this.localisationKey = localisationKey;
      }

    public String getLocalisationKey() {
        return localisationKey;
    }         
}