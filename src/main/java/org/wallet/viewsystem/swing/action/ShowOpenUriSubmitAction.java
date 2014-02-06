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

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.dataproviders.ShowUriDialogDataProvider;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.dialogs.ShowOpenUriDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This {@link Action} processes the open uri command
 */
public class ShowOpenUriSubmitAction extends AbstractAction {

    private static final long serialVersionUID = 1913592460523457765L;

    private final Controller controller;
    private final WorldcoinController worldcoinController;
    
    private ShowUriDialogDataProvider dataProvider;
    private ShowOpenUriDialog showOpenUriDialog;

    /**
     * Creates a new {@link ShowOpenUriSubmitAction}.
     */
    public ShowOpenUriSubmitAction(WorldcoinWalletFrame mainFrame, WorldcoinController worldcoinController, ShowUriDialogDataProvider dataProvider,
            ShowOpenUriDialog showOpenUriDialog) {
        super(worldcoinController.getLocaliser().getString("showOpenUriView.yesText"));
        
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        
        this.dataProvider = dataProvider;
        this.showOpenUriDialog = showOpenUriDialog;

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());

        putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("showOpenUriViewAction.tooltip"));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("showOpenUriViewAction.mnemonicKey"));
    }

    /**
     * show the showOpenUriDialog
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        // check to see if the wallet files have changed
        WalletData perWalletModelData = this.worldcoinController.getModel().getActivePerWalletModelData();
        boolean haveFilesChanged = this.worldcoinController.getFileHandler().haveFilesChanged(perWalletModelData);

        if (haveFilesChanged) {
            // set on the perWalletModelData that files have changed and fire
            // data changed
            perWalletModelData.setFilesHaveBeenChangedByAnotherProcess(true);
            this.worldcoinController.fireFilesHaveBeenChangedByAnotherProcess(perWalletModelData);
        } else {
            // get the data out of the temporary data and put it in the wallet
            // preferences

            String sendAddress = dataProvider.getAddress();
            String sendLabel = dataProvider.getLabel();
            String sendAmount = dataProvider.getAmount();
            boolean showDialog = dataProvider.isShowUriDialog();

            if (sendAddress != null) {
                this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_ADDRESS, sendAddress);
            }
            if (sendLabel != null) {
                this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_LABEL, sendLabel);
            }
            if (sendAmount != null) {
                this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_AMOUNT, sendAmount);
            }
            
            // we want the send view to paste in the send data
            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_PERFORM_PASTE_NOW, "true");

            // we want to set the user preference to use the uri as the user
            // clicked yes
            controller.getModel().setUserPreference(WorldcoinModel.OPEN_URI_USE_URI, "true");

            // save as user preference whether to show dialog or not
            controller.getModel().setUserPreference(WorldcoinModel.OPEN_URI_SHOW_DIALOG, (Boolean.valueOf(showDialog)).toString());

            showOpenUriDialog.setVisible(false);
            controller.displayView(View.SEND_WORLDCOIN_VIEW);
        }
    }
}