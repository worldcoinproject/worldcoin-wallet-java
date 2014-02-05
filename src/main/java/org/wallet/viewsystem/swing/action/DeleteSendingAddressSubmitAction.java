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
package org.wallet.viewsystem.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JTable;

import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WalletAddressBookData;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WalletInfoData;
import org.wallet.store.WorldcoinWalletVersion;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.swing.view.dialogs.DeleteSendingAddressConfirmDialog;
import org.wallet.viewsystem.swing.view.models.AddressBookTableModel;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link Action} represents an action to delete a sending address.
 */
public class DeleteSendingAddressSubmitAction extends WorldcoinWalletSubmitAction {

    private static final long serialVersionUID = 200111999465875405L;

    private static final Logger log = LoggerFactory.getLogger(DeleteSendingAddressSubmitAction.class);

    private SendWorldcoinPanel sendWorldcoinPanel;
    private DeleteSendingAddressConfirmDialog deleteSendingAddressConfirmDialog;

    /**
     * Creates a new {@link DeleteSendingAddressSubmitAction}.
     */
    public DeleteSendingAddressSubmitAction(WorldcoinController worldcoinController, SendWorldcoinPanel sendWorldcoinPanel, DeleteSendingAddressConfirmDialog deleteSendingAddressConfirmDialog) {
        super(worldcoinController, "deleteSendingAddressSubmitAction.text", "deleteSendingAddressSubmitAction.tooltip",
                "deleteSendingAddressSubmitAction.mnemonicKey", ImageLoader.createImageIcon(ImageLoader.DELETE_ADDRESS_ICON_FILE));
        this.sendWorldcoinPanel = sendWorldcoinPanel;
        this.deleteSendingAddressConfirmDialog = deleteSendingAddressConfirmDialog;
    }

    /**
     * Delete the currently selected sending address.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

        WalletData perWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();

        WalletInfoData walletInfo = perWalletModelData.getWalletInfo();
        if (walletInfo == null) {
            walletInfo = new WalletInfoData(perWalletModelData.getWalletFilename(), perWalletModelData.getWallet(), WorldcoinWalletVersion.PROTOBUF_ENCRYPTED);
            perWalletModelData.setWalletInfo(walletInfo);
        }

        if (walletInfo.getSendingAddresses().size() > 0) {
            JTable addressesTable = sendWorldcoinPanel.getAddressesTable();
            AddressBookTableModel addressesTableModel = sendWorldcoinPanel.getAddressesTableModel();
            int viewRow = addressesTable.getSelectedRow();
            if (viewRow >= 0) {
                int selectedAddressRowModel = addressesTable.convertRowIndexToModel(viewRow);
                WalletAddressBookData rowData = addressesTableModel.getAddressBookDataByRow(selectedAddressRowModel, false);
                if (rowData != null) {
                    if (selectedAddressRowModel < addressesTableModel.getRowCount()) {
                        walletInfo.getSendingAddresses().remove(rowData);
                        super.worldcoinController.getModel().getActivePerWalletModelData().setDirty(true);
                        addressesTableModel.fireTableDataChanged();
                    } else {
                        log.error("Could not remove row " + selectedAddressRowModel + " as table model only contained " + addressesTableModel.getRowCount() + " rows");
                    }
                    
                    int newViewRowToSelect = viewRow == 0 ? 0 : viewRow - 1;
                    if (addressesTableModel.getRowCount() > 0) {
                        int newModelRowtoSelect = addressesTable.convertRowIndexToModel(newViewRowToSelect);
                        WalletAddressBookData newRowData = addressesTableModel.getAddressBookDataByRow(newModelRowtoSelect, false);
                    
                        super.worldcoinController.getModel().setActiveWalletPreference(sendWorldcoinPanel.getAddressConstant(),
                                newRowData.getAddress());
                        super.worldcoinController.getModel().setActiveWalletPreference(sendWorldcoinPanel.getLabelConstant(),
                                newRowData.getLabel());

                        if (sendWorldcoinPanel.getAddressTextField() != null) {
                            sendWorldcoinPanel.getAddressTextField().setText(newRowData.getAddress());
                        }
                        sendWorldcoinPanel.getLabelTextArea().setText(newRowData.getLabel());

                        sendWorldcoinPanel.displayQRCode(newRowData.getAddress(), sendWorldcoinPanel.getAmount(), newRowData.getLabel());
                    }
                }
            }     
        }
        
        sendWorldcoinPanel.checkDeleteSendingEnabled();
        
        if (deleteSendingAddressConfirmDialog != null) {
            deleteSendingAddressConfirmDialog.setVisible(false);
        }
    }
}
