package org.wallet.file;

import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.exchange.CurrencyInfo;
import org.wallet.model.worldcoin.WalletTableData;

import com.googlecode.jcsv.writer.CSVEntryConverter;

/**
 * Create a CSVEntryConverter for the header values in the CSV
 */
public class WalletTableDataHeaderEntryConverter implements CSVEntryConverter<WalletTableData> {

    WorldcoinController worldcoinController = null;

    @Override
    public String[] convertEntry(WalletTableData walletTableData) {
        String[] columns = new String[5];

        // Date.
        columns[0] = worldcoinController.getLocaliser().getString("walletData.dateText");
        
        // Description.
        columns[1] = worldcoinController.getLocaliser().getString("walletData.descriptionText");

        // Amount in WDC.
        columns[2] = worldcoinController.getLocaliser().getString("sendWorldcoinPanel.amountLabel") + " (" + worldcoinController.getLocaliser().getString("sendWorldcoinPanel.amountUnitLabel") + ")";;
        
        // Amount in fiat
        if (CurrencyConverter.INSTANCE.isShowingFiat()) {
            CurrencyInfo currencyInfo = CurrencyConverter.INSTANCE.getCurrencyCodeToInfoMap().get(CurrencyConverter.INSTANCE.getCurrencyUnit().getCode());
            String currencySymbol = CurrencyConverter.INSTANCE.getCurrencyUnit().getCode();
            if (currencyInfo != null) {
                currencySymbol = currencyInfo.getCurrencySymbol();
            }
            columns[3] = worldcoinController.getLocaliser().getString("sendWorldcoinPanel.amountLabel") + " (" + currencySymbol + ")";
        } else {
            columns[3] = "";
        }
         
        // Transaction hash.
        columns[4] = worldcoinController.getLocaliser().getString("exportTransactionsSubmitAction.transactionId");

        return columns;
    }

    public void setWorldcoinController(WorldcoinController worldcoinController) {
        this.worldcoinController = worldcoinController;
    }
}
