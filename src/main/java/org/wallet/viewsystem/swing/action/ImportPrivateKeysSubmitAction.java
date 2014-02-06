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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;

import org.wallet.controller.worldcoin.WorldcoinController;
import org.worldcoinj.wallet.Protos.Wallet.EncryptionType;
import org.wallet.file.BackupManager;
import org.wallet.file.PrivateKeyAndDate;
import org.wallet.file.PrivateKeysHandler;
import org.wallet.file.PrivateKeysHandlerException;
import org.wallet.file.WalletSaveException;
import org.wallet.message.Message;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WalletBusyListener;
import org.wallet.network.ReplayManager;
import org.wallet.network.ReplayTask;
import org.wallet.utils.DateUtils;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.panels.ImportPrivateKeysPanel;
import org.wallet.viewsystem.swing.view.walletlist.SingleWalletPanel;
import org.wallet.viewsystem.swing.view.walletlist.WalletListPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import com.google.worldcoin.core.ECKey;
import com.google.worldcoin.core.Utils;
import com.google.worldcoin.core.Wallet;
import com.google.worldcoin.crypto.KeyCrypter;
import com.google.worldcoin.crypto.KeyCrypterException;
import com.piuk.blockchain.MyWallet;

/**
 * This {@link Action} imports the private keys to the active wallet.
 */
public class ImportPrivateKeysSubmitAction extends WorldcoinWalletSubmitAction implements WalletBusyListener {

    private static final Logger log = LoggerFactory.getLogger(ImportPrivateKeysSubmitAction.class);

    private static final long serialVersionUID = 1923492087598757765L;

    private WorldcoinWalletFrame mainFrame;
    private ImportPrivateKeysPanel importPrivateKeysPanel;
    private JPasswordField walletPasswordField;
    private JPasswordField passwordField;
    private JPasswordField passwordField2;

    private boolean performReplay = true;

    private File privateKeysBackupFile;

    private static final long NUMBER_OF_MILLISECONDS_IN_A_SECOND = 1000;

    /**
     * Creates a new {@link ImportPrivateKeysSubmitAction}.
     */
    public ImportPrivateKeysSubmitAction(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame, ImportPrivateKeysPanel importPrivateKeysPanel,
            ImageIcon icon, JPasswordField walletPasswordField, JPasswordField passwordField1, JPasswordField passwordField2) {
        super(worldcoinController, "importPrivateKeysSubmitAction.text", "importPrivateKeysSubmitAction.tooltip",
                "importPrivateKeysSubmitAction.mnemonicKey", icon);
        this.mainFrame = mainFrame;
        this.importPrivateKeysPanel = importPrivateKeysPanel;
        this.walletPasswordField = walletPasswordField;
        this.passwordField = passwordField1;
        this.passwordField2 = passwordField2;
        
        // This action is a WalletBusyListener.
        super.worldcoinController.registerWalletBusyListener(this);
        walletBusyChange(super.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    /**
     * Import the private keys and replay the blockchain.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        privateKeysBackupFile = null;

        if (abort()) {
            return;
        }

        String importFilename = importPrivateKeysPanel.getOutputFilename();
        if (importFilename == null || importFilename.equals("")) {
            // No import file - nothing to do.
            importPrivateKeysPanel.setMessageText1(controller.getLocaliser().getString(
                    "importPrivateKeysSubmitAction.privateKeysNothingToDo"));
            importPrivateKeysPanel.setMessageText2(" ");
            return;
        }

        // See if a wallet password is required and present.
        if (super.worldcoinController.getModel().getActiveWallet() != null) {
            KeyCrypter keyCrypter = super.worldcoinController.getModel().getActiveWallet().getKeyCrypter();
            if (keyCrypter != null && keyCrypter.getUnderstoodEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
                if (walletPasswordField.getPassword() == null || walletPasswordField.getPassword().length == 0) {
                    importPrivateKeysPanel.setMessageText1(controller.getLocaliser().getString(
                            "showExportPrivateKeysAction.youMustEnterTheWalletPassword"));
                    importPrivateKeysPanel.setMessageText2(" ");
                    return;
                }

                try {
                    // See if the password is the correct wallet password.
                    if (!super.worldcoinController.getModel().getActiveWallet()
                            .checkPassword(CharBuffer.wrap(walletPasswordField.getPassword()))) {
                        // The password supplied is incorrect.
                        importPrivateKeysPanel.setMessageText1(controller.getLocaliser().getString(
                                "createNewReceivingAddressSubmitAction.passwordIsIncorrect"));
                        importPrivateKeysPanel.setMessageText2(" ");
                        return;
                    }
                } catch (KeyCrypterException ede) {
                    log.debug(ede.getClass().getCanonicalName() + " " + ede.getMessage());
                    // The password supplied is probably incorrect.
                    importPrivateKeysPanel.setMessageText1(controller.getLocaliser().getString(
                            "createNewReceivingAddressSubmitAction.passwordIsIncorrect"));
                    importPrivateKeysPanel.setMessageText2(" ");
                    return;
                }
            }
        }

        setEnabled(false);

        log.debug("Importing from file '" + importFilename + "'.");
        File importFile = new File(importFilename);

        CharSequence passwordCharSequence = CharBuffer.wrap(passwordField.getPassword());

        try {
            if (importPrivateKeysPanel.worldcoinWalletFileChooser.accept(importFile)) {
                log.debug("Regular WorldcoinWallet import.");

                PrivateKeysHandler privateKeysHandler = new PrivateKeysHandler(super.worldcoinController.getModel().getNetworkParameters());
                importPrivateKeysPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                Collection<PrivateKeyAndDate> privateKeyAndDateArray = privateKeysHandler.readInPrivateKeys(importFile,
                        passwordCharSequence);

                changeWalletBusyAndImportInBackground(privateKeyAndDateArray,  CharBuffer.wrap(walletPasswordField.getPassword()));
                importPrivateKeysPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } else if (importPrivateKeysPanel.myWalletEncryptedFileChooser.accept(importFile)) {
                log.debug("MyWallet encrypted wallet backup import.");
                String importFileContents = PrivateKeysHandler.readFile(importFile);

                String mainPassword = new String(passwordField.getPassword());
                String secondPassword = new String(passwordField2.getPassword());

                MyWallet wallet = new MyWallet(importFileContents, mainPassword);

                log.debug("Create MyWallet wallet " + wallet);

                boolean needSecondPassword = false;
                if (wallet.isDoubleEncrypted()) {
                    if ("".equals(secondPassword)) {
                        log.debug("Second password missing but is needed");
                        needSecondPassword = true;
                        importPrivateKeysPanel.requestSecondPassword();
                    }
                }

                log.debug("needSecondPassword = " + needSecondPassword);

                if (!needSecondPassword) {
                    wallet.setTemporySecondPassword(secondPassword);

                    Wallet worldcoinj = wallet.getWorldcoinJWallet();
                    log.debug("worldcoinj wallet.1 = " + worldcoinj);
                    
                    Collection<PrivateKeyAndDate> privateKeyAndDateArray = new ArrayList<PrivateKeyAndDate>();
                    if (worldcoinj != null && worldcoinj.getKeychain() != null) {
                        log.debug("Found " + worldcoinj.getKeychainSize() + " keys to import.1");
                        for (ECKey key : worldcoinj.getKeychain()) {
                            privateKeyAndDateArray.add(new PrivateKeyAndDate(key, null));
                        }
                    } else {
                        log.debug("Worldcoinj wallet was null or contained no keychain.1");
                    }
                    changeWalletBusyAndImportInBackground(privateKeyAndDateArray, CharBuffer.wrap(walletPasswordField.getPassword()));
                }

            } else if (importPrivateKeysPanel.myWalletPlainFileChooser.accept(importFile)) {
                log.debug("MyWallet unencrypted wallet backup import.");
                
                String importFileContents = PrivateKeysHandler.readFile(importFile);
                log.debug("Imported file contents length was " + importFileContents.length());
                
                MyWallet wallet = new MyWallet(importFileContents);
                log.debug("MyWallet wallet.2 = " + wallet);

                Wallet worldcoinj = wallet.getWorldcoinJWallet();
                log.debug("worldcoinj wallet.2 = " + worldcoinj);
                
                Collection<PrivateKeyAndDate> privateKeyAndDateArray = new ArrayList<PrivateKeyAndDate>();
                if (worldcoinj != null && worldcoinj.getKeychain() != null) {
                    log.debug("Found " + worldcoinj.getKeychainSize() + " keys to import.2");
                    for (ECKey key : worldcoinj.getKeychain()) {
                        privateKeyAndDateArray.add(new PrivateKeyAndDate(key, null));
                    }
                } else {
                    log.debug("Worldcoinj wallet was null or contained no keychain.2");
                }
                changeWalletBusyAndImportInBackground(privateKeyAndDateArray, CharBuffer.wrap(walletPasswordField.getPassword()));

            } else {
                log.error("The wallet import file was not a recognised type.");
            }
        } catch (Exception e) {
            log.error(e.getClass().getName() + " " + e.getMessage());
            setEnabled(true);

            importPrivateKeysPanel.setMessageText1(controller.getLocaliser().getString(
                    "importPrivateKeysSubmitAction.privateKeysUnlockFailure", new Object[] { e.getMessage() }));
            importPrivateKeysPanel.setMessageText2(" ");
            return;
        } finally {
            importPrivateKeysPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void changeWalletBusyAndImportInBackground(final Collection<PrivateKeyAndDate> privateKeyAndDateArray,
            final CharSequence walletPassword) {
        // Double check wallet is not busy then declare that the active wallet
        // is busy with the task
        WalletData perWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();

        if (!perWalletModelData.isBusy()) {
            perWalletModelData.setBusy(true);
            perWalletModelData.setBusyTaskKey("importPrivateKeysSubmitAction.text");
            perWalletModelData.setBusyTaskVerbKey("importPrivateKeysSubmitAction.verb");

            importPrivateKeysPanel.setMessageText1(controller.getLocaliser().getString(
                    "importPrivateKeysSubmitAction.importingPrivateKeys"));
            importPrivateKeysPanel.setMessageText2(" ");

            super.worldcoinController.fireWalletBusyChange(true);

            importPrivateKeysInBackground(privateKeyAndDateArray, walletPassword);
        }
    }
    
    /**
     * Import the private keys in a background Swing worker thread.
     */
    private void importPrivateKeysInBackground(final Collection<PrivateKeyAndDate> privateKeyAndDateArray,
            final CharSequence walletPassword) {
        final WalletData finalPerWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();
        final ImportPrivateKeysPanel finalImportPanel = importPrivateKeysPanel;
        final WorldcoinController finalWorldcoinController = super.worldcoinController;

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String uiMessage = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                Boolean successMeasure = Boolean.FALSE;
                boolean keyEncryptionRequired = false;
                try {
                    Wallet walletToAddKeysTo = finalPerWalletModelData.getWallet();

                    Collection<byte[]> unencryptedWalletPrivateKeys = new ArrayList<byte[]>();
                    Date earliestTransactionDate = new Date(DateUtils.nowUtc().getMillis());

                    if (walletToAddKeysTo.getEncryptionType() != EncryptionType.UNENCRYPTED) {
                        keyEncryptionRequired = true;
                    }

                    try {
                        if (walletToAddKeysTo != null) {
                            synchronized (walletToAddKeysTo.getKeychain()) {
                                // Work out what the unencrypted private keys are.
                                KeyCrypter walletKeyCrypter = walletToAddKeysTo.getKeyCrypter();
                                KeyParameter aesKey = null;
                                if (keyEncryptionRequired) {
                                    if (walletKeyCrypter == null) {
                                        log.error("Missing KeyCrypter. Could not decrypt private keys.");
                                    }
                                    aesKey = walletKeyCrypter.deriveKey(CharBuffer.wrap(walletPassword));
                                }
                                for (ECKey ecKey : walletToAddKeysTo.getKeychain()) {
                                    if (keyEncryptionRequired) {
                                        if (ecKey.getEncryptedPrivateKey() == null
                                                || ecKey.getEncryptedPrivateKey().getEncryptedBytes() == null
                                                || ecKey.getEncryptedPrivateKey().getEncryptedBytes().length == 0) {

                                            log.error("Missing encrypted private key bytes for key " + ecKey.toString()
                                                    + ", enc.priv = "
                                                    + Utils.bytesToHexString(ecKey.getEncryptedPrivateKey().getEncryptedBytes()));
                                        } else {
                                            byte[] decryptedPrivateKey = ecKey.getKeyCrypter().decrypt(
                                                    ecKey.getEncryptedPrivateKey(), aesKey);
                                            unencryptedWalletPrivateKeys.add(decryptedPrivateKey);
                                        }

                                    } else {
                                        // WorldcoinWallet is not encrypted.
                                        unencryptedWalletPrivateKeys.add(ecKey.getPrivKeyBytes());
                                    }
                                }

                                // Keep track of earliest transaction date go backwards from now.
                                if (privateKeyAndDateArray != null) {
                                    for (PrivateKeyAndDate privateKeyAndDate : privateKeyAndDateArray) {
                                        ECKey keyToAdd = privateKeyAndDate.getKey();
                                        if (keyToAdd != null) {
                                            if (privateKeyAndDate.getDate() != null) {
                                                keyToAdd.setCreationTimeSeconds(privateKeyAndDate.getDate().getTime()
                                                        / NUMBER_OF_MILLISECONDS_IN_A_SECOND);
                                            }

                                            if (!keyChainContainsPrivateKey(unencryptedWalletPrivateKeys, keyToAdd, walletPassword)) {
                                                if (keyEncryptionRequired) {
                                                    ECKey encryptedKey = new ECKey(walletKeyCrypter.encrypt(
                                                            keyToAdd.getPrivKeyBytes(), aesKey), keyToAdd.getPubKey(),
                                                            walletKeyCrypter);
                                                    walletToAddKeysTo.addKey(encryptedKey);
                                                } else {
                                                    walletToAddKeysTo.addKey(keyToAdd);
                                                }

                                                // Update earliest transaction date.
                                                if (privateKeyAndDate.getDate() == null) {
                                                    // Need to go back to the genesis block.
                                                    earliestTransactionDate = null;
                                                } else {
                                                    if (earliestTransactionDate != null) {
                                                        earliestTransactionDate = earliestTransactionDate.before(privateKeyAndDate
                                                                .getDate()) ? earliestTransactionDate : privateKeyAndDate.getDate();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        // Wipe the work collection of private key bytes to remove it from memory.
                        for (byte[] privateKeyBytes : unencryptedWalletPrivateKeys) {
                            if (privateKeyBytes != null) {
                                for (int i = 0; i < privateKeyBytes.length; i++) {
                                    privateKeyBytes[i] = 0;
                                }
                            }
                        }
                    }

                    log.debug(walletToAddKeysTo.toString());

                    finalWorldcoinController.getFileHandler().savePerWalletModelData(finalPerWalletModelData, false);

                    finalWorldcoinController.getModel().createAddressBookReceivingAddresses(finalPerWalletModelData.getWalletFilename());

                    // Import was successful.
                    uiMessage = finalWorldcoinController.getLocaliser().getString("importPrivateKeysSubmitAction.privateKeysImportSuccess");

                    // Backup the private keys.
                    privateKeysBackupFile = finalWorldcoinController.getFileHandler().backupPrivateKeys(CharBuffer.wrap(walletPassword));

                    // Backup the wallet and wallet info.
                    BackupManager.INSTANCE.backupPerWalletModelData(finalWorldcoinController.getFileHandler(), finalPerWalletModelData);
                    
                    // Begin blockchain replay - returns quickly - just kicks it off.
                    log.debug("Starting replay from date = " + earliestTransactionDate);
                    if (performReplay) {
                        List<WalletData> perWalletModelDataList = new ArrayList<WalletData>();
                        perWalletModelDataList.add(finalPerWalletModelData);
                        
                        // Initialise the message shown in the SingleWalletPanel
                        if (mainFrame != null) {
                            WalletListPanel walletListPanel = mainFrame.getWalletsView();
                            if (walletListPanel != null) {
                                SingleWalletPanel singleWalletPanel = walletListPanel.findWalletPanelByFilename(finalPerWalletModelData.getWalletFilename());

                                if (singleWalletPanel != null) {
                                    singleWalletPanel.setSyncMessage(controller.getLocaliser().getString("importPrivateKeysSubmitAction.verb"), Message.NOT_RELEVANT_PERCENTAGE_COMPLETE);
                                }
                            }
                        }

                        ReplayTask replayTask = new ReplayTask(perWalletModelDataList, earliestTransactionDate, ReplayTask.UNKNOWN_START_HEIGHT);
                        ReplayManager.INSTANCE.offerReplayTask(replayTask);
                        successMeasure = Boolean.TRUE;
                    }
                } catch (WalletSaveException wse) {
                    logError(wse);
                } catch (KeyCrypterException kce) {
                    logError(kce);
                } catch (PrivateKeysHandlerException pkhe) {
                    logError(pkhe);
                } catch (Exception e) {
                    logError(e);
                }
                return successMeasure;
            }
            
            private void logError(Exception e) {
                log.error(e.getClass().getName() + " " + e.getMessage());
                e.printStackTrace();
                uiMessage = controller.getLocaliser().getString("importPrivateKeysSubmitAction.privateKeysImportFailure",
                        new Object[] { e.getMessage() });

            }

            @Override
            protected void done() {
                try {
                    Boolean wasSuccessful = get();

                    if (finalImportPanel != null && uiMessage != null) {
                        finalImportPanel.setMessageText1(uiMessage);
                    }
                    
                    if (privateKeysBackupFile != null) {
                        try {
                            finalImportPanel.setMessageText2(controller.getLocaliser().getString(
                                    "changePasswordPanel.keysBackupSuccess",
                                    new Object[] { privateKeysBackupFile.getCanonicalPath() }));
                        } catch (IOException e1) {
                            log.debug(e1.getClass().getCanonicalName() + " " + e1.getMessage());
                        }
                    }

                    if (wasSuccessful) {
                        finalImportPanel.clearPasswords();
                    }
                } catch (Exception e) {
                    // Not really used but caught so that SwingWorker shuts down cleanly.
                    log.error(e.getClass() + " " + e.getMessage());
                } 
            }
        };
        log.debug("Importing private keys in background SwingWorker thread");
        worker.execute();
    }

    /**
     * Determine whether the key is already in the wallet.
     * @throws KeyCrypterException
     */
    private boolean keyChainContainsPrivateKey(Collection<byte[]> unencryptedPrivateKeys, ECKey keyToAdd, CharSequence walletPassword) throws KeyCrypterException {
        if (unencryptedPrivateKeys == null || keyToAdd == null) {
            return false;
        } else {
            byte[] unencryptedKeyToAdd = new byte[0];
            if (keyToAdd.isEncrypted()) {
                unencryptedKeyToAdd = keyToAdd.getKeyCrypter().decrypt(keyToAdd.getEncryptedPrivateKey(), keyToAdd.getKeyCrypter().deriveKey(walletPassword));
            }
            for (byte[] loopEncryptedPrivateKey : unencryptedPrivateKeys) { 
                if (Arrays.equals(unencryptedKeyToAdd, loopEncryptedPrivateKey)) {
                    return true;
                }
            }
            return false;
        }
    }

    // Used in testing.
    public void setPerformReplay(boolean performReplay) {
        this.performReplay = performReplay;
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        // Update the enable status of the action to match the wallet busy status.
        if (super.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // WorldcoinWallet is busy with another operation that may change the private keys - Action is disabled.
            putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("worldcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())}));
            setEnabled(false);           
        } else {
            // Enable unless wallet has been modified by another process.
            if (!super.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("importPrivateKeysSubmitAction.text"));
                setEnabled(true);
            }
        }
    }
}