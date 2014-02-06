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

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WalletBusyListener;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.swing.view.panels.HelpContentsPanel;

/**
 * This {@link Action} represents a localised WorldcoinWallet action that just displays a view
 * and is a WalletChangeListener
 */
public class WorldcoinWalletBusyAction extends AbstractAction implements WalletBusyListener {

    private static final long serialVersionUID = 191948235465057705L;

    private final Controller controller;
    private final WorldcoinController worldcoinController;
    
    private View viewToDisplay;
    private String tooltipKey;

    /**
     * Creates a new {@link WorldcoinWalletAction}.
     * @param controller The Controller
     * @param imagePath The relative path to the image to load for the item. Usually an imageLoader constant
     * @param textKey The localisation key for the text of the action
     * @param tooltipKey The localisation key for the tooltip of the action
     * @param mnemonicKey The localisation key for the mnemonic of the action
     * @param viewToDisplay The view to display on action activation.  One of the View constants
     */
    public WorldcoinWalletBusyAction(WorldcoinController worldcoinController, String imagePath, String textKey, String tooltipKey, String mnemonicKey, View viewToDisplay) {
        super(textKey == null ? "" : worldcoinController.getLocaliser().getString(textKey), ImageLoader.createImageIcon(imagePath));
        
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        
        this.viewToDisplay = viewToDisplay;
        this.tooltipKey = tooltipKey;

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString(tooltipKey)));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic(mnemonicKey));
        
        this.worldcoinController.registerWalletBusyListener(this);
    }

    /**
     * Display the view specified.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        controller.displayView(viewToDisplay);
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        // Update the tooltip to match the wallet busy status.
        // Enable/ disable is done by WorldcoinWalletFrame.
        if (newWalletIsBusy) {
            // WorldcoinWallet is busy with another operation that may change the private keys - Action is disabled.
            putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("worldcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())}));
        } else {
            // Enable unless wallet has been modified by another process.
            if (!this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString(tooltipKey));
            }
        }
    }
}