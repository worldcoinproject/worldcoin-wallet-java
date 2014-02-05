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

import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.model.worldcoin.WalletAddressBookData;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WalletInfoData;
import org.wallet.store.WorldcoinWalletVersion;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This {@link Action} represents an action to create a sending address.
 */
public class CreateNewSendingAddressAction extends WorldcoinWalletSubmitAction {

    private static final long serialVersionUID = 200111935465875405L;

    private SendWorldcoinPanel sendWorldcoinPanel;

    /**
     * Creates a new {@link CreateNewSendingAddressAction}.
     */
    public CreateNewSendingAddressAction(WorldcoinController worldcoinController, SendWorldcoinPanel sendWorldcoinPanel) {
        super(worldcoinController, "createOrEditAddressAction.createReceiving.text", "createOrEditAddressAction.createSending.tooltip",
                "createOrEditAddressAction.createSending.mnemonicKey", null);
        this.sendWorldcoinPanel = sendWorldcoinPanel;
    }

    /**
     * Create new send address.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

        // Check to see if the wallet files have changed.
        WalletData perWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();

        WalletInfoData walletInfo = perWalletModelData.getWalletInfo();
        if (walletInfo == null) {
            walletInfo = new WalletInfoData(perWalletModelData.getWalletFilename(), perWalletModelData.getWallet(), WorldcoinWalletVersion.PROTOBUF_ENCRYPTED);
            perWalletModelData.setWalletInfo(walletInfo);
        }

        if (walletInfo.getSendingAddresses().isEmpty()) {
            String address = super.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_ADDRESS);
            String label = super.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_LABEL);

            perWalletModelData.getWalletInfo().addSendingAddress(new WalletAddressBookData(label, address));
            sendWorldcoinPanel.getAddressesTableModel().fireTableDataChanged();
            super.worldcoinController.getModel().getActivePerWalletModelData().setDirty(true);
        } else {
            perWalletModelData.getWalletInfo().addSendingAddress(new WalletAddressBookData("", ""));
            sendWorldcoinPanel.getAddressesTableModel().fireTableDataChanged();
            sendWorldcoinPanel.selectRows();

            super.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_ADDRESS, "");
            super.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_LABEL, "");
        }
        
        sendWorldcoinPanel.checkDeleteSendingEnabled();
        
        controller.displayView(controller.getCurrentView());

        if (sendWorldcoinPanel.getLabelTextArea() != null) {
            sendWorldcoinPanel.getLabelTextArea().requestFocusInWindow();
        }
    }
}