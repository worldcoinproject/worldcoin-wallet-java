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
package org.wallet.viewsystem.swing.view.panels;

import com.google.worldcoin.core.Sha256Hash;
import com.google.worldcoin.core.Transaction;
import com.google.worldcoin.core.Utils;
import com.google.worldcoin.core.Wallet.SendRequest;
import org.wallet.WorldcoinWallet;
import org.worldcoinj.wallet.Protos.Wallet.EncryptionType;
import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.model.worldcoin.WalletBusyListener;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.swing.ColorAndFontConstants;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.action.CancelBackToParentAction;
import org.wallet.viewsystem.swing.action.OkBackToParentAction;
import org.wallet.viewsystem.swing.action.SendWorldcoinNowAction;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletButton;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletDialog;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletLabel;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletTitledPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * The send worldcoin confirm panel.
 */
public class SendWorldcoinConfirmPanel extends JPanel implements WalletBusyListener {
    private static final long serialVersionUID = 191435612399957705L;

    private static final Logger log = LoggerFactory.getLogger(SendWorldcoinConfirmPanel.class);

    private static final int STENT_WIDTH = 10;

    private WorldcoinWalletFrame mainFrame;
    private WorldcoinWalletDialog sendWorldcoinConfirmDialog;

    private final Controller controller;
    private final WorldcoinController worldcoinController;

    private WorldcoinWalletLabel sendAddressText;
    private WorldcoinWalletLabel sendLabelText;
    private WorldcoinWalletLabel sendAmountText;
    private WorldcoinWalletLabel sendFeeText;

    private String sendAddress;
    private String sendLabel;
    private SendRequest sendRequest;

    private WorldcoinWalletLabel confirmText1;
    private WorldcoinWalletLabel confirmText2;

    private SendWorldcoinNowAction sendWorldcoinNowAction;
    private WorldcoinWalletButton sendButton;
    private WorldcoinWalletButton cancelButton;

    private JPasswordField walletPasswordField;
    private WorldcoinWalletLabel walletPasswordPromptLabel;
    private WorldcoinWalletLabel explainLabel;

    private static SendWorldcoinConfirmPanel thisPanel = null;

    private static ImageIcon shapeTriangleIcon;
    private static ImageIcon shapeSquareIcon;
    private static ImageIcon shapeHeptagonIcon;
    private static ImageIcon shapeHexagonIcon;
    private static ImageIcon progress0Icon;

    static {
        shapeTriangleIcon = ImageLoader.createImageIcon(ImageLoader.SHAPE_TRIANGLE_ICON_FILE);
        shapeSquareIcon = ImageLoader.createImageIcon(ImageLoader.SHAPE_SQUARE_ICON_FILE);
        shapeHeptagonIcon = ImageLoader.createImageIcon(ImageLoader.SHAPE_PENTAGON_ICON_FILE);
        shapeHexagonIcon = ImageLoader.createImageIcon(ImageLoader.SHAPE_HEXAGON_ICON_FILE);
        progress0Icon = ImageLoader.createImageIcon(ShowTransactionsPanel.PROGRESS_0_ICON_FILE);
    }

    /**
     * Creates a new {@link SendWorldcoinConfirmPanel}.
     */
    public SendWorldcoinConfirmPanel(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame, WorldcoinWalletDialog sendWorldcoinConfirmDialog, SendRequest sendRequest) {
        super();
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        this.mainFrame = mainFrame;
        this.sendWorldcoinConfirmDialog = sendWorldcoinConfirmDialog;
        this.sendRequest = sendRequest;

        thisPanel = this;

        initUI();

        cancelButton.requestFocusInWindow();
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        this.worldcoinController.registerWalletBusyListener(this);
    }

    /**
     * Initialise worldcoin confirm panel.
     */
    public void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        mainPanel.setLayout(new GridBagLayout());

        String[] keys = new String[] { "sendWorldcoinPanel.addressLabel",
                "sendWorldcoinPanel.labelLabel", "sendWorldcoinPanel.amountLabel",
                "showPreferencesPanel.feeLabel.text", "showExportPrivateKeysPanel.walletPasswordPrompt"};

        int stentWidth = WorldcoinWalletTitledPanel.calculateStentWidthForKeys(controller.getLocaliser(), keys, mainPanel)
                + ExportPrivateKeysPanel.STENT_DELTA;

        // Get the data out of the wallet preferences.
        sendAddress = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_ADDRESS);
        sendLabel = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_LABEL);
        String sendAmount = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_AMOUNT) + " " + controller.getLocaliser(). getString("sendWorldcoinPanel.amountUnitLabel");

        String sendAmountLocalised = CurrencyConverter.INSTANCE.prettyPrint(sendAmount);

        String fee = "0";
        if (sendRequest != null) {
            fee = Utils.worldcoinValueToPlainString(sendRequest.fee);
        }

        String sendFeeLocalised = CurrencyConverter.INSTANCE.prettyPrint(fee);

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(STENT_WIDTH), constraints);

        ImageIcon bigIcon = ImageLoader.createImageIcon(ImageLoader.WORLDCOIN_WALLET_128_ICON_FILE);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.weightx = 0.5;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 5;
        constraints.anchor = GridBagConstraints.CENTER;
        JLabel bigIconLabel = new JLabel(bigIcon);
        mainPanel.add(bigIconLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(STENT_WIDTH, STENT_WIDTH), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 7;
        constraints.gridy = 1;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(STENT_WIDTH), constraints);

        explainLabel = new WorldcoinWalletLabel("");
        explainLabel.setText(controller.getLocaliser().getString("sendWorldcoinConfirmView.message"));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.weightx = 0.8;
        constraints.weighty = 0.4;
        constraints.gridwidth = 5;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(explainLabel, constraints);
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(explainLabel.getPreferredSize().width, explainLabel.getPreferredSize().height), constraints);

        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 2;
        constraints.weightx = 0.6;
        constraints.weighty = 0.8;
        constraints.gridwidth = 3;
        constraints.gridheight = 5;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(detailPanel, constraints);

        GridBagConstraints constraints2 = new GridBagConstraints();

        constraints2.fill = GridBagConstraints.HORIZONTAL;
        constraints2.gridx = 0;
        constraints2.gridy = 0;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(WorldcoinWalletTitledPanel.createStent(stentWidth), constraints2);

        constraints2.fill = GridBagConstraints.HORIZONTAL;
        constraints2.gridx = 1;
        constraints2.gridy = 0;
        constraints2.weightx = 0.05;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.CENTER;
        detailPanel.add(WorldcoinWalletTitledPanel.createStent(WorldcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints2);

        JLabel forcer1 = new JLabel();
        forcer1.setOpaque(false);
        constraints2.fill = GridBagConstraints.HORIZONTAL;
        constraints2.gridx = 2;
        constraints2.gridy = 0;
        constraints2.weightx = 10;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(forcer1, constraints2);

        WorldcoinWalletLabel sendAddressLabel = new WorldcoinWalletLabel("");
        sendAddressLabel.setText(controller.getLocaliser().getString("sendWorldcoinPanel.addressLabel"));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 0;
        constraints2.gridy = 1;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(sendAddressLabel, constraints2);

        sendAddressText = new WorldcoinWalletLabel("");
        sendAddressText.setText(sendAddress);
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 1;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(sendAddressText, constraints2);

        WorldcoinWalletLabel sendLabelLabel = new WorldcoinWalletLabel("");
        sendLabelLabel.setText(controller.getLocaliser().getString("sendWorldcoinPanel.labelLabel"));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 0;
        constraints2.gridy = 2;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(sendLabelLabel, constraints2);

        sendLabelText = new WorldcoinWalletLabel("");
        sendLabelText.setText(sendLabel);
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 2;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(sendLabelText, constraints2);

        WorldcoinWalletLabel sendAmountLabel = new WorldcoinWalletLabel("");
        sendAmountLabel.setText(controller.getLocaliser().getString("sendWorldcoinPanel.amountLabel"));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 0;
        constraints2.gridy = 3;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(sendAmountLabel, constraints2);

        sendAmountText = new WorldcoinWalletLabel("");
        sendAmountText.setText(sendAmountLocalised);
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 3;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(sendAmountText, constraints2);

        WorldcoinWalletLabel sendFeeLabel = new WorldcoinWalletLabel("");
        sendFeeLabel.setText(controller.getLocaliser().getString("showPreferencesPanel.feeLabel.text"));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 0;
        constraints2.gridy = 4;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(sendFeeLabel, constraints2);

        sendFeeText = new WorldcoinWalletLabel("");
        sendFeeText.setText(sendFeeLocalised);
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 4;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(sendFeeText, constraints2);

        constraints2.fill = GridBagConstraints.HORIZONTAL;
        constraints2.gridx = 0;
        constraints2.gridy = 5;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(WorldcoinWalletTitledPanel.createStent(stentWidth), constraints2);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 7;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(stentWidth), constraints);

        // Add wallet password field.
        walletPasswordPromptLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("showExportPrivateKeysPanel.walletPasswordPrompt"));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(walletPasswordPromptLabel, constraints);
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(walletPasswordPromptLabel.getPreferredSize().width, walletPasswordPromptLabel.getPreferredSize().height), constraints);


        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 4;
        constraints.gridy = 7;
        constraints.weightx = 0.05;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(WorldcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        JLabel forcer2 = new JLabel();
        forcer2.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 5;
        constraints.gridy = 7;
        constraints.weightx = 10;
        constraints.weighty = 0.05;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(forcer2, constraints);

        JPanel filler4 = new JPanel();
        filler4.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 7;
        constraints.weightx = 0.3;
        constraints.weighty = 0.01;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler4, constraints);

        walletPasswordField = new JPasswordField(24);
        walletPasswordField.setMinimumSize(new Dimension(200, 20));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 5;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(walletPasswordField, constraints);
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(200, 20), constraints);

        JPanel filler5 = new JPanel();
        filler4.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.01;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler5, constraints);

        if (this.worldcoinController.getModel().getActiveWallet() != null) {
            if (this.worldcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
                // Need wallet password.
                walletPasswordField.setEnabled(true);
                walletPasswordPromptLabel.setEnabled(true);
            } else {
                // No wallet password required.
                walletPasswordField.setEnabled(false);
                walletPasswordPromptLabel.setEnabled(false);
            }
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        //buttonPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 10;
        constraints.weightx = 0.8;
        constraints.weighty = 0.1;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(buttonPanel, constraints);

        CancelBackToParentAction cancelAction = new CancelBackToParentAction(controller, ImageLoader.createImageIcon(ImageLoader.CROSS_ICON_FILE), sendWorldcoinConfirmDialog);
        cancelButton = new WorldcoinWalletButton(cancelAction, controller);
        buttonPanel.add(cancelButton);

        sendWorldcoinNowAction = new SendWorldcoinNowAction(mainFrame, this.worldcoinController, this, walletPasswordField, ImageLoader.createImageIcon(ImageLoader.SEND_WORLDCOIN_ICON_FILE), sendRequest);
        sendButton = new WorldcoinWalletButton(sendWorldcoinNowAction, controller);
        buttonPanel.add(sendButton);

        confirmText1 = new WorldcoinWalletLabel("");
        confirmText1.setText(" ");
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 11;
        constraints.weightx = 0.8;
        constraints.weighty = 0.15;
        constraints.gridwidth = 6;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(confirmText1, constraints);

        JLabel filler3 = new JLabel();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 7;
        constraints.gridy = 11;
        constraints.weightx = 0.05;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler3, constraints);

        confirmText2 = new WorldcoinWalletLabel(" ");
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 12;
        constraints.weightx = 0.8;
        constraints.weighty = 0.15;
        constraints.gridwidth = 6;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(confirmText2, constraints);

        JLabel filler6 = new JLabel();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 7;
        constraints.gridy = 12;
        constraints.weightx = 0.05;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler6, constraints);

        enableSendAccordingToNumberOfConnectedPeersAndWalletBusy();
    }

    private void enableSendAccordingToNumberOfConnectedPeersAndWalletBusy() {
        boolean enableSend = false;
        String message = " ";

        if (this.controller.getModel() != null) {
            String singleNodeConnection = this.controller.getModel().getUserPreference(WorldcoinModel.SINGLE_NODE_CONNECTION);
            boolean singleNodeConnectionOverride = singleNodeConnection != null && singleNodeConnection.trim().length() > 0;

            String peers = this.controller.getModel().getUserPreference(WorldcoinModel.PEERS);
            boolean singlePeerOverride = peers != null && peers.split(",").length == 1;

            if (thisPanel.sendWorldcoinNowAction != null) {
                if (!singleNodeConnectionOverride && !singlePeerOverride && this.worldcoinController.getModel().getNumberOfConnectedPeers() < WorldcoinModel.MINIMUM_NUMBER_OF_CONNECTED_PEERS_BEFORE_SEND_IS_ENABLED) {
                     // Disable send button
                    enableSend = false;
                    message = controller.getLocaliser().getString("sendWorldcoinConfirmView.worldcoin-walletMustBeOnline");
                } else {
                    // Enable send button
                    enableSend = true;
                    message = " ";
                }
                if (this.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
                    enableSend = false;
                    message = controller.getLocaliser().getString("worldcoinWalletSubmitAction.walletIsBusy",
                            new Object[]{controller.getLocaliser().getString(this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())});
                }
                thisPanel.sendWorldcoinNowAction.setEnabled(enableSend);
            }
        }

        if (sendWorldcoinNowAction != null) {
            sendWorldcoinNowAction.setEnabled(enableSend);
            if (confirmText1 != null) {
                if (enableSend) {
                    // Only clear the 'worldcoin-walletMustBeOnline' message.
                    if (controller.getLocaliser().getString("sendWorldcoinConfirmView.worldcoin-walletMustBeOnline").equals(confirmText1.getText())) {
                        confirmText1.setText(message);
                    }
                } else {
                    confirmText1.setText(message);
                }
            }
        }
    }

    public void setMessageText(final String message1) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                confirmText1.setText(message1);
            }});
        invalidate();
        validate();
        repaint();
    }

    public void setMessageText(final String message1, final String message2) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                confirmText1.setText(message1);
                confirmText2.setText(" " + message2);
            }});
        invalidate();
        validate();
        repaint();
    }

    public void clearAfterSend() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                walletPasswordField.setText("");
                walletPasswordField.setVisible(false);
                explainLabel.setVisible(false);
                walletPasswordPromptLabel.setVisible(false);
            }});
    }

    public void showOkButton() {
        OkBackToParentAction okAction = new OkBackToParentAction(controller, sendWorldcoinConfirmDialog);
        sendButton.setAction(okAction);

        cancelButton.setVisible(false);
    }

    public static void updatePanel() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (thisPanel != null && thisPanel.isVisible()) {
                    final WorldcoinController worldcoinController = WorldcoinWallet.getWorldcoinController();
                    if (worldcoinController != null) {
                        String singleNodeConnection = worldcoinController.getModel().getUserPreference(WorldcoinModel.SINGLE_NODE_CONNECTION);
                        boolean singleNodeConnectionOverride = singleNodeConnection != null && singleNodeConnection.trim().length() > 0;

                        String peers = worldcoinController.getModel().getUserPreference(WorldcoinModel.PEERS);
                        boolean singlePeerOverride = peers != null && peers.split(",").length == 1;

                        boolean enableSend = false;
                        if (thisPanel.sendWorldcoinNowAction != null) {
                            if (!singleNodeConnectionOverride && !singlePeerOverride && worldcoinController.getModel().getNumberOfConnectedPeers() < WorldcoinModel.MINIMUM_NUMBER_OF_CONNECTED_PEERS_BEFORE_SEND_IS_ENABLED) {
                                // Disable send button
                                enableSend = false;
                            } else {
                                // Enable send button
                                enableSend = true;
                            }
                            if (worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
                                enableSend = false;
                            }
                            thisPanel.sendWorldcoinNowAction.setEnabled(enableSend);
                        }

                        WorldcoinWalletLabel confirmText1 = thisPanel.confirmText1;
                        if (enableSend) {
                            if (confirmText1 != null) {
                                if (WorldcoinWallet.getController().getLocaliser()
                                        .getString("sendWorldcoinConfirmView.worldcoin-walletMustBeOnline").equals(confirmText1.getText())) {
                                    confirmText1.setText(" ");
                                }
                            }
                        } else {
                            if (confirmText1 != null) {
                                confirmText1.setText(WorldcoinWallet.getController().getLocaliser()
                                        .getString("sendWorldcoinConfirmView.worldcoin-walletMustBeOnline"));
                            }
                        }
                    }

                    thisPanel.invalidate();
                    thisPanel.validate();
                    thisPanel.repaint();
                }
            }
        });
    }

    public static void updatePanelDueToTransactionConfidenceChange(final Sha256Hash transactionWithChangedConfidenceHash,
            final int numberOfPeersSeenBy) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (thisPanel == null || !thisPanel.isVisible() || thisPanel.getSendWorldcoinNowAction() == null) {
                    return;
                }
                Transaction sentTransaction = thisPanel.getSendWorldcoinNowAction().getTransaction();

                if (sentTransaction == null || !sentTransaction.getHash().equals(transactionWithChangedConfidenceHash)) {
                    return;
                }

                WorldcoinWalletLabel confirmText2 = thisPanel.getConfirmText2();
                if (confirmText2 != null) {
                    confirmText2.setText(thisPanel.getConfidenceToolTip(numberOfPeersSeenBy));
                    confirmText2.setIcon(thisPanel.getConfidenceIcon(numberOfPeersSeenBy));
                }

                thisPanel.invalidate();
                thisPanel.validate();
                thisPanel.repaint();
            }
        });
    }
    private String getConfidenceToolTip(int numberOfPeers) {
        StringBuilder builder = new StringBuilder("");
        if (numberOfPeers == 0) {
            builder.append(WorldcoinWallet.getController().getLocaliser().getString("transactionConfidence.seenByUnknownNumberOfPeers"));
        } else {
            builder
                .append(WorldcoinWallet.getController().getLocaliser().getString("transactionConfidence.seenBy"))
                .append(" ");
            builder.append(numberOfPeers);
            if (numberOfPeers > 1)
                builder
                    .append(" ")
                    .append(WorldcoinWallet.getController().getLocaliser().getString("transactionConfidence.peers"))
                    .append(".");
            else
                builder.append(" ")
                    .append(WorldcoinWallet.getController().getLocaliser().getString("transactionConfidence.peer"))
                    .append(".");
        }
        return builder.toString();
    }

    private ImageIcon getConfidenceIcon(int numberOfPeers) {
        // By default return a triangle which indicates the least known.
        ImageIcon iconToReturn;

        if (numberOfPeers >= 4) {
            return progress0Icon;
        } else {
            switch (numberOfPeers) {
            case 0:
                iconToReturn = shapeTriangleIcon;
                break;
            case 1:
                iconToReturn = shapeSquareIcon;
                break;
            case 2:
                iconToReturn = shapeHeptagonIcon;
                break;
            case 3:
                iconToReturn = shapeHexagonIcon;
                break;
            default:
                iconToReturn = shapeTriangleIcon;
            }
        }
        return iconToReturn;
    }

    public WorldcoinWalletButton getCancelButton() {
        return cancelButton;
    }

    // Used in testing.
    public SendWorldcoinNowAction getSendWorldcoinNowAction() {
        return sendWorldcoinNowAction;
    }

    public String getMessageText1() {
        return confirmText1.getText();
    }

    public String getMessageText2() {
        return confirmText2.getText();
    }

    public void setWalletPassword(CharSequence password) {
        walletPasswordField.setText(password.toString());
    }

    public boolean isWalletPasswordFieldEnabled() {
        return walletPasswordField.isEnabled();
    }

    public WorldcoinWalletLabel getConfirmText2() {
        return confirmText2;
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        enableSendAccordingToNumberOfConnectedPeersAndWalletBusy();
    }
}