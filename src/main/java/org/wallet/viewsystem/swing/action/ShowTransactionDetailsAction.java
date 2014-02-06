/**
 * Copyright 2012 wallet.org
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
package org.wallet.viewsystem.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WalletTableData;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.dialogs.TransactionDetailsDialog;
import org.wallet.viewsystem.swing.view.panels.ShowTransactionsPanel;

/**
 * This {@link Action} shows the transaction details dialog
 */
public class ShowTransactionDetailsAction extends AbstractAction {

    private static final long serialVersionUID = 1913592498732457765L;

    private final Controller controller;
    private final WorldcoinController worldcoinController;
    
    private WorldcoinWalletFrame mainFrame;
    private ShowTransactionsPanel showTransactionsPanel;

    /**
     * Creates a new {@link ShowTransactionDetailsAction}.
     */
    public ShowTransactionDetailsAction(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame, ShowTransactionsPanel showTransactionsPanel) {
        super(worldcoinController.getLocaliser().getString("showTransactionsDetailAction.text"), ImageLoader.createImageIcon(ImageLoader.TRANSACTIONS_ICON_FILE));
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        this.showTransactionsPanel = showTransactionsPanel;
        
        this.mainFrame = mainFrame;

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("showTransactionsDetailAction.tooltip"));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("showTransactionsDetailAction.mnemonicKey"));
    }

    /**
     * show the show transaction details dialog
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        WalletTableData rowTableData = showTransactionsPanel.getSelectedRowData();

        final TransactionDetailsDialog transactionDetailsDialog = new TransactionDetailsDialog(this.worldcoinController, mainFrame, rowTableData);
        transactionDetailsDialog.setVisible(true);
        
        // Put the focus back on the table so that the up and down arrows work.
        showTransactionsPanel.getTable().requestFocusInWindow();
    }
}