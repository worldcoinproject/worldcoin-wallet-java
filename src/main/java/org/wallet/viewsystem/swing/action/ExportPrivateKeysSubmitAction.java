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
import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;

import org.worldcoinj.wallet.Protos.Wallet.EncryptionType;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.file.PrivateKeysHandler;
import org.wallet.file.Verification;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WalletBusyListener;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.panels.ExportPrivateKeysPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;

import com.google.worldcoin.core.WorldcoinWalletBlockChain;
import com.google.worldcoin.crypto.KeyCrypterException;

/**
 * This {@link Action} exports the active wallets private keys.
 */
public class ExportPrivateKeysSubmitAction extends WorldcoinWalletSubmitAction implements WalletBusyListener {
    private static final Logger log = LoggerFactory.getLogger(ExportPrivateKeysSubmitAction.class);

    private static final long serialVersionUID = 1923492460598757765L;

    private ExportPrivateKeysPanel exportPrivateKeysPanel;
    private WorldcoinWalletFrame mainFrame;

    private PrivateKeysHandler privateKeysHandler;

    private JPasswordField walletPassword;

    private JPasswordField exportFilePassword;

    private JPasswordField exportFileRepeatPassword;

    /**
     * Creates a new {@link ExportPrivateKeysSubmitAction}.
     */ 
    public ExportPrivateKeysSubmitAction(WorldcoinController worldcoinController, ExportPrivateKeysPanel exportPrivateKeysPanel,
            ImageIcon icon, JPasswordField walletPassword, JPasswordField exportFilePassword, JPasswordField exportFileRepeatPassword, WorldcoinWalletFrame mainFrame) {
        super(worldcoinController, "showExportPrivateKeysAction.text.camel", "showExportPrivateKeysAction.tooltip", "showExportPrivateKeysAction.mnemonicKey", icon);
                this.exportPrivateKeysPanel = exportPrivateKeysPanel;
        this.walletPassword = walletPassword;
        this.exportFilePassword = exportFilePassword;
        this.exportFileRepeatPassword = exportFileRepeatPassword;
        this.mainFrame = mainFrame;
        
        // This action is a WalletBusyListener.
        super.worldcoinController.registerWalletBusyListener(this);
        walletBusyChange(super.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    /**
     * Export the private keys to a file.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

        exportPrivateKeysPanel.clearMessages();

        // See if a wallet password is required and present.
        if (super.worldcoinController.getModel().getActiveWallet() != null
                && super.worldcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
            if (walletPassword.getPassword() == null || walletPassword.getPassword().length == 0) {
                exportPrivateKeysPanel.setMessage1(controller.getLocaliser().getString(
                        "showExportPrivateKeysAction.youMustEnterTheWalletPassword"));
                return;
            }

            try {
                // See if the password is the correct wallet password.
                if (!super.worldcoinController.getModel().getActiveWallet().checkPassword(CharBuffer.wrap(walletPassword.getPassword()))) {
                    // The password supplied is incorrect.
                    exportPrivateKeysPanel.setMessage1(controller.getLocaliser().getString(
                            "createNewReceivingAddressSubmitAction.passwordIsIncorrect"));
                    exportPrivateKeysPanel.setMessage2(" ");
                    return;
                }
            } catch (KeyCrypterException kce) {
                exportPrivateKeysPanel.setMessage1(controller.getLocaliser().getString(
                        "createNewReceivingAddressSubmitAction.passwordIsIncorrect"));
                exportPrivateKeysPanel.setMessage2(" ");
            }
        }

        // Get the required output file.
        String exportPrivateKeysFilename = exportPrivateKeysPanel.getOutputFilename();

        // Check an output file was selected.
        if (exportPrivateKeysFilename == null || "".equals(exportPrivateKeysFilename)) {
            exportPrivateKeysPanel.setMessage1(controller.getLocaliser().getString(
                    "showExportPrivateKeysAction.youMustSelectAnOutputFile"));
            return;
        }

        File exportPrivateKeysFile = new File(exportPrivateKeysFilename);

        privateKeysHandler = new PrivateKeysHandler(super.worldcoinController.getModel().getNetworkParameters());

        boolean performEncryptionOfExportFile = false;

        CharSequence exportPasswordToUse = null;

        if (exportPrivateKeysPanel.requiresEncryption()) {
            // Get the passwords on the export file password fields.
            if (exportFilePassword.getPassword() == null || exportFilePassword.getPassword().length == 0) {
                // Notify must enter a password.
                exportPrivateKeysPanel.setMessage1(controller.getLocaliser()
                        .getString("showExportPrivateKeysAction.enterPasswords"));
                return;
            } else {
                if (!Arrays.areEqual(exportFilePassword.getPassword(), exportFileRepeatPassword.getPassword())) {
                    // Notify user passwords are different.
                    exportPrivateKeysPanel.setMessage1(controller.getLocaliser().getString(
                            "showExportPrivateKeysAction.passwordsAreDifferent"));
                    return;
                } else {
                    // Perform encryption.
                    performEncryptionOfExportFile = true;
                    exportPasswordToUse = CharBuffer.wrap(exportFilePassword.getPassword());
                }
            }
        }

        // Check on file overwrite.
        if (exportPrivateKeysFile.exists()) {
            String yesText = controller.getLocaliser().getString("showOpenUriView.yesText");
            String noText = controller.getLocaliser().getString("showOpenUriView.noText");
            String questionText = controller.getLocaliser().getString("showExportPrivateKeysAction.thisFileExistsOverwrite",
                    new Object[] { exportPrivateKeysFile.getName() });
            String questionTitle = controller.getLocaliser().getString("showExportPrivateKeysAction.thisFileExistsOverwriteTitle");
            int selection = JOptionPane.showOptionDialog(mainFrame, questionText, questionTitle, JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, ImageLoader.createImageIcon(ImageLoader.QUESTION_MARK_ICON_FILE), new String[] {
                            yesText, noText }, noText);
            if (selection != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Double check wallet is not busy then declare that the active wallet
        // is busy with the task
        WalletData perWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();

        if (!perWalletModelData.isBusy()) {
            perWalletModelData.setBusy(true);
            perWalletModelData.setBusyTaskKey("showExportPrivateKeysAction.text.camel");

            exportPrivateKeysPanel.setMessage1(controller.getLocaliser().getString(
                    "exportPrivateKeysSubmitAction.exportingPrivateKeys"));
            exportPrivateKeysPanel.setMessage2("");
            
            super.worldcoinController.fireWalletBusyChange(true);

            CharSequence walletPasswordToUse = null;
            if (walletPassword.getPassword() != null) {
                walletPasswordToUse = CharBuffer.wrap(walletPassword.getPassword());
            }
            exportPrivateKeysInBackground(exportPrivateKeysFile, performEncryptionOfExportFile, exportPasswordToUse,
                    walletPasswordToUse);
        }
    }

    /**
     * Export the private keys in a background Swing worker thread.
     */
    private void exportPrivateKeysInBackground(final File exportPrivateKeysFile, final boolean performEncryptionOfExportFile,
            final CharSequence exportPasswordToUse, final CharSequence walletPassword) {
        final WalletData finalPerWalletModelData = super.worldcoinController.getModel().getActivePerWalletModelData();
        final ExportPrivateKeysPanel finalExportPanel = exportPrivateKeysPanel;

        final WorldcoinController finalWorldcoinController = super.worldcoinController;

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String uiMessage1 = null;
            private String uiMessage2 = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                Boolean successMeasure = Boolean.FALSE;

                WorldcoinWalletBlockChain blockChain = null;
                if (finalWorldcoinController.getWorldcoinWalletService() != null) {
                    blockChain = finalWorldcoinController.getWorldcoinWalletService().getChain();
                }

                try {
                    privateKeysHandler.exportPrivateKeys(exportPrivateKeysFile, finalWorldcoinController.getModel().getActivePerWalletModelData()
                            .getWallet(), blockChain, performEncryptionOfExportFile, exportPasswordToUse, walletPassword);

                    // Success.
                    uiMessage1 = controller.getLocaliser().getString("showExportPrivateKeysAction.privateKeysExportSuccess");

                    // Perform a verification on the exported file to see if it
                    // is correct.
                    Verification verification = privateKeysHandler.verifyExportFile(exportPrivateKeysFile, finalWorldcoinController.getModel()
                            .getActivePerWalletModelData().getWallet(), blockChain, performEncryptionOfExportFile,
                            exportPasswordToUse, walletPassword);
                    uiMessage2 = controller.getLocaliser().getString(verification.getMessageKey(), verification.getMessageData());
                    successMeasure = true;
                } catch (IOException ioe) {
                    logError(ioe);
                }

                return successMeasure;
            }

            private void logError(Exception e) {
                log.error(e.getClass().getName() + " " + e.getMessage());
                e.printStackTrace();
                uiMessage1 = controller.getLocaliser().getString("importPrivateKeysSubmitAction.privateKeysImportFailure",
                        new Object[] { e.getMessage() });
                uiMessage2 = "";

            }

            @Override
            protected void done() {
                try {
                    Boolean wasSuccessful = get();

                    if (finalExportPanel != null && uiMessage1 != null) {
                        finalExportPanel.setMessage1(uiMessage1);
                    }

                    if (finalExportPanel != null && uiMessage2 != null) {
                        finalExportPanel.setMessage2(uiMessage2);
                    }
                    
                    // Clear the passwords if the export was successful and the user is still
                    // looking at the same wallet as at start.
                    if (wasSuccessful && finalPerWalletModelData.getWalletFilename().equals(finalWorldcoinController.getModel().getActiveWalletFilename())) {
                        finalExportPanel.clearPasswords();
                    }
                } catch (Exception e) {
                    // Not really used but caught so that SwingWorker shuts down cleanly.
                    log.error(e.getClass() + " " + e.getMessage());
                } finally {
                    // Declare that wallet is no longer busy with the task.
                    finalPerWalletModelData.setBusyTaskKey(null);
                    finalPerWalletModelData.setBusy(false);
                    finalWorldcoinController.fireWalletBusyChange(false);
                }
            }
        };
        log.debug("Exporting private keys in background SwingWorker thread");
        worker.execute();
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
                putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("exportPrivateKeysSubmitAction.text"));
                setEnabled(true);
            }
        }
    }
}