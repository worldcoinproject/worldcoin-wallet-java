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
package org.wallet.viewsystem.swing.view.panels;


import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WalletBusyListener;
import org.wallet.model.core.CoreModel;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.DisplayHint;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.Viewable;
import org.wallet.viewsystem.swing.ColorAndFontConstants;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.action.HelpContextAction;
import org.wallet.viewsystem.swing.action.ResetTransactionsSubmitAction;
import org.wallet.viewsystem.swing.view.components.*;

import javax.swing.*;
import java.awt.*;

/**
 * The reset blockchain and transactions view.
 */
public class ResetTransactionsPanel extends JPanel implements Viewable, WalletBusyListener {

    private static final long serialVersionUID = 199992298245057705L;

    private final Controller controller;
    private final WorldcoinController worldcoinController;
    
    private WorldcoinWalletFrame mainFrame;

    private WorldcoinWalletLabel walletFilenameLabel;

    private WorldcoinWalletLabel walletDescriptionLabel;
    
    private ResetTransactionsSubmitAction resetTransactionsSubmitAction;

    /**
     * Creates a new {@link ResetTransactionsPanel}.
     */
    public ResetTransactionsPanel(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame) {
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        this.mainFrame = mainFrame;
        
        setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        setLayout(new BorderLayout());
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        initUI();
        
        this.worldcoinController.registerWalletBusyListener(this);
        walletBusyChange(this.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        GridBagConstraints constraints = new GridBagConstraints();

        String[] keys = new String[] { "resetTransactionsPanel.walletDescriptionLabel",
                "resetTransactionsPanel.walletFilenameLabel" };
        int stentWidth = WorldcoinWalletTitledPanel.calculateStentWidthForKeys(controller.getLocaliser(), keys, this);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(createExplainPanel(stentWidth), constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.weightx = 0.4;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(createButtonPanel(), constraints);

        JLabel filler1 = new JLabel();
        filler1.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(filler1, constraints);

        Action helpAction;
        if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_ICON_FILE,
                    "worldcoinWalletFrame.helpMenuText", "worldcoinWalletFrame.helpMenuTooltip", "worldcoinWalletFrame.helpMenuText",
                    HelpContentsPanel.HELP_RESET_BLOCKCHAIN_URL);
        } else {
            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
                    "worldcoinWalletFrame.helpMenuText", "worldcoinWalletFrame.helpMenuTooltip", "worldcoinWalletFrame.helpMenuText",
                    HelpContentsPanel.HELP_RESET_BLOCKCHAIN_URL);
        }       
        
        HelpButton helpButton = new HelpButton(helpAction, controller);
        helpButton.setText("");
        helpButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        String tooltipText = HelpContentsPanel.createMultilineTooltipText(new String[] { controller.getLocaliser().getString(
                "worldcoinWalletFrame.helpMenuTooltip") });
        helpButton.setToolTipText(tooltipText);
        helpButton.setHorizontalAlignment(SwingConstants.LEADING);
        helpButton.setBorder(BorderFactory.createEmptyBorder(0, AbstractTradePanel.HELP_BUTTON_INDENT,
                AbstractTradePanel.HELP_BUTTON_INDENT, AbstractTradePanel.HELP_BUTTON_INDENT));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        mainPanel.add(helpButton, constraints);

        JLabel filler2 = new JLabel();
        filler2.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 100;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(filler2, constraints);

        JScrollPane mainScrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getViewport().setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        mainScrollPane.getViewport().setOpaque(true);
        mainScrollPane.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        mainScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);

        add(mainScrollPane, BorderLayout.CENTER);
    }

    private JPanel createExplainPanel(int stentWidth) {
        WorldcoinWalletTitledPanel explainPanel = new WorldcoinWalletTitledPanel(controller.getLocaliser().getString(
                "resetTransactionsPanel.explainTitle"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        explainPanel.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();

        WorldcoinWalletTitledPanel.addLeftJustifiedTextAtIndent(" ", 3, explainPanel);

        String explainText1 = controller.getLocaliser().getString("resetTransactionsPanel.explainLabel.text1");
        WorldcoinWalletTextArea explainTextArea1 = new WorldcoinWalletTextArea(explainText1, 2, 40, controller);
        explainTextArea1.setOpaque(true);
        explainTextArea1.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        explainTextArea1.setWrapStyleWord(true);
        explainTextArea1.setLineWrap(true);
        explainTextArea1.setEditable(false);

        FontMetrics fontMetrics = getFontMetrics(getFont());
        int preferredWidth = fontMetrics.stringWidth(WelcomePanel.EXAMPLE_TEXT);
        int fontHeight = fontMetrics.getHeight();
        int height1 = WelcomePanel.calculateHeight(explainText1);

        Dimension preferredSize = new Dimension(preferredWidth, height1 * fontHeight);
        explainTextArea1.setMinimumSize(preferredSize);
        explainTextArea1.setPreferredSize(preferredSize);
        explainTextArea1.setMaximumSize(preferredSize);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.LINE_START;
        explainPanel.add(explainTextArea1, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        explainPanel.add(WorldcoinWalletTitledPanel.createStent(stentWidth), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        explainPanel.add(WorldcoinWalletTitledPanel.createStent(WorldcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS), constraints);

        WorldcoinWalletLabel walletFilenameLabelLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString(
                "resetTransactionsPanel.walletFilenameLabel"));

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        explainPanel.add(walletFilenameLabelLabel, constraints);

        walletFilenameLabel = new WorldcoinWalletLabel(this.worldcoinController.getModel().getActiveWalletFilename());
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 6;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        explainPanel.add(walletFilenameLabel, constraints);

        WorldcoinWalletLabel walletDescriptionLabelLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString(
                "resetTransactionsPanel.walletDescriptionLabel"));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        explainPanel.add(walletDescriptionLabelLabel, constraints);

        walletDescriptionLabel = new WorldcoinWalletLabel(this.worldcoinController.getModel().getActivePerWalletModelData().getWalletDescription());
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 5;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        explainPanel.add(walletDescriptionLabel, constraints);

        JPanel filler3 = new JPanel();
        filler3.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.weightx = 0.3;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        explainPanel.add(filler3, constraints);

        return explainPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.TRAILING);
        buttonPanel.setLayout(flowLayout);
        buttonPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        resetTransactionsSubmitAction = new ResetTransactionsSubmitAction(this.worldcoinController, mainFrame,
                ImageLoader.createImageIcon(ImageLoader.RESET_TRANSACTIONS_ICON_FILE));
        WorldcoinWalletButton submitButton = new WorldcoinWalletButton(resetTransactionsSubmitAction, controller);
        buttonPanel.add(submitButton);

        return buttonPanel;
    }

    @Override
    public void navigateAwayFromView() {
    }

    /**
     * Show explanatory text for resetting blockchain and transactions and a button to do it.
     */
    @Override
    public void displayView(DisplayHint displayHint) {
        // If it is a wallet transaction change no need to update.
        if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
            return;
        }
        walletFilenameLabel.setText(this.worldcoinController.getModel().getActiveWalletFilename());
        walletDescriptionLabel.setText(this.worldcoinController.getModel().getActivePerWalletModelData().getWalletDescription());
        
        walletBusyChange(this.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    @Override
    public Icon getViewIcon() {
        return ImageLoader.createImageIcon(ImageLoader.RESET_TRANSACTIONS_ICON_FILE);
    }

    @Override
    public String getViewTitle() {
        return controller.getLocaliser().getString("resetTransactionsAction.text");
    }

    @Override
    public String getViewTooltip() {
        return controller.getLocaliser().getString("resetTransactionsAction.tooltip");
    }

    @Override
    public View getViewId() {
        return View.RESET_TRANSACTIONS_VIEW;
    }

    public ResetTransactionsSubmitAction getResetTransactionsSubmitAction() {
        return resetTransactionsSubmitAction;
    }
    
    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {       
        // Update the enable status of the action to match the wallet busy status.
        if (this.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // WorldcoinWallet is busy with another operation that may change the private keys - Action is disabled.
            resetTransactionsSubmitAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("worldcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())})));
            resetTransactionsSubmitAction.setEnabled(false);           
        } else {
            // Enable unless wallet has been modified by another process.
            if (!this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                resetTransactionsSubmitAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("resetTransactionsSubmitAction.tooltip")));
                resetTransactionsSubmitAction.setEnabled(true);
            }
        }
    }
}