/**
 * Copyright 2011 wallet.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wallet.viewsystem.swing;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.table.AbstractTableModel;

import org.joda.money.Money;
import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.exchange.CurrencyInfo;
import org.wallet.model.worldcoin.WalletTableData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class WalletTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -937886012854496208L;

    private static final Logger log = LoggerFactory.getLogger(WalletTableModel.class);

    private ArrayList<String> headers;

    private ArrayList<WalletTableData> walletData;

    private final Controller controller;
    private final WorldcoinController worldcoinController;

    public WalletTableModel(WorldcoinController worldcoinController) {
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;

        createHeaders();

        walletData = this.worldcoinController.getModel().createWalletTableData(this.worldcoinController, this.worldcoinController.getModel().getActiveWalletFilename());
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 3 || columnIndex == 4) {
            return Number.class;
        } else {
            return super.getColumnClass(columnIndex);
        }
    }

    @Override
    public int getColumnCount() {
        return headers.size();
    }

    @Override
    public int getRowCount() {
        return walletData.size();
    }

    public WalletTableData getRow(int row) {
        return walletData.get(row);
    }

    @Override
    public String getColumnName(int column) {
        return headers.get(column);
    }

    @Override
    public Object getValueAt(int row, int column) {
        WalletTableData walletDataRow = null;
        if (row >= 0 && row < walletData.size()) {
            walletDataRow = walletData.get(row);
        }
        if (walletDataRow == null) {
            return null;
        }

        switch (column) {
        case 0: {
            if (walletDataRow.getTransaction() != null && walletDataRow.getTransaction().getConfidence() != null) {
                return walletDataRow.getTransaction();
            } else {
                return null;
            }
        }
        case 1: {
            if (walletDataRow.getDate() == null) {
                return new Date(0); // the earliest date (for sorting)
            } else {
                return walletDataRow.getDate();
            }
        }
        case 2:
            return walletDataRow.getDescription();
        case 3:
            // Amount in WDC
            BigInteger debitAmount = walletDataRow.getDebit();
            if (debitAmount != null && debitAmount.compareTo(BigInteger.ZERO) > 0) {
                return controller.getLocaliser().worldcoinValueToString(debitAmount.negate(), false, true);
            }

            BigInteger creditAmount = walletDataRow.getCredit();
            if (creditAmount != null) {
                return controller.getLocaliser().worldcoinValueToString(creditAmount, false, true);
            }
            
            return null;         
        case 4:
            // Amount in fiat
            if (walletDataRow.getDebit() != null  && walletDataRow.getDebit().compareTo(BigInteger.ZERO) > 0) {
                Money debitAmountFiat = CurrencyConverter.INSTANCE.convertFromWDCToFiat(walletDataRow.getDebit());
                if (debitAmountFiat != null) {
                    return CurrencyConverter.INSTANCE.getFiatAsLocalisedString(debitAmountFiat.negated(), false, false);
                }
            }

            Money creditAmountFiat = CurrencyConverter.INSTANCE.convertFromWDCToFiat(walletDataRow.getCredit());
            if (creditAmountFiat != null) {
                return CurrencyConverter.INSTANCE.getFiatAsLocalisedString(creditAmountFiat, false, false);
            }
            
            return "";
        default:
            return null;
        }
    }

    /**
     * Table model is read only.
     */
    @Override
    public void setValueAt(Object value, int row, int column) {
        throw new UnsupportedOperationException();
    }

    public void recreateWalletData() {
        // Recreate the wallet data as the underlying wallet has changed.
        walletData = this.worldcoinController.getModel().createActiveWalletData(this.worldcoinController);
        fireTableDataChanged();
    }

    public void createHeaders() {
        headers = new ArrayList<String>();
        for (int j = 0; j < WalletTableData.COLUMN_HEADER_KEYS.length; j++) {
            if ("sendWorldcoinPanel.amountLabel".equals(WalletTableData.COLUMN_HEADER_KEYS[j])) {
                String header = controller.getLocaliser().getString(WalletTableData.COLUMN_HEADER_KEYS[j]) + " (" + controller.getLocaliser().getString("sendWorldcoinPanel.amountUnitLabel") + ")";
                headers.add(header);
            } else {
                headers.add(controller.getLocaliser().getString(WalletTableData.COLUMN_HEADER_KEYS[j]));                
            } 
        }
        
        // Add in the converted fiat, if appropriate
        if (CurrencyConverter.INSTANCE.isShowingFiat()) {
            CurrencyInfo currencyInfo = CurrencyConverter.INSTANCE.getCurrencyCodeToInfoMap().get(CurrencyConverter.INSTANCE.getCurrencyUnit().getCode());
            String currencySymbol = CurrencyConverter.INSTANCE.getCurrencyUnit().getCode();
            if (currencyInfo != null) {
                currencySymbol = currencyInfo.getCurrencySymbol();
            }
            String header = controller.getLocaliser().getString("sendWorldcoinPanel.amountLabel") + " (" + currencySymbol + ")";
            headers.add(header);
        }
    }
}
