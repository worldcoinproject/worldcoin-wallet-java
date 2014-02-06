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

import com.google.worldcoin.core.AddressFormatException;
import com.google.worldcoin.core.Transaction;
import com.google.worldcoin.core.Wallet.SendRequest;
import com.google.worldcoin.crypto.KeyCrypterException;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.worldcoinj.wallet.Protos.Wallet.EncryptionType;
import org.wallet.controller.Controller;
import org.wallet.file.WalletSaveException;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.worldcoin.*;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinConfirmPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.CharBuffer;

/**
 * This {@link Action} actually spends worldcoin.
 */
public class SendWorldcoinNowAction extends AbstractAction implements WalletBusyListener {

  public Logger log = LoggerFactory.getLogger(SendWorldcoinNowAction.class.getName());

  private static final long serialVersionUID = 1913592460523457765L;

  private final Controller controller;
  private final WorldcoinController worldcoinController;

  private SendWorldcoinConfirmPanel sendWorldcoinConfirmPanel;
  private JPasswordField walletPasswordField;

  private final static int MAX_LENGTH_OF_ERROR_MESSAGE = 120;

  /**
   * Boolean to indicate that the test parameters should be used for "sending".
   */
  private boolean useTestParameters = false;

  /**
   * Boolean to indicate that the "send was successful" or not (when useTestParameters = true).
   */
  private boolean sayTestSendWasSuccessful = false;

  private Transaction transaction;

  private SendRequest sendRequest;


  /**
   * Creates a new {@link SendWorldcoinNowAction}.
   */
  public SendWorldcoinNowAction(WorldcoinWalletFrame mainFrame, WorldcoinController worldcoinController,
                                SendWorldcoinConfirmPanel sendWorldcoinConfirmPanel, JPasswordField walletPasswordField, ImageIcon icon, SendRequest sendRequest) {
    super(worldcoinController.getLocaliser().getString("sendWorldcoinConfirmAction.text"), icon);

    this.worldcoinController = worldcoinController;
    this.controller = this.worldcoinController;

    this.sendWorldcoinConfirmPanel = sendWorldcoinConfirmPanel;
    this.walletPasswordField = walletPasswordField;
    this.sendRequest = sendRequest;

    MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());

    putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("sendWorldcoinConfirmAction.tooltip"));
    putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("sendWorldcoinConfirmAction.mnemonicKey"));

    // This action is a WalletBusyListener.
    this.worldcoinController.registerWalletBusyListener(this);
    walletBusyChange(this.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
  }

  /**
   * Actually send the worldcoin.
   */
  @Override
  public void actionPerformed(ActionEvent event) {
    sendWorldcoinConfirmPanel.setMessageText(" ", " ");

    // Check to see if the wallet files have changed.
    WalletData perWalletModelData = this.worldcoinController.getModel().getActivePerWalletModelData();
    boolean haveFilesChanged = this.worldcoinController.getFileHandler().haveFilesChanged(perWalletModelData);

    if (haveFilesChanged) {
      // Set on the perWalletModelData that files have changed and fire data changed.
      perWalletModelData.setFilesHaveBeenChangedByAnotherProcess(true);
      this.worldcoinController.fireFilesHaveBeenChangedByAnotherProcess(perWalletModelData);
    } else {
      // Put sending message and remove the send button.
      sendWorldcoinConfirmPanel.setMessageText(controller.getLocaliser().getString("sendWorldcoinNowAction.sendingWorldcoin"), "");

      // Get the label and address out of the wallet preferences.
      String sendAddress = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_ADDRESS);
      String sendLabel = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_LABEL);

      if (sendLabel != null && !sendLabel.equals("")) {
        WalletInfoData addressBook = perWalletModelData.getWalletInfo();
        addressBook.addSendingAddress(new WalletAddressBookData(sendLabel, sendAddress));
      }

      char[] walletPassword = walletPasswordField.getPassword();

      if (this.worldcoinController.getModel().getActiveWallet() != null
              && this.worldcoinController.getModel().getActiveWallet().getEncryptionType() != EncryptionType.UNENCRYPTED) {
        // Encrypted wallet.
        if (walletPassword == null || walletPassword.length == 0) {
          // User needs to enter password.
          sendWorldcoinConfirmPanel.setMessageText(
                  controller.getLocaliser().getString("showExportPrivateKeysAction.youMustEnterTheWalletPassword"), "");
          return;
        }

        try {
          if (!this.worldcoinController.getModel().getActiveWallet().checkPassword(CharBuffer.wrap(walletPassword))) {
            // The password supplied is incorrect.
            sendWorldcoinConfirmPanel.setMessageText(
                    controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.passwordIsIncorrect"),
                    "");
            return;
          }
        } catch (KeyCrypterException kce) {
          log.debug(kce.getClass().getCanonicalName() + " " + kce.getMessage());
          // The password supplied is probably incorrect.
          sendWorldcoinConfirmPanel.setMessageText(
                  controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.passwordIsIncorrect"), "");
          return;
        }
      }

      // Double check wallet is not busy then declare that the active wallet is busy with the task
      if (!perWalletModelData.isBusy()) {
        perWalletModelData.setBusy(true);
        perWalletModelData.setBusyTaskVerbKey("sendWorldcoinNowAction.sendingWorldcoin");

        this.worldcoinController.fireWalletBusyChange(true);
        sendWorldcoinConfirmPanel.setMessageText(controller.getLocaliser().getString("sendWorldcoinNowAction.sendingWorldcoin"), "");
        sendWorldcoinConfirmPanel.invalidate();
        sendWorldcoinConfirmPanel.validate();
        sendWorldcoinConfirmPanel.repaint();

        performSend(perWalletModelData, sendRequest, CharBuffer.wrap(walletPassword));
      }
    }
  }

  /**
   * Send the transaction directly.
   */
  private void performSend(WalletData perWalletModelData, SendRequest sendRequest, CharSequence walletPassword) {
    String message = null;

    boolean sendWasSuccessful = Boolean.FALSE;
    try {
      if (sendRequest != null && sendRequest.tx != null) {
        log.debug("Sending from wallet " + perWalletModelData.getWalletFilename() + ", tx = " + sendRequest.tx.toString());
      }

      if (useTestParameters) {
        log.debug("Using test parameters - not really sending");
        if (sayTestSendWasSuccessful) {
          sendWasSuccessful = Boolean.TRUE;
          log.debug("Using test parameters - saying send was successful");
        } else {
          message = "test - send failed";
          log.debug("Using test parameters - saying send failed");
        }
      } else {
        transaction = this.worldcoinController.getWorldcoinWalletService().sendCoins(perWalletModelData, sendRequest, walletPassword);
        if (transaction == null) {
          // a null transaction returned indicates there was not
          // enough money (in spite of our validation)
          message = controller.getLocaliser().getString("sendWorldcoinNowAction.thereWereInsufficientFundsForTheSend");
          log.error(message);
        } else {
          sendWasSuccessful = Boolean.TRUE;
          log.debug("Sent transaction was:\n" + transaction.toString());
        }
      }
    } catch (KeyCrypterException e) {
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } catch (WalletSaveException e) {
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } catch (AddressFormatException e) {
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } catch (IllegalStateException e) {
      log.error(e.getMessage(), e);
      message = controller.getLocaliser().getString("sendWorldcoinNowAction.pingFailure");
    } catch (Exception e) {
      // Really trying to catch anything that goes wrong with the send worldcoin.
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } finally {
      // Save the wallet.
      try {
        this.worldcoinController.getFileHandler().savePerWalletModelData(perWalletModelData, false);
      } catch (WalletSaveException e) {
        log.error(e.getMessage(), e);
        message = e.getMessage();
      }

      if (sendWasSuccessful) {
        String successMessage = controller.getLocaliser().getString("sendWorldcoinNowAction.worldcoinSentOk");
        if (sendWorldcoinConfirmPanel != null && (sendWorldcoinConfirmPanel.isVisible() || useTestParameters)) {
          sendWorldcoinConfirmPanel.setMessageText(
                  controller.getLocaliser().getString("sendWorldcoinNowAction.worldcoinSentOk"));
          sendWorldcoinConfirmPanel.showOkButton();
          sendWorldcoinConfirmPanel.clearAfterSend();
        } else {
          MessageManager.INSTANCE.addMessage(new Message(successMessage));
        }
      } else {
        log.error(message);

        if (message != null && message.length() > MAX_LENGTH_OF_ERROR_MESSAGE) {
          message = message.substring(0, MAX_LENGTH_OF_ERROR_MESSAGE) + "...";
        }

        String errorMessage = controller.getLocaliser().getString("sendWorldcoinNowAction.worldcoinSendFailed");
        if (sendWorldcoinConfirmPanel != null && (sendWorldcoinConfirmPanel.isVisible() || useTestParameters)) {
          sendWorldcoinConfirmPanel.setMessageText(errorMessage, message);
        } else {
          MessageManager.INSTANCE.addMessage(new Message(errorMessage + " " + message));
        }
      }

      // Declare that wallet is no longer busy with the task.
      perWalletModelData.setBusyTaskKey(null);
      perWalletModelData.setBusy(false);
      this.worldcoinController.fireWalletBusyChange(false);

      log.debug("firing fireRecreateAllViews...");
      controller.fireRecreateAllViews(false);
      log.debug("firing fireRecreateAllViews...done");
    }
  }

  public Transaction getTransaction() {
    return transaction;
  }

  void setTestParameters(boolean useTestParameters, boolean sayTestSendWasSuccessful) {
    this.useTestParameters = useTestParameters;
    this.sayTestSendWasSuccessful = sayTestSendWasSuccessful;
  }

  @Override
  public void walletBusyChange(boolean newWalletIsBusy) {
    // Update the enable status of the action to match the wallet busy status.
    if (this.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
      // WorldcoinWallet is busy with another operation that may change the private keys - Action is disabled.
      putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("worldcoinWalletSubmitAction.walletIsBusy",
              new Object[]{controller.getLocaliser().getString(this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())}));
      setEnabled(false);
    } else {
      // Enable unless wallet has been modified by another process.
      if (!this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
        putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("sendWorldcoinConfirmAction.tooltip"));
        setEnabled(true);
      }
    }
  }
}