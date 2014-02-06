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
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.dataproviders.ShowUriDialogDataProvider;
import org.wallet.viewsystem.swing.view.dialogs.ShowOpenUriDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This {@link Action} represents a cancel action to go back to the parent view for the open uri command
 */
public class ShowOpenUriCancelAction extends AbstractAction {

    private static final long serialVersionUID = 191354561231234705L;

    private Controller controller;
    
    private ShowUriDialogDataProvider dataProvider;
    
    private ShowOpenUriDialog showOpenUriDialog;

    /**
     * Creates a new {@link ShowOpenUriCancelAction}.
     */
    public ShowOpenUriCancelAction(Controller controller, ShowUriDialogDataProvider dataProvider, ShowOpenUriDialog showOpenUriDialog) {
        super(controller.getLocaliser().getString("showOpenUriView.noText"));
        this.controller = controller;
        this.dataProvider = dataProvider;
        this.showOpenUriDialog = showOpenUriDialog;

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("cancelBackToParentAction.tooltip"));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("canceBackToParentAction.mnemonicKey"));
    }

    /**
     * return to the transactions view
     */
    @Override
    public void actionPerformed(ActionEvent e) {
//        Item showDialogItem = dataProvider.getData().getItem(WorldcoinModel.OPEN_URI_SHOW_DIALOG);
             
        if (dataProvider != null) {
            String openUriDialogAsString = (Boolean.valueOf(dataProvider.isShowUriDialog())).toString();
            controller.getModel().setUserPreference(WorldcoinModel.OPEN_URI_SHOW_DIALOG, openUriDialogAsString);
        }
        
        // we do not want to use the uri as the user clicked cancel
        controller.getModel().setUserPreference(WorldcoinModel.OPEN_URI_USE_URI, "false");
        
        showOpenUriDialog.setVisible(false);
        controller.displayView(View.TRANSACTIONS_VIEW);      
    }
}