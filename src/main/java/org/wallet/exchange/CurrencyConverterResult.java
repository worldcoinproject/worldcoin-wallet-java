package org.wallet.exchange;

import org.joda.money.Money;

/**
 * A pojo to store the result of a currency conversion.
 * 
 * The fiat and wdc are kept separate just to avoid mixing them up accidentally.
 */
public class CurrencyConverterResult {
    
    private boolean fiatMoneyValid;
    
    private Money fiatMoney;
    
    private String fiatMessage;
    
    private boolean wdcMoneyValid;
    
    private Money wdcMoney;
    
    private String wdcMessage;

    public CurrencyConverterResult() {
        fiatMoneyValid = false;
        fiatMoney = null;
        fiatMessage = null;
        
        wdcMoneyValid = false;
        wdcMoney = null;
        wdcMessage = null;
    }

    public boolean isFiatMoneyValid() {
        return fiatMoneyValid;
    }

    public void setFiatMoneyValid(boolean fiatMoneyValid) {
        this.fiatMoneyValid = fiatMoneyValid;
    }

    public Money getFiatMoney() {
        return fiatMoney;
    }

    public void setFiatMoney(Money fiatMoney) {
        this.fiatMoney = fiatMoney;
    }

    public String getFiatMessage() {
        return fiatMessage;
    }

    public void setFiatMessage(String fiatMessage) {
        this.fiatMessage = fiatMessage;
    }

    public boolean isWdcMoneyValid() {
        return wdcMoneyValid;
    }

    public void setWdcMoneyValid(boolean wdcMoneyValid) {
        this.wdcMoneyValid = wdcMoneyValid;
    }

    public Money getWdcMoney() {
        return wdcMoney;
    }

    public void setWdcMoney(Money wdcMoney) {
        this.wdcMoney = wdcMoney;
    }

    public String getWdcMessage() {
        return wdcMessage;
    }

    public void setWdcMessage(String wdcMessage) {
        this.wdcMessage = wdcMessage;
    }
}
