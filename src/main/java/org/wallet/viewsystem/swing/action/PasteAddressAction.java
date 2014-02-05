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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WalletAddressBookData;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.utils.WhitespaceTrimmer;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinPanel;

/**
 * This {@link Action} represents the swing paste address action
 */
public class PasteAddressAction extends AbstractAction {

    private static final long serialVersionUID = 114352235465057705L;

    private final Controller controller;
    private final WorldcoinController worldcoinController;
    
    private SendWorldcoinPanel sendWorldcoinPanel;

    /**
     * Creates a new {@link PasteAddressAction}.
     */
    public PasteAddressAction(WorldcoinController worldcoinController, SendWorldcoinPanel sendWorldcoinPanel, ImageIcon icon) {
        super("", icon);
        // super(controller.getLocaliser().getString("pasteAddressAction.text"));
        
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        
        this.sendWorldcoinPanel = sendWorldcoinPanel;

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("pasteAddressAction.tooltip"));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("pasteAddressAction.mnemonicKey"));
    }

    /**
     * delegate to generic paste address action
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // check to see if the wallet files have changed
        WalletData perWalletModelData = this.worldcoinController.getModel().getActivePerWalletModelData();
        boolean haveFilesChanged = this.worldcoinController.getFileHandler().haveFilesChanged(perWalletModelData);

        if (haveFilesChanged) {
            // set on the perWalletModelData that files have changed and fire
            // data changed
            perWalletModelData.setFilesHaveBeenChangedByAnotherProcess(true);
            this.worldcoinController.fireFilesHaveBeenChangedByAnotherProcess(perWalletModelData);
        } else {
            TextTransfer textTransfer = new TextTransfer();
            String stringToPaste = textTransfer.getClipboardContents();
            stringToPaste = WhitespaceTrimmer.trim(stringToPaste);

            // TODO parse string - if worldcoin URI then fill out other fields

            String label = sendWorldcoinPanel.getLabelTextArea().getText();
            WalletAddressBookData addressBookData = new WalletAddressBookData(label, stringToPaste);
            sendWorldcoinPanel.setAddressBookDataByRow(addressBookData);

            // put it in the user preferences - will then get loaded when view
            // form loads
            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_ADDRESS, stringToPaste);

            // forward back to the view currently being displayed
            controller.displayView(controller.getCurrentView());
         }
    }
}