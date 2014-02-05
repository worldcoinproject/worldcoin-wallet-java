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

import com.google.worldcoin.core.ECKey;
import com.google.worldcoin.crypto.KeyCrypter;
import com.google.worldcoin.crypto.KeyCrypterException;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.worldcoinj.wallet.Protos.Wallet.EncryptionType;
import org.wallet.file.BackupManager;
import org.wallet.file.FileHandler;
import org.wallet.file.WalletSaveException;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.worldcoin.*;
import org.wallet.store.WorldcoinWalletVersion;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.swing.view.dialogs.CreateNewReceivingAddressDialog;
import org.wallet.viewsystem.swing.view.panels.CreateNewReceivingAddressPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This {@link Action} represents an action to actually create receiving
 * addresses.
 */
public class CreateNewReceivingAddressSubmitAction extends WorldcoinWalletSubmitAction implements WalletBusyListener {
    private static Logger log = LoggerFactory.getLogger(CreateNewReceivingAddressSubmitAction.class);

    private static final long serialVersionUID = 200152235465875405L;

    private CreateNewReceivingAddressDialog createNewReceivingAddressDialog;
    private CreateNewReceivingAddressPanel createNewReceivingAddressPanel;

    private JPasswordField walletPassword;
    
    /**
     * The last private keys backup file used - used in testing.
     */
    private File lastPrivateKeysBackupFile;
    
    /**
     * Creates a new {@link CreateNewReceivingAddressSubmitAction}.
     */
    public CreateNewReceivingAddressSubmitAction(WorldcoinController worldcoinController,
            CreateNewReceivingAddressDialog createNewReceivingAddressDialog,
            CreateNewReceivingAddressPanel createNewReceivingAddressPanel, JPasswordField walletPassword) {
        super(worldcoinController, "createNewReceivingAddressSubmitAction.text", "createNewReceivingAddressSubmitAction.tooltip",
                "createNewReceivingAddressSubmitAction.mnemonicKey", ImageLoader.createImageIcon(ImageLoader.ADD_ICON_FILE));
        this.createNewReceivingAddressDialog = createNewReceivingAddressDialog;
        this.createNewReceivingAddressPanel = createNewReceivingAddressPanel;
        this.walletPassword = walletPassword;
        this.lastPrivateKeysBackupFile = null;
        
        // This action is a WalletBusyListener
        super.worldcoinController.registerWalletBusyListener(this);
        walletBusyChange(super.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    /**
     * Create new receiving addresses.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

        WalletData perWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();
        boolean encryptNewKeys = false;
        
        if (super.worldcoinController.getModel().getActiveWallet() != null
                && super.worldcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
            if (walletPassword.getPassword() == null || walletPassword.getPassword().length == 0) {
                // User needs to enter password.
                createNewReceivingAddressPanel.setMessageText(controller.getLocaliser().getString(
                        "showExportPrivateKeysAction.youMustEnterTheWalletPassword"));
                return;
            }
            encryptNewKeys = true;

            try {
                if (!super.worldcoinController.getModel().getActiveWallet().checkPassword(CharBuffer.wrap(walletPassword.getPassword()))) {
                    // The password supplied is incorrect.
                    createNewReceivingAddressPanel.setMessageText(controller.getLocaliser().getString(
                            "createNewReceivingAddressSubmitAction.passwordIsIncorrect"));
                    return;
                }
            } catch (KeyCrypterException ede) {
                log.debug(ede.getClass().getCanonicalName() + " " + ede.getMessage());
                // The password supplied is probably incorrect.
                createNewReceivingAddressPanel.setMessageText(controller.getLocaliser().getString(
                        "createNewReceivingAddressSubmitAction.passwordIsIncorrect"));
                return;
            }
        }

        WalletInfoData walletInfo = perWalletModelData.getWalletInfo();
        if (walletInfo == null) {
            walletInfo = new WalletInfoData(perWalletModelData.getWalletFilename(), perWalletModelData.getWallet(), WorldcoinWalletVersion.PROTOBUF_ENCRYPTED);
            perWalletModelData.setWalletInfo(walletInfo);
        }
        
        // Double check wallet is not busy then declare that the active wallet is busy with the addReceivingAddresses task
        if (!perWalletModelData.isBusy()) {
            perWalletModelData.setBusy(true);
            perWalletModelData.setBusyTaskKey("createNewReceivingAddressSubmitAction.tooltip");

            // Can no longer cancel as the task has started.
            createNewReceivingAddressPanel.getCancelButton().setEnabled(false);

            int numberOfAddressesToCreate = createNewReceivingAddressPanel.getNumberOfAddressesToCreate();
            
            String walletDescription =  super.worldcoinController.getModel().getActiveWalletInfo().getProperty(WalletInfoData.DESCRIPTION_PROPERTY);
            String shortMessage = controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.creatingShort", new Object[] {numberOfAddressesToCreate});
            String longMessage = controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.creatingLong", new Object[] {numberOfAddressesToCreate, walletDescription});
            createNewReceivingAddressPanel.setMessageText(shortMessage);
            MessageManager.INSTANCE.addMessage(new Message(" "));
            Message logMessage = new Message(longMessage);
            logMessage.setShowInStatusBar(false);
            MessageManager.INSTANCE.addMessage(logMessage);

            super.worldcoinController.fireWalletBusyChange(true);

            createNewReceivingAddressesInBackground(createNewReceivingAddressPanel.getNumberOfAddressesToCreate(), encryptNewKeys, 
                CharBuffer.wrap(walletPassword.getPassword()), this);
        }
    }
    
    /**
     * Create the new receiving addresses in a background Swing worker thread.
     */
    private void createNewReceivingAddressesInBackground(final int numberOfAddressesToCreate, final boolean encryptNewKeys, 
            final CharSequence walletPassword, final CreateNewReceivingAddressSubmitAction thisAction) {
        final WalletData finalPerWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();

        final WorldcoinController finalController = super.worldcoinController;
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String shortMessage = null;
            private String longMessage = null;
            private String lastAddressString = null;
            private File privateKeysBackupFile = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                Boolean successMeasure = Boolean.FALSE;
                
                privateKeysBackupFile = null;

                final KeyCrypter walletKeyCrypter = finalPerWalletModelData.getWallet().getKeyCrypter();
                    try {
                        // Derive AES key to use outside of loop - it is the same for all keys in a single wallet.
                        KeyParameter aesKey = null;
                        if (encryptNewKeys) {
                            aesKey = walletKeyCrypter.deriveKey(walletPassword);
                        }
                        List<ECKey> newKeys = new ArrayList<ECKey>();
                        for (int i = 0; i < numberOfAddressesToCreate; i++) {
                            ECKey newKey;
                            if (encryptNewKeys) {
                                // Use the wallet KeyCrypter.
                                newKey = (new ECKey()).encrypt(walletKeyCrypter, aesKey);
                            } else {
                                newKey = new ECKey();
                            }
                            newKeys.add(newKey);
                        }
                        
                        FileHandler fileHandler = finalController.getFileHandler();
                        
                        synchronized (finalPerWalletModelData.getWallet()) {
                            finalPerWalletModelData.getWallet().addKeys(newKeys);
                        }
                        
                        // Add keys to address book.
                        for (ECKey newKey : newKeys) {
                            lastAddressString = newKey.toAddress(finalController.getModel().getNetworkParameters()).toString();
                            finalPerWalletModelData.getWalletInfo().addReceivingAddress(new WalletAddressBookData("", lastAddressString),
                                false);
                        }
                        
                        // Backup the private keys.
                        privateKeysBackupFile = fileHandler.backupPrivateKeys(CharBuffer.wrap(walletPassword));
                        thisAction.setLastPrivateKeysBackupFile(privateKeysBackupFile);

                        // Backup the wallet and wallet info.
                        BackupManager.INSTANCE.backupPerWalletModelData(fileHandler, finalPerWalletModelData);

                        successMeasure = Boolean.TRUE;
                    } catch (KeyCrypterException kce) {
                        logError(kce);
                    } catch (IOException io) {
                        logError(io);
                    } catch (Exception e) {
                        logError(e);
                    }
                
                return successMeasure;
            }
            
            private void logError(Exception e) {
                log.error(e.getClass().getName() + " " + e.getMessage());
                e.printStackTrace();
                shortMessage = controller.getLocaliser().getString("createNewReceivingAddressesSubmitAction.failure",
                        new Object[] { e.getMessage() });
                longMessage = shortMessage;
            }

            @Override
            protected void done() {
                try {
                    Boolean wasSuccessful = get();
 
                    String walletDescription =  "";
                    if (finalPerWalletModelData != null && finalPerWalletModelData.getWalletInfo() != null) {
                        walletDescription = finalPerWalletModelData.getWalletInfo().getProperty(WalletInfoData.DESCRIPTION_PROPERTY);
                    }
                    
                    if (wasSuccessful) {
                        shortMessage = controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.createdSuccessfullyShort", new Object[] {numberOfAddressesToCreate});
                        longMessage = controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.createdSuccessfullyLong", new Object[] {numberOfAddressesToCreate, walletDescription});
                        if (privateKeysBackupFile != null) {
                            longMessage = longMessage + ".\n" + controller.getLocaliser().getString("changePasswordPanel.keysBackupSuccess", new Object[] { privateKeysBackupFile.getCanonicalPath() });
                        }
                        
                        log.debug(longMessage);
                        
                        if (createNewReceivingAddressPanel.getReceiveWorldcoinPanel() != null) {
                            createNewReceivingAddressPanel.getReceiveWorldcoinPanel().getAddressesTableModel().fireTableDataChanged();
                            createNewReceivingAddressPanel.getReceiveWorldcoinPanel().selectRows();
                        }
                        
                        finalPerWalletModelData.getWalletInfo().put(WorldcoinModel.RECEIVE_ADDRESS, lastAddressString);
                        finalPerWalletModelData.getWalletInfo().put(WorldcoinModel.RECEIVE_LABEL, "");
                        
                        try {
                            finalController.getFileHandler().savePerWalletModelData(finalPerWalletModelData, false);
                        } catch (WalletSaveException wse) {
                            log.error(wse.getClass().getCanonicalName() + " " + wse.getMessage());
                            MessageManager.INSTANCE.addMessage(new Message(controller.getLocaliser().getString("createNewReceivingAddressesSubmitAction.failure",
                                    new Object[] { wse.getClass().getCanonicalName() + " " + wse.getMessage() })));
                        }   
                    } else {
                        log.error(longMessage);
                    }
                    
                    if (shortMessage != null) {
                        createNewReceivingAddressPanel.setMessageText(shortMessage);           
                         if (createNewReceivingAddressDialog != null && createNewReceivingAddressDialog.isVisible()) {
                             // Show short message in dialog, long in messages.
                            createNewReceivingAddressPanel.setMessageText(shortMessage);
                            Message logMessage = new Message(longMessage);
                            logMessage.setShowInStatusBar(false);
                            MessageManager.INSTANCE.addMessage(logMessage);
                       } else {
                            // Show long message on statusbar and in messages.
                            MessageManager.INSTANCE.addMessage(new Message(longMessage));
                        }
                    }
                 } catch (Exception e) {
                    // Not really used but caught so that SwingWorker shuts down cleanly.
                    log.error(e.getClass() + " " + e.getMessage());
                } finally {
                    // Can now cancel the operation.
                    createNewReceivingAddressPanel.getCancelButton().setEnabled(true);

                    // Declare that wallet is no longer busy with the task.
                    finalPerWalletModelData.setBusyTaskKey(null);
                    finalPerWalletModelData.setBusy(false);
                    finalController.fireWalletBusyChange(false);                   
                }
            }
        };
        log.debug("Creating receive addresses in background SwingWorker thread");
        worker.execute();
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        // Update the enable status of the action to match the wallet busy status.
        if (super.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // WorldcoinWallet is busy with another operation that may change the private keys - Action is disabled.
            putValue(SHORT_DESCRIPTION, this.worldcoinController.getLocaliser().getString("worldcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())}));
            setEnabled(false);           
        } else {
            // Enable unless wallet has been modified by another process.
            if (!super.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                putValue(SHORT_DESCRIPTION, this.worldcoinController.getLocaliser().getString("createNewReceivingAddressSubmitAction.tooltip"));
                setEnabled(true);
            }
            
            // Make sure the cancel button is enabled.
            createNewReceivingAddressPanel.getCancelButton().setEnabled(true);
        }
    }

    public File getLastPrivateKeysBackupFile() {
        return lastPrivateKeysBackupFile;
    }

    public void setLastPrivateKeysBackupFile(File lastPrivateKeysBackupFile) {
        this.lastPrivateKeysBackupFile = lastPrivateKeysBackupFile;
    }
}