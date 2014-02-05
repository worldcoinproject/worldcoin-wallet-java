package org.wallet.viewsystem.dataproviders;


/**
 * DataProvider for send worldcoin and send worldcoin confirm action
 * @author jim
 *
 */
public interface WorldcoinFormDataProvider extends DataProvider {
    /**
     * Get the address
     */
    public String getAddress();
    
    /**
     * Get the label
     */
    public String getLabel();
    
    /**
     * Get the amount (denominated in WDC).
     */
    public String getAmount();
    
    /**
     * Get the amount (denominated in fiat)
     */
    public String getAmountFiat();
}
