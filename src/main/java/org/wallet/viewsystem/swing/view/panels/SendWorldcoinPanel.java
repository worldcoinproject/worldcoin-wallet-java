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
package org.wallet.viewsystem.swing.view.panels;

import com.google.worldcoin.core.Address;
import com.google.worldcoin.core.AddressFormatException;
import com.google.worldcoin.core.Utils;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.exchange.CurrencyConverterResult;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.model.worldcoin.WalletAddressBookData;
import org.wallet.model.core.CoreModel;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.DisplayHint;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.Viewable;
import org.wallet.viewsystem.swing.ColorAndFontConstants;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.action.*;
import org.wallet.viewsystem.swing.view.components.*;
import org.wallet.viewsystem.swing.view.models.AddressBookTableModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class SendWorldcoinPanel extends AbstractTradePanel implements Viewable {

  private static final long serialVersionUID = -2065108865497111662L;
  private static SendWorldcoinConfirmAction sendWorldcoinConfirmAction;
  private static boolean enableSendButton = false;
  private static WorldcoinWalletButton sendButton;
  private WorldcoinWalletButton pasteAddressButton;

  private static SendWorldcoinPanel thisPanel;

  private static String regularTooltipText = "";
  private static String pleaseWaitTooltipText = "";

  public SendWorldcoinPanel(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame) {
    super(mainFrame, worldcoinController);
    thisPanel = this;
    checkDeleteSendingEnabled();

    regularTooltipText = controller.getLocaliser().getString("sendWorldcoinAction.tooltip");
    pleaseWaitTooltipText = controller.getLocaliser().getString("sendWorldcoinAction.pleaseWait.tooltip");
  }

  public static void setEnableSendButton(boolean enableSendButton) {
    SendWorldcoinPanel.enableSendButton = enableSendButton;

    if (EventQueue.isDispatchThread()) {
      enableSendButtonOnSwingThread();
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          enableSendButtonOnSwingThread();
        }
      });
    }
  }

  private static void enableSendButtonOnSwingThread() {
    if (sendWorldcoinConfirmAction != null) {
      sendWorldcoinConfirmAction.setEnabled(SendWorldcoinPanel.enableSendButton);
    }

    final String finalRegularTooltipText = regularTooltipText;
    final String finalPleaseWaitTooltipText = pleaseWaitTooltipText;

    if (sendButton != null) {
      if (SendWorldcoinPanel.enableSendButton) {
        sendButton.setEnabled(true);
        sendButton.setToolTipText(HelpContentsPanel.createTooltipText(finalRegularTooltipText));
      } else {
        sendButton.setEnabled(false);
        sendButton.setToolTipText(HelpContentsPanel.createTooltipText(finalPleaseWaitTooltipText));
      }
    }

    if (thisPanel != null) {
      thisPanel.invalidate();
      thisPanel.validate();
      thisPanel.repaint();
    }
  }

  @Override
  protected boolean isReceiveWorldcoin() {
    return false;
  }

  @Override
  public Action getCreateNewAddressAction() {
    return new CreateNewSendingAddressAction(super.worldcoinController, this);
  }

  @Override
  protected Action getDeleteAddressAction() {
    if (deleteAddressAction == null) {
      return new DeleteSendingAddressAction(this.worldcoinController, mainFrame, this);
    } else {
      return deleteAddressAction;
    }
  }

  @Override
  public void checkDeleteSendingEnabled() {
    AddressBookTableModel addressesTableModel = getAddressesTableModel();
    if (deleteAddressAction != null) {
      deleteAddressAction.setEnabled(addressesTableModel != null && addressesTableModel.getRowCount() > 0);
    }
  }

  @Override
  public String getAddressConstant() {
    return WorldcoinModel.SEND_ADDRESS;
  }

  @Override
  public String getLabelConstant() {
    return WorldcoinModel.SEND_LABEL;
  }

  @Override
  public String getAmountConstant() {
    return WorldcoinModel.SEND_AMOUNT;
  }

  /**
   * method for concrete impls to populate the localisation map
   */
  @Override
  protected void populateLocalisationMap() {
    localisationKeyConstantToKeyMap.put(ADDRESSES_TITLE, "sendWorldcoinPanel.sendingAddressesTitle");
    localisationKeyConstantToKeyMap.put(CREATE_NEW_TOOLTIP, "createOrEditAddressAction.createSending.tooltip");
    localisationKeyConstantToKeyMap.put(DELETE_TOOLTIP, "deleteSendingAddressSubmitAction.tooltip");
  }

  @Override
  protected JPanel createFormPanel(JPanel formPanel, GridBagConstraints constraints) {
    formPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

    JPanel buttonPanel = new JPanel();
    FlowLayout flowLayout = new FlowLayout();
    flowLayout.setAlignment(FlowLayout.LEADING);
    buttonPanel.setLayout(flowLayout);

    formPanel.setLayout(new GridBagLayout());

    // create stents and forcers
    createFormPanelStentsAndForcers(formPanel, constraints);

    WorldcoinWalletLabel addressLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("sendWorldcoinPanel.addressLabel"));
    addressLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinPanel.addressLabel.tooltip")));
    addressLabel.setHorizontalAlignment(JLabel.TRAILING);
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weightx = 4.0;
    constraints.weighty = 0.2;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.LINE_END;
    formPanel.add(addressLabel, constraints);
    String receiveAddressText = controller.getLocaliser().getString("receiveWorldcoinPanel.addressLabel");
    WorldcoinWalletLabel notUsedReceiveAddressLabel = new WorldcoinWalletLabel(receiveAddressText);
    formPanel.add(WorldcoinWalletTitledPanel.createStent((int) notUsedReceiveAddressLabel.getPreferredSize().getWidth()), constraints);

    int longFieldWidth = fontMetrics.stringWidth(WorldcoinWalletFrame.EXAMPLE_LONG_FIELD_TEXT);
    addressTextField = new WorldcoinWalletTextField("", 24, controller);
    addressTextField.setHorizontalAlignment(JTextField.LEADING);
    addressTextField.setMinimumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
            .getHeight() + TEXTFIELD_VERTICAL_DELTA));
    addressTextField.setPreferredSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
            .getHeight() + TEXTFIELD_VERTICAL_DELTA));
    addressTextField.setMaximumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
            .getHeight() + TEXTFIELD_VERTICAL_DELTA));

    addressTextField.addKeyListener(new QRCodeKeyListener());
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 2;
    constraints.gridy = 1;
    constraints.weightx = 1.0;
    constraints.weighty = 0.2;
    constraints.gridwidth = 3;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(addressTextField, constraints);

    ImageIcon copyIcon = ImageLoader.createImageIcon(ImageLoader.COPY_ICON_FILE);
    CopySendAddressAction copyAddressAction = new CopySendAddressAction(controller, this, copyIcon);
    WorldcoinWalletButton copyAddressButton = new WorldcoinWalletButton(copyAddressAction, controller);
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 6;
    constraints.gridy = 1;
    constraints.weightx = 1;
    constraints.gridwidth = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(copyAddressButton, constraints);

    ImageIcon pasteIcon = ImageLoader.createImageIcon(ImageLoader.PASTE_ICON_FILE);
    PasteAddressAction pasteAddressAction = new PasteAddressAction(super.worldcoinController, this, pasteIcon);
    pasteAddressButton = new WorldcoinWalletButton(pasteAddressAction, controller);
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 8;
    constraints.gridy = 1;
    constraints.weightx = 10.0;
    constraints.weighty = 0.2;
    constraints.gridwidth = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(pasteAddressButton, constraints);

    WorldcoinWalletLabel labelLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("sendWorldcoinPanel.labelLabel"));
    labelLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinPanel.labelLabel.tooltip")));
    labelLabel.setHorizontalAlignment(JLabel.TRAILING);
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 0;
    constraints.gridy = 3;
    constraints.weightx = 0.1;
    constraints.weighty = 1.0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.LINE_END;
    formPanel.add(labelLabel, constraints);

    JTextField aTextField = new JTextField();
    labelTextArea = new WorldcoinWalletTextArea("", AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS, 20, controller);
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
    constraints.gridwidth = 3;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(labelScrollPane, constraints);

    WorldcoinWalletLabel amountLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("sendWorldcoinPanel.amountLabel"));
    amountLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinPanel.amountLabel.tooltip")));
    amountLabel.setHorizontalAlignment(JLabel.TRAILING);
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 0;
    constraints.gridy = 5;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 0.1;
    constraints.weighty = 0.20;
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
              HelpContentsPanel.HELP_SENDING_URL);
    } else {
      helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
              "worldcoinWalletFrame.helpMenuText", "worldcoinWalletFrame.helpMenuTooltip", "worldcoinWalletFrame.helpMenuText",
              HelpContentsPanel.HELP_SENDING_URL);
    }
    HelpButton helpButton = new HelpButton(helpAction, controller);
    helpButton.setText("");

    String tooltipText = HelpContentsPanel.createMultilineTooltipText(new String[]{
            controller.getLocaliser().getString("sendWorldcoinPanel.helpLabel1.message"),
            controller.getLocaliser().getString("sendWorldcoinPanel.helpLabel2.message"),
            controller.getLocaliser().getString("sendWorldcoinPanel.helpLabel3.message"), "\n",
            controller.getLocaliser().getString("worldcoinWalletFrame.helpMenuTooltip")});
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

    sendWorldcoinConfirmAction = new SendWorldcoinConfirmAction(super.worldcoinController, mainFrame, this);
    sendWorldcoinConfirmAction.setEnabled(enableSendButton);
    sendButton = new WorldcoinWalletButton(sendWorldcoinConfirmAction, controller);
    if (enableSendButton) {
      sendButton.setEnabled(true);
      sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinAction.tooltip")));
    } else {
      sendButton.setEnabled(false);
      sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinAction.pleaseWait.tooltip")));
    }

    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 6;
    constraints.gridy = 5;
    constraints.weightx = 0.1;
    constraints.weighty = 0.1;
    constraints.gridwidth = 3;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(sendButton, constraints);

    Action sidePanelAction = new MoreOrLessAction(controller, this);
    sidePanelButton = new WorldcoinWalletButton(sidePanelAction, controller);
    sidePanelButton.setBorder(BorderFactory.createEmptyBorder());
    sidePanelButton.setBorderPainted(false);
    sidePanelButton.setFocusPainted(false);
    sidePanelButton.setContentAreaFilled(false);

    displaySidePanel();

    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 4;
    constraints.gridy = 3;
    constraints.weightx = 0.1;
    constraints.weighty = 0.3;
    constraints.gridwidth = 7;
    constraints.gridheight = 3;
    constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
    formPanel.add(sidePanelButton, constraints);

    return formPanel;
  }

  @Override
  public String getAddress() {
    if (addressTextField != null) {
      return addressTextField.getText();
    } else {
      return "";
    }
  }

  @Override
  public void loadForm() {
    // get the current address, label and amount from the model
    String address = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_ADDRESS);
    String label = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_LABEL);
    String amountNotLocalised = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_AMOUNT);

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

    if (address != null) {
      addressTextField.setText(address);
    } else {
      addressTextField.setText("");
    }
    if (label != null) {
      labelTextArea.setText(label);
    } else {
      labelTextArea.setText("");
    }

    // if there is a pending 'handleopenURI' that needs pasting into the
    // send form, do it
    String performPasteNow = this.worldcoinController.getModel().getActiveWalletPreference(WorldcoinModel.SEND_PERFORM_PASTE_NOW);
    if (Boolean.TRUE.toString().equalsIgnoreCase(performPasteNow)) {
      try {
        Address decodeAddress = new Address(this.worldcoinController.getModel().getNetworkParameters(), address);
        processDecodedString(com.google.worldcoin.uri.WorldcoinURI.convertToWorldcoinURI(decodeAddress, Utils.toNanoCoins(amountNotLocalised), label, null), null);
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_PERFORM_PASTE_NOW, "false");
        sendButton.requestFocusInWindow();

        mainFrame.bringToFront();
      } catch (AddressFormatException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void setAddressBookDataByRow(WalletAddressBookData addressBookData) {
    addressTextField.setText(addressBookData.getAddress());
    addressesTableModel.setAddressBookDataByRow(addressBookData, selectedAddressRowModel, false);
  }

  @Override
  public void displayView(DisplayHint displayHint) {
    super.displayView(displayHint);

    if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
      return;
    }

    JTextField aTextField = new JTextField();

    labelTextArea.setBorder(aTextField.getBorder());

    String bringToFront = controller.getModel().getUserPreference(WorldcoinModel.BRING_TO_FRONT);
    if (Boolean.TRUE.toString().equals(bringToFront)) {
      controller.getModel().setUserPreference(WorldcoinModel.BRING_TO_FRONT, "false");
      mainFrame.bringToFront();
    }

    // disable any new changes if another process has changed the wallet
    if (this.worldcoinController.getModel().getActivePerWalletModelData() != null
            && this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
      // files have been changed by another process - disallow edits
      mainFrame.setUpdatesStoppedTooltip(addressTextField);
      addressTextField.setEditable(false);
      addressTextField.setEnabled(false);

      if (sendButton != null) {
        sendButton.setEnabled(false);
        mainFrame.setUpdatesStoppedTooltip(sendButton);
      }
      if (pasteAddressButton != null) {
        pasteAddressButton.setEnabled(false);
        mainFrame.setUpdatesStoppedTooltip(pasteAddressButton);
      }
      titleLabel.setText(controller.getLocaliser().getString("sendWorldcoinPanel.sendingAddressesTitle.mayBeOutOfDate"));
      mainFrame.setUpdatesStoppedTooltip(titleLabel);
    } else {
      addressTextField.setToolTipText(null);
      addressTextField.setEditable(true);
      addressTextField.setEnabled(true);

      if (sendButton != null) {
        if (SendWorldcoinPanel.enableSendButton) {
          sendButton.setEnabled(true);
          sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinAction.tooltip")));
        } else {
          sendButton.setEnabled(false);
          sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinAction.pleaseWait.tooltip")));
        }
      }
      if (pasteAddressButton != null) {
        pasteAddressButton.setEnabled(true);
        pasteAddressButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("pasteAddressAction.tooltip")));
      }
      titleLabel.setText(controller.getLocaliser().getString("sendWorldcoinPanel.sendingAddressesTitle"));
      titleLabel.setToolTipText(null);
    }
    checkDeleteSendingEnabled();
  }

  @Override
  public Icon getViewIcon() {
    return ImageLoader.createImageIcon(ImageLoader.SEND_WORLDCOIN_ICON_FILE);
  }

  @Override
  public String getViewTitle() {
    return controller.getLocaliser().getString("sendWorldcoinConfirmAction.text");
  }

  @Override
  public String getViewTooltip() {
    return controller.getLocaliser().getString("sendWorldcoinConfirmAction.tooltip");
  }

  @Override
  public View getViewId() {
    return View.SEND_WORLDCOIN_VIEW;
  }

  public SendWorldcoinConfirmAction getSendWorldcoinConfirmAction() {
    return sendWorldcoinConfirmAction;
  }
}
