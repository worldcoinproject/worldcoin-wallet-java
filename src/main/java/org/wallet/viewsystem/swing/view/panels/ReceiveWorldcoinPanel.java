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

import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.exchange.CurrencyConverterResult;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.model.worldcoin.WalletAddressBookData;
import org.wallet.model.worldcoin.WalletInfoData;
import org.wallet.model.core.CoreModel;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.DisplayHint;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.Viewable;
import org.wallet.viewsystem.swing.ColorAndFontConstants;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.action.*;
import org.wallet.viewsystem.swing.view.components.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;

public class ReceiveWorldcoinPanel extends AbstractTradePanel implements Viewable {

    static final Logger log = LoggerFactory.getLogger(ReceiveWorldcoinPanel.class);

    private static final long serialVersionUID = -2065108865497842662L;

    private CreateNewReceivingAddressAction createNewReceivingAddressAction;

    public ReceiveWorldcoinPanel(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame) {
        super(mainFrame, worldcoinController);
    }
    
    @Override
    protected boolean isReceiveWorldcoin() {
        return true;
    }
    
    @Override
    protected Action getCreateNewAddressAction() {
        createNewReceivingAddressAction = new CreateNewReceivingAddressAction(super.worldcoinController, mainFrame, this);
        return createNewReceivingAddressAction;
    }
    
    @Override
    protected Action getDeleteAddressAction() {
        // Return a delete sending address action - it gets turned into a stent
        return new DeleteSendingAddressAction(super.worldcoinController, mainFrame, null);
    }
 
    @Override
    public String getAddressConstant() {
        return WorldcoinModel.RECEIVE_ADDRESS;
    }
    
    @Override
    public String getLabelConstant() {
        return WorldcoinModel.RECEIVE_LABEL;
    }
    @Override
    public String getAmountConstant() {
        return WorldcoinModel.RECEIVE_AMOUNT;
    }
    
    /**
     * method for concrete impls to populate the localisation map
     */
    @Override
    protected void populateLocalisationMap() {
        localisationKeyConstantToKeyMap.put(ADDRESSES_TITLE, "receiveWorldcoinPanel.receivingAddressesTitle");
        localisationKeyConstantToKeyMap.put(CREATE_NEW_TOOLTIP, "createOrEditAddressAction.createReceiving.tooltip");       
    }

    @Override
    protected JPanel createFormPanel(JPanel formPanel, GridBagConstraints constraints) {
        formPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

        JPanel buttonPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEADING);
        buttonPanel.setLayout(flowLayout);

        formPanel.setLayout(new GridBagLayout());
        JPanel filler1 = new JPanel();
        filler1.setOpaque(false);

        // create stents and forcers
        createFormPanelStentsAndForcers(formPanel,constraints);

        WorldcoinWalletLabel addressLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("receiveWorldcoinPanel.addressLabel"));
        addressLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("receiveWorldcoinPanel.addressLabel.tooltip")));
        addressLabel.setBorder(BorderFactory.createMatteBorder((int)(TEXTFIELD_VERTICAL_DELTA * 0.5), 0, (int)(TEXTFIELD_VERTICAL_DELTA * 0.5), 0, ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR));
        addressLabel.setHorizontalAlignment(JLabel.TRAILING);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        formPanel.add(addressLabel, constraints);

        FontMetrics fontMetric = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());
        int longFieldWidth = fontMetric.stringWidth(WorldcoinWalletFrame.EXAMPLE_LONG_FIELD_TEXT);
        addressTextField = new WorldcoinWalletTextField("", 24, controller);

        //addressTextField = new WorldcoinWalletTextArea("", 24, 1, controller);
        addressTextField.setEditable(false);
        addressTextField.setOpaque(false);
        Insets insets = addressTextField.getBorder().getBorderInsets(addressTextField);
        addressTextField.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right));
        addressTextField.setMinimumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight()));
        
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.weightx = 0.1;
        constraints.weighty = 0.2;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        formPanel.add(addressTextField, constraints);

        ImageIcon copyIcon = ImageLoader.createImageIcon(ImageLoader.COPY_ICON_FILE);
        CopyReceiveAddressAction copyAddressAction = new CopyReceiveAddressAction(controller, this, copyIcon);
        WorldcoinWalletButton copyAddressButton = new WorldcoinWalletButton(copyAddressAction, controller);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 6;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        formPanel.add(copyAddressButton, constraints);
        
        JPanel pasteButtonStent = WorldcoinWalletTitledPanel.createStent((int)copyAddressButton.getPreferredSize().getWidth(), (int)copyAddressButton.getPreferredSize().getHeight());
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 8;
        constraints.gridy = 1;
        constraints.weightx = 10.0;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        formPanel.add(pasteButtonStent, constraints);

        WorldcoinWalletLabel labelLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("receiveWorldcoinPanel.labelLabel"));
        labelLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("receiveWorldcoinPanel.labelLabel.tooltip")));
        labelLabel.setHorizontalAlignment(JLabel.TRAILING);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0.1;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        formPanel.add(labelLabel, constraints);

        JTextField aTextField = new JTextField();
        labelTextArea = new WorldcoinWalletTextArea("", AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS , 20, controller);
        labelTextArea.setBorder(aTextField.getBorder());
        labelTextArea.addKeyListener(new QRCodeKeyListener());
 
        final JScrollPane labelScrollPane = new JScrollPane(labelTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        labelScrollPane.setOpaque(true);
        labelScrollPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        labelScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); 
        labelScrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (labelScrollPane.getVerticalScrollBar().isVisible()) {
                    labelScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));          
                } else {
                    labelScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));              
                }
            }
        });
        labelScrollPane.setMinimumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
                .getHeight() * AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + TEXTFIELD_VERTICAL_DELTA));

        labelScrollPane.setMinimumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
                .getHeight() * AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + TEXTFIELD_VERTICAL_DELTA + 6));
        labelScrollPane.setPreferredSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
                .getHeight() * AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + TEXTFIELD_VERTICAL_DELTA + 6));
        labelScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        labelScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.weightx = 0.6;
        constraints.weighty = 1.0;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        formPanel.add(labelScrollPane, constraints);

        WorldcoinWalletLabel amountLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("receiveWorldcoinPanel.amountLabel"));
        amountLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("receiveWorldcoinPanel.amountLabel.tooltip")));
        amountLabel.setHorizontalAlignment(JLabel.TRAILING);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 0.1;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        formPanel.add(amountLabel, constraints);

        JPanel amountPanel = createAmountPanel();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.1;
        constraints.weighty = 0.20;
        constraints.anchor = GridBagConstraints.LINE_START;
        formPanel.add(amountPanel, constraints);

        notificationLabel = new WorldcoinWalletLabel("");
        notificationLabel.setForeground(Color.RED);
        //notificationLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.gridwidth = 8;
        constraints.gridheight = 3;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;
        formPanel.add(notificationLabel, constraints);
        
        Action helpAction;
        if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_ICON_FILE,
                    "worldcoinWalletFrame.helpMenuText", "worldcoinWalletFrame.helpMenuTooltip", "worldcoinWalletFrame.helpMenuText",
                    HelpContentsPanel.HELP_RECEIVING_URL);
        } else {
            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
                    "worldcoinWalletFrame.helpMenuText", "worldcoinWalletFrame.helpMenuTooltip", "worldcoinWalletFrame.helpMenuText",
                    HelpContentsPanel.HELP_RECEIVING_URL);
        }
        HelpButton helpButton = new HelpButton(helpAction, controller);
        helpButton.setText("");
 
        String tooltipText = HelpContentsPanel.createMultilineTooltipText(new String[]{controller.getLocaliser().getString("receiveWorldcoinPanel.helpLabel1.message"),
                controller.getLocaliser().getString("receiveWorldcoinPanel.helpLabel2.message"), controller.getLocaliser().getString("receiveWorldcoinPanel.helpLabel3.message"),
                "\n", controller.getLocaliser().getString("worldcoinWalletFrame.helpMenuTooltip") });
        helpButton.setToolTipText(tooltipText);
        helpButton.setHorizontalAlignment(SwingConstants.LEADING);
        helpButton.setBorder(BorderFactory.createEmptyBorder(0, HELP_BUTTON_INDENT, HELP_BUTTON_INDENT, HELP_BUTTON_INDENT));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.weightx = 1;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.BELOW_BASELINE_LEADING;
        formPanel.add(helpButton, constraints);
        
        SendWorldcoinConfirmAction sendWorldcoinConfirmAction = new SendWorldcoinConfirmAction(super.worldcoinController, mainFrame, this);
        WorldcoinWalletButton notUsedSendButton = new WorldcoinWalletButton(sendWorldcoinConfirmAction, controller);
        JPanel sendButtonStent = WorldcoinWalletTitledPanel.createStent((int)notUsedSendButton.getPreferredSize().getWidth(), (int)notUsedSendButton.getPreferredSize().getHeight());
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 6;
        constraints.gridy = 5;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        formPanel.add(sendButtonStent, constraints);

        Action sidePanelAction = new MoreOrLessAction(controller, this);
        sidePanelButton = new WorldcoinWalletButton(sidePanelAction, controller);
        sidePanelButton.setBorder(BorderFactory.createEmptyBorder());
        sidePanelButton.setBorderPainted(false);
        sidePanelButton.setFocusPainted(false);
        sidePanelButton.setContentAreaFilled(false);

        displaySidePanel();

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 3;
        constraints.weightx = 0.1;
        constraints.weighty = 0.3;
        constraints.gridwidth = 8;
        constraints.gridheight = 3;
        constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
        formPanel.add(sidePanelButton, constraints);

        // disable any new changes if another process has changed the wallet
        if (this.worldcoinController.getModel().getActivePerWalletModelData() != null
                && this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
            // files have been changed by another process - disallow edits
            labelTextArea.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("singleWalletPanel.dataHasChanged.tooltip")));
            mainFrame.setUpdatesStoppedTooltip(labelTextArea);

            labelTextArea.setEditable(false);
            labelTextArea.setEnabled(false);
            mainFrame.setUpdatesStoppedTooltip(amountWDCTextField);
            amountWDCTextField.setEditable(false);
            amountWDCTextField.setEnabled(false);
        } else {
            labelTextArea.setToolTipText(null);
            labelTextArea.setEditable(true);
            labelTextArea.setEnabled(true);
            amountWDCTextField.setToolTipText(null);
            amountWDCTextField.setEditable(true);
            amountWDCTextField.setEnabled(true);
        }

        return formPanel;
    }

    public String getReceiveAddress() {
        if (addressTextField != null) {
            return addressTextField.getText();
        } else {
            return "";
        }
    }
    
    @Override
    public void loadForm() {
        // get the current address, label and amount from the model
        String address = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.RECEIVE_ADDRESS);
        String label = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.RECEIVE_LABEL);

        String amountNotLocalised = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.RECEIVE_AMOUNT);

        if (amountWDCTextField != null) {
            CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDCNotLocalised(amountNotLocalised);
            
            if (converterResult.isWdcMoneyValid()) {
                parsedAmountWDC = converterResult.getWdcMoney();
                String amountLocalised = CurrencyConverter.INSTANCE.getWDCAsLocalisedString(converterResult.getWdcMoney());
                amountWDCTextField.setText(amountLocalised);
                if (notificationLabel != null) {
                    notificationLabel.setText("");
                }
            } else {
                parsedAmountWDC = null;
                amountWDCTextField.setText("");
                if (notificationLabel != null) {
                    notificationLabel.setText(converterResult.getWdcMessage());
                }
            }
        }
        
        // if the currently stored address is missing or is not in this wallet,
        // pick
        // the address book's first receiving address
        boolean pickFirstReceivingAddress = false;
        if (address == null || address.equals("")) {
            pickFirstReceivingAddress = true;
        } else {
            WalletInfoData addressBook = this.worldcoinController.getModel().getActiveWalletInfo();
            if (addressBook != null) {
                if (!addressBook.containsReceivingAddress(address)) {
                    pickFirstReceivingAddress = true;
                }
            }
        }

        if (pickFirstReceivingAddress) {
            WalletInfoData addressBook = this.worldcoinController.getModel().getActiveWalletInfo();
            if (addressBook != null) {
                ArrayList<WalletAddressBookData> receivingAddresses = addressBook.getReceivingAddresses();
                if (receivingAddresses != null) {
                    if (receivingAddresses.iterator().hasNext()) {
                        WalletAddressBookData addressBookData = receivingAddresses.iterator().next();
                        if (addressBookData != null) {
                            address = addressBookData.getAddress();
                            label = addressBookData.getLabel();
                            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.RECEIVE_ADDRESS, address);
                            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.RECEIVE_LABEL, label);
                        }
                    }
                }
            }
        }

        if (address != null) {
            addressTextField.setText(address);
        }
        if (label != null) {
            labelTextArea.setText(label);
        }
    }

    @Override
    public void displayView(DisplayHint displayHint) {
        super.displayView(displayHint);
        
        if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
            return;
        }
        
        JTextField aTextField = new JTextField();
        labelTextArea.setBorder(aTextField.getBorder());

        // disable any new changes if another process has changed the wallet
        if (this.worldcoinController.getModel().getActivePerWalletModelData() != null
                && this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
            // files have been changed by another process - disallow edits
            titleLabel.setText(controller.getLocaliser()
                    .getString("receiveWorldcoinPanel.receivingAddressesTitle.mayBeOutOfDate"));
            mainFrame.setUpdatesStoppedTooltip(titleLabel);
        } else {
            titleLabel.setText(controller.getLocaliser().getString("receiveWorldcoinPanel.receivingAddressesTitle"));
            titleLabel.setToolTipText(null);
        }
    }
    
    @Override
    public Icon getViewIcon() {
        return ImageLoader.createImageIcon(ImageLoader.RECEIVE_WORLDCOIN_ICON_FILE);
    }

    @Override
    public String getViewTitle() {
        return controller.getLocaliser().getString("receiveWorldcoinAction.textShort");
    }
  
    @Override
    public String getViewTooltip() {
        return controller.getLocaliser().getString("receiveWorldcoinAction.tooltip");
    }

    @Override
    public View getViewId() {
        return View.RECEIVE_WORLDCOIN_VIEW;
    }
    
    public CreateNewReceivingAddressAction getCreateNewReceivingAddressAction() {
        return createNewReceivingAddressAction;
    }

    @Override
    public void checkDeleteSendingEnabled() {
        // Not used on receive worldcoin panel.
    }
}
