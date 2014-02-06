/**
 * Copyright 2013 wallet.org
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

import com.google.worldcoin.core.*;
import com.google.worldcoin.crypto.KeyCrypterException;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WalletBusyListener;
import org.wallet.utils.WhitespaceTrimmer;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.panels.SignMessagePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.CharBuffer;
import java.util.Iterator;

/**
 * This {@link Action} signs a message
 */
public class SignMessageSubmitAction extends WorldcoinWalletSubmitAction implements WalletBusyListener {

    private static final Logger log = LoggerFactory.getLogger(ImportPrivateKeysSubmitAction.class);

    private static final long serialVersionUID = 1923333087598757765L;

    private WorldcoinWalletFrame mainFrame;
    private SignMessagePanel signMessagePanel;
    
    /**
     * Creates a new {@link SignMessageSubmitAction}.
     */
    public SignMessageSubmitAction(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame,
            SignMessagePanel signMessagePanel, ImageIcon icon) {
        super(worldcoinController, "signMessageAction.text", "signMessageAction.tooltip", "signMessageAction.mnemonicKey", icon);
        this.mainFrame = mainFrame;
        this.signMessagePanel = signMessagePanel;

        // This action is a WalletBusyListener.
        super.worldcoinController.registerWalletBusyListener(this);
        walletBusyChange(super.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    /**
     * Verify the message.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (abort()) {
            return;
        }
        
        if (signMessagePanel == null) {
            return;
        }
        
        String addressText = null;
        if (signMessagePanel.getAddressTextArea() != null) {
            addressText = signMessagePanel.getAddressTextArea().getText();
            if (addressText != null) {
              addressText = WhitespaceTrimmer.trim(addressText);
            }
        }
        
        String messageText = null;
        if (signMessagePanel.getMessageTextArea() != null) {
            messageText = signMessagePanel.getMessageTextArea().getText();
        }
        

        
        CharSequence walletPassword = null;
        if (signMessagePanel.getWalletPasswordField() != null) {
            walletPassword = CharBuffer.wrap(signMessagePanel.getWalletPasswordField().getPassword());

            if (worldcoinController.getModel().getActiveWallet().isEncrypted()) {
                if (walletPassword.length() == 0) {
                    signMessagePanel.setMessageText1(controller.getLocaliser().getString(
                            "showExportPrivateKeysAction.youMustEnterTheWalletPassword"));
                    signMessagePanel.setMessageText2(" ");
                    return;
                }

                if (!worldcoinController.getModel().getActiveWallet().checkPassword(walletPassword)) {
                    // The password supplied is incorrect.
                    signMessagePanel.setMessageText1(controller.getLocaliser().getString(
                            "createNewReceivingAddressSubmitAction.passwordIsIncorrect"));
                    signMessagePanel.setMessageText2(" ");
                    return;
                }
            }
        }      
        
        log.debug("addressText = '" + addressText + "'");
        log.debug("messageText = '" + messageText + "'");
        
        if (addressText == null || "".equals(addressText)) {
            signMessagePanel.setMessageText1(controller.getLocaliser().getString("signMessageAction.noAddress"));
            signMessagePanel.setMessageText2(" ");  
            return;
        }
        
        if (messageText == null || "".equals(messageText.trim())) {
            signMessagePanel.setMessageText1(controller.getLocaliser().getString("signMessageAction.noMessage"));
            signMessagePanel.setMessageText2(" ");  
            return;
        }

        try {
            Address signingAddress = new Address(worldcoinController.getModel().getNetworkParameters(), addressText);
            
            // Find the ECKey corresponding to the signing address.
            Wallet activeWallet = worldcoinController.getModel().getActiveWallet();
            Iterable<ECKey> keychain = activeWallet.getKeys();
            Iterator<ECKey> iterator = keychain.iterator();
            ECKey signingKey = null;
            while (iterator.hasNext()) {
                ECKey ecKey = iterator.next();
                if (ecKey.toAddress(worldcoinController.getModel().getNetworkParameters()).equals(signingAddress)) {
                    signingKey = ecKey;
                    break;
                }
            }
            if (signingKey == null) {
                // No signing key found.
                signMessagePanel.setMessageText1(controller.getLocaliser().getString("signMessageAction.noSigningKey", new String[]{addressText}));
                signMessagePanel.setMessageText2(" "); 
            } else {
                KeyParameter aesKey = null;
                if (signingKey.isEncrypted()) {
                    aesKey = signingKey.getKeyCrypter().deriveKey(walletPassword);
                    signingKey = signingKey.decrypt(signingKey.getKeyCrypter(), aesKey);
                }

                String signatureBase64 = signingKey.signMessage(messageText, aesKey);
                if (signMessagePanel.getSignatureTextArea() != null) {
                    signMessagePanel.getSignatureTextArea().setText(signatureBase64);
                    signMessagePanel.setMessageText1(controller.getLocaliser().getString("signMessageAction.success"));
                    signMessagePanel.setMessageText2(" "); 

                }        
            }
        } catch (KeyCrypterException e) {
            logError(e);
        } catch (WrongNetworkException e) {
            logError(e);
        } catch (AddressFormatException e) {
            logError(e);
        } 
    }
    
    private void logError(Exception e) {
        e.printStackTrace();
        signMessagePanel.setMessageText1(controller.getLocaliser().getString("signMessageAction.error"));
        signMessagePanel.setMessageText2(controller.getLocaliser().getString("deleteWalletConfirmDialog.walletDeleteError2", 
                new String[] {e.getClass().getCanonicalName() + " " + e.getMessage()}));
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        // Update the enable status of the action to match the wallet busy
        // status.
        if (super.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // WorldcoinWallet is busy with another operation that may change the private
            // keys - Action is disabled.
            putValue(
                    SHORT_DESCRIPTION,
                    controller.getLocaliser().getString(
                            "worldcoinWalletSubmitAction.walletIsBusy",
                            new Object[] { controller.getLocaliser().getString(
                                    this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey()) }));
            setEnabled(false);
        } else {
            // Enable unless wallet has been modified by another process.
            if (!super.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("signMessageAction.tooltip"));
                setEnabled(true);
            }
        }
    }
}