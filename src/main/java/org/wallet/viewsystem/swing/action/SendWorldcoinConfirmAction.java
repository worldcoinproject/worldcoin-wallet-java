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

import com.google.worldcoin.core.*;
import com.google.worldcoin.core.Wallet.SendRequest;
import com.google.worldcoin.crypto.KeyCrypterException;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.dataproviders.WorldcoinFormDataProvider;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.dialogs.SendWorldcoinConfirmDialog;
import org.wallet.viewsystem.swing.view.dialogs.ValidationErrorDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.math.BigInteger;

/**
 * This {@link Action} shows the send worldcoin confirm dialog or validation dialog on an attempted spend.
 */
public class SendWorldcoinConfirmAction extends WorldcoinWalletSubmitAction {

    private static final long serialVersionUID = 1913592460523457765L;

    private static final Logger log = LoggerFactory.getLogger(SendWorldcoinConfirmAction.class);

    private WorldcoinWalletFrame mainFrame;
    private WorldcoinFormDataProvider dataProvider;
    private WorldcoinController worldcoinController;

    /**
     * Creates a new {@link SendWorldcoinConfirmAction}.
     */
    public SendWorldcoinConfirmAction(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame, WorldcoinFormDataProvider dataProvider) {
        super(worldcoinController, "sendWorldcoinConfirmAction.text", "sendWorldcoinConfirmAction.tooltip", "sendWorldcoinConfirmAction.mnemonicKey", ImageLoader.createImageIcon(ImageLoader.SEND_WORLDCOIN_ICON_FILE));
        this.mainFrame = mainFrame;
        this.dataProvider = dataProvider;
        this.worldcoinController = worldcoinController;
    }

    /**
     * Complete the transaction to work out the fee) and then show the send worldcoin confirm dialog.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

        SendWorldcoinConfirmDialog sendWorldcoinConfirmDialog = null;
        ValidationErrorDialog validationErrorDialog = null;

        try {
            String sendAddress = dataProvider.getAddress();
            String sendAmount = dataProvider.getAmount();

            Validator validator = new Validator(super.worldcoinController);
            if (validator.validate(sendAddress, sendAmount)) {
                // The address and amount are valid.

                // Create a SendRequest.
                Address sendAddressObject;

                sendAddressObject = new Address(worldcoinController.getModel().getNetworkParameters(), sendAddress);
                SendRequest sendRequest = Wallet.SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount));

                sendRequest.ensureMinRequiredFee = true;
                sendRequest.fee = BigInteger.ZERO;
                sendRequest.feePerKb = WorldcoinModel.SEND_FEE_PER_KB_DEFAULT;

                // Note - Request is populated with the AES key in the SendWorldcoinNowAction after the user has entered it on the SendWorldcoinConfirm form.

                // Complete it (which works out the fee) but do not sign it yet.
                log.debug("Just about to complete the tx (and calculate the fee)...");
                boolean completedOk = worldcoinController.getModel().getActiveWallet().completeTx(sendRequest, false);
                log.debug("The fee after completing the transaction was " + sendRequest.fee);
                if (completedOk) {
                    // There is enough money.

                  //  worldcoinController.getModel().getActiveWallet().g.getp.broadcastTransaction(sendRequest.tx);
                    sendWorldcoinConfirmDialog = new SendWorldcoinConfirmDialog(super.worldcoinController, mainFrame, sendRequest);
                    sendWorldcoinConfirmDialog.setVisible(true);
                } else {
                    // There is not enough money.
                    // TODO setup validation parameters accordingly so that it displays ok.
                    validationErrorDialog = new ValidationErrorDialog(super.worldcoinController, mainFrame, sendRequest, true);
                    validationErrorDialog.setVisible(true);
                }

            } else {
                validationErrorDialog = new ValidationErrorDialog(super.worldcoinController, mainFrame, null, false);
                validationErrorDialog.setVisible(true);
            }
        } catch (WrongNetworkException e1) {
            logMessage(e1);
        } catch (AddressFormatException e1) {
            logMessage(e1);
        } catch (KeyCrypterException e1) {
            logMessage(e1);
        } catch (Exception e1) {
            logMessage(e1);
        }
    }

    private void logMessage(Exception e) {
        e.printStackTrace();
        String errorMessage = controller.getLocaliser().getString("sendWorldcoinNowAction.worldcoinSendFailed");
        String detailMessage = controller.getLocaliser().getString("deleteWalletConfirmDialog.walletDeleteError2", new String[]{e.getClass().getCanonicalName() + " " + e.getMessage()});
        MessageManager.INSTANCE.addMessage(new Message(errorMessage + " " + detailMessage));
    }
}