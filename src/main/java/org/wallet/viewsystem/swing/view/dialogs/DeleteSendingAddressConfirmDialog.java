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
package org.wallet.viewsystem.swing.view.dialogs;

import org.wallet.viewsystem.swing.view.panels.SendWorldcoinPanel;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.swing.ColorAndFontConstants;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.action.CancelBackToParentAction;
import org.wallet.viewsystem.swing.action.DeleteSendingAddressSubmitAction;
import org.wallet.viewsystem.swing.view.components.FontSizer;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletButton;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletDialog;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletLabel;

/**
 * The delete sending address confirm dialog.
 */
public class DeleteSendingAddressConfirmDialog extends WorldcoinWalletDialog {
    private static final long serialVersionUID = 191435699945057705L;

    private static final int HEIGHT_DELTA = 100;
    private static final int WIDTH_DELTA = 200;

    private final Controller controller;
    private final WorldcoinController worldcoinController;

    private WorldcoinWalletLabel labelText;
    private WorldcoinWalletLabel addressLabelText;

    private WorldcoinWalletLabel explainLabel;

    private WorldcoinWalletButton deleteSendingAddressButton;
    private WorldcoinWalletButton cancelButton;

    private SendWorldcoinPanel sendWorldcoinPanel;

    /**
     * Creates a new {@link DeleteWalletConfirmDialog}.
     */
    public DeleteSendingAddressConfirmDialog(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame,
            SendWorldcoinPanel sendWorldcoinPanel) {
        super(mainFrame, worldcoinController.getLocaliser().getString("deleteSendingAddressConfirmDialog.title"));
        
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        
        this.sendWorldcoinPanel = sendWorldcoinPanel;

        ImageIcon imageIcon = ImageLoader.createImageIcon(ImageLoader.WORLDCOIN_WALLET_ICON_FILE);
        if (imageIcon != null) {
            setIconImage(imageIcon.getImage());
        }

        initUI();

        cancelButton.requestFocusInWindow();
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
    }

    /**
     * Initialise dialog.
     */
    public void initUI() {
        FontMetrics fontMetrics = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());

        int minimumHeight = fontMetrics.getHeight() * 5 + HEIGHT_DELTA;
        int minimumWidth = Math.max(fontMetrics.stringWidth(this.worldcoinController.getModel().getActiveWalletFilename()),
                fontMetrics.stringWidth(controller.getLocaliser().getString("deleteSendingAddressConfirmDialog.message")))
                + WIDTH_DELTA;
        setMinimumSize(new Dimension(minimumWidth, minimumHeight));
        positionDialogRelativeToParent(this, 0.5D, 0.47D);

        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        JLabel filler00 = new JLabel();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.3;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler00, constraints);

        JLabel filler01 = new JLabel();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 5;
        constraints.gridy = 1;
        constraints.weightx = 0.3;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler01, constraints);

        ImageIcon bigIcon = ImageLoader.createImageIcon(ImageLoader.EXCLAMATION_MARK_ICON_FILE);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.5;
        constraints.weighty = 0.2;
        constraints.gridwidth = 1;
        constraints.gridheight = 5;
        constraints.anchor = GridBagConstraints.CENTER;
        JLabel bigIconLabel = new JLabel(bigIcon);
        mainPanel.add(bigIconLabel, constraints);

        explainLabel = new WorldcoinWalletLabel("");
        explainLabel.setText(controller.getLocaliser().getString("deleteSendingAddressConfirmDialog.message"));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 0.8;
        constraints.weighty = 0.3;
        constraints.gridwidth = 5;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(explainLabel, constraints);

        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.weightx = 0.6;
        constraints.weighty = 0.6;
        constraints.gridwidth = 3;
        constraints.gridheight = 5;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(detailPanel, constraints);

        GridBagConstraints constraints2 = new GridBagConstraints();

        JLabel filler0 = new JLabel();
        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 1;
        constraints2.gridy = 0;
        constraints2.weightx = 0.05;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(filler0, constraints2);

        WorldcoinWalletLabel addressLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("sendWorldcoinPanel.addressLabel"));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 0;
        constraints2.gridy = 1;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.2;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(addressLabel, constraints2);

        addressLabelText = new WorldcoinWalletLabel(sendWorldcoinPanel.getAddress());
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 1;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.2;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(addressLabelText, constraints2);

        JLabel filler1 = new JLabel();
        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 1;
        constraints2.gridy = 2;
        constraints2.weightx = 0.1;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(filler1, constraints2);

        WorldcoinWalletLabel labelLabel = new WorldcoinWalletLabel("");
        labelLabel.setText(controller.getLocaliser().getString("sendWorldcoinPanel.labelLabel"));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 0;
        constraints2.gridy = 3;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.2;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_END;
        detailPanel.add(labelLabel, constraints2);

        labelText = new WorldcoinWalletLabel(sendWorldcoinPanel.getLabel());
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 3;
        constraints2.weightx = 0.3;
        constraints2.weighty = 0.2;
        constraints2.gridwidth = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(labelText, constraints2);

        JLabel filler2 = new JLabel();
        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 1;
        constraints2.gridy = 4;
        constraints2.weightx = 0.05;
        constraints2.weighty = 0.05;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        detailPanel.add(filler2, constraints2);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.weightx = 0.8;
        constraints.weighty = 0.1;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(buttonPanel, constraints);

        CancelBackToParentAction cancelAction = new CancelBackToParentAction(controller, null, this);
        cancelButton = new WorldcoinWalletButton(cancelAction, controller);
        buttonPanel.add(cancelButton);

        DeleteSendingAddressSubmitAction deleteSendingAddressSubmitAction = new DeleteSendingAddressSubmitAction(this.worldcoinController,
                sendWorldcoinPanel, this);
        deleteSendingAddressButton = new WorldcoinWalletButton(deleteSendingAddressSubmitAction, controller);
        buttonPanel.add(deleteSendingAddressButton);

        JLabel filler4 = new JLabel();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 5;
        constraints.gridy = 9;
        constraints.weightx = 0.05;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler4, constraints);
    }
}