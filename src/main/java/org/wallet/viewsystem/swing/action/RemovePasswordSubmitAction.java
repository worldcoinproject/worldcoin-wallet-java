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
import java.nio.CharBuffer;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.file.BackupManager;
import org.wallet.file.FileHandler;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WalletBusyListener;
import org.wallet.model.worldcoin.WalletInfoData;
import org.wallet.store.WorldcoinWalletVersion;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.panels.RemovePasswordPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.worldcoin.core.Wallet;
import com.google.worldcoin.crypto.KeyCrypterException;

/**
 * This {@link Action} action removes the encryption of private keys in a wallet.
 */
public class RemovePasswordSubmitAction extends WorldcoinWalletSubmitAction implements WalletBusyListener {
    private static final Logger log = LoggerFactory.getLogger(RemovePasswordSubmitAction.class);

    private static final long serialVersionUID = 1923492460598757765L;

    private RemovePasswordPanel removePasswordPanel;
    private JPasswordField password1;

    /**
     * Creates a new {@link RemovePasswordSubmitAction}.
     */
    public RemovePasswordSubmitAction(WorldcoinController worldcoinController, RemovePasswordPanel removePasswordPanel,
            ImageIcon icon, JPasswordField password1, WorldcoinWalletFrame mainFrame) {
        super(worldcoinController, "removePasswordSubmitAction.text", "removePasswordSubmitAction.tooltip", "removePasswordSubmitAction.mnemonicKey", icon);
        this.removePasswordPanel = removePasswordPanel;
        this.password1 = password1;
        
        // This action is a WalletBusyListener.
        super.worldcoinController.registerWalletBusyListener(this);
        walletBusyChange(super.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    /**
     * Remove the password protection on a wallet.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        removePasswordPanel.clearMessages();

        char[] passwordToUse = password1.getPassword();

        // Get the passwords on the password fields.
        if (password1.getPassword() == null || password1.getPassword().length == 0) {
            // Notify that the user must enter a password.
            removePasswordPanel.setMessage1(controller.getLocaliser()
                    .getString("removePasswordPanel.enterPassword"));
            return;
        }
       
        if (super.worldcoinController.getModel().getActiveWallet() != null) {
            Wallet wallet = super.worldcoinController.getModel().getActiveWallet();
            if (wallet != null) {
                    WalletData perWalletModelData = null;
                    WalletInfoData walletInfoData = null;
                    
                    try {
                        // Double check wallet is not busy then declare that the active
                        // wallet is busy with the task
                        perWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();
                        walletInfoData = super.worldcoinController.getModel().getActiveWalletInfo();

                        if (!perWalletModelData.isBusy()) {
                            perWalletModelData.setBusy(true);
                            perWalletModelData.setBusyTaskKey("removePasswordSubmitAction.text");

                            super.worldcoinController.fireWalletBusyChange(true);

                            wallet.decrypt(wallet.getKeyCrypter().deriveKey(CharBuffer.wrap(passwordToUse)));
                            walletInfoData.setWalletVersion(WorldcoinWalletVersion.PROTOBUF);
                            perWalletModelData.setDirty(true);
                            FileHandler fileHandler = new FileHandler(super.worldcoinController);
                            fileHandler.savePerWalletModelData(perWalletModelData, true);
                            
                            // Backup the wallet and wallet info.
                            BackupManager.INSTANCE.backupPerWalletModelData(fileHandler, perWalletModelData);
                        }
                    } catch (KeyCrypterException kce) {
                        removePasswordPanel.setMessage1(controller.getLocaliser()
                                .getString("removePasswordPanel.removePasswordFailed", new String[]{kce.getMessage()}));
                        return;
                    } finally {
                        // Declare that wallet is no longer busy with the task.
                        if (perWalletModelData != null) {
                            perWalletModelData.setBusyTaskKey(null);
                            perWalletModelData.setBusy(false);
                        }
                        super.worldcoinController.fireWalletBusyChange(false);
                    }
            }
        }
        controller.fireDataChangedUpdateNow();

        // Success.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                removePasswordPanel.clearMessages();
                removePasswordPanel.clearPasswords();
                removePasswordPanel.setMessage1(controller.getLocaliser()
                        .getString("removePasswordPanel.removePasswordSuccess")); 
            }});
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        // Update the enable status of the action to match the wallet busy status.
        if (super.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // WorldcoinWallet is busy with another operation that may change the private keys - Action is disabled.
            putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("worldcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())}));
        } else {
            // Enable unless wallet has been modified by another process.
            if (!super.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("removePasswordSubmitAction.text"));
            }
        }
    }
}