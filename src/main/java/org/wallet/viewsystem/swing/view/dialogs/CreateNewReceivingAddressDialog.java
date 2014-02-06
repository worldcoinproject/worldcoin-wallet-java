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

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.components.FontSizer;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletDialog;
import org.wallet.viewsystem.swing.view.panels.CreateNewReceivingAddressPanel;
import org.wallet.viewsystem.swing.view.panels.ReceiveWorldcoinPanel;

import javax.swing.*;
import java.awt.*;

/**
 * The dialog for creating new receiving addresses.
 */
public class CreateNewReceivingAddressDialog extends WorldcoinWalletDialog {

    private static final long serialVersionUID = 191439652345057705L;

    private static final int HEIGHT_DELTA = 40;
    private static final int WIDTH_DELTA = 160;
 
    private ReceiveWorldcoinPanel receiveWorldcoinPanel;

    private final Controller controller;
    private final WorldcoinController worldcoinController;
    
    private CreateNewReceivingAddressPanel createNewReceivingAddressPanel; 

    /**
     * Creates a new {@link CreateNewReceivingAddressDialog}.
     */
    public CreateNewReceivingAddressDialog(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame, ReceiveWorldcoinPanel receiveWorldcoinPanel) {
        super(mainFrame, worldcoinController.getLocaliser().getString("createNewReceivingAddressDialog.title"));
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        this.receiveWorldcoinPanel = receiveWorldcoinPanel;
      
        initUI();
        
        createNewReceivingAddressPanel.getCancelButton().requestFocusInWindow();
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
    }

    /**
     * Initialise dialog.
     */
    public void initUI() {
        try {
            FontMetrics fontMetrics = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());
        
            int minimumHeight = fontMetrics.getHeight() * 8 + HEIGHT_DELTA;
            int minimumWidth = Math.max(
                fontMetrics.stringWidth(
                    controller.getLocaliser().getString("createNewReceivingAddressDialog.message")),
                fontMetrics.stringWidth(
                    controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.createdSuccessfullyShort",
                        new Object[] {100}))) + WIDTH_DELTA;
            setMinimumSize(new Dimension(minimumWidth, minimumHeight));
            positionDialogRelativeToParent(this, 0.5D, 0.47D);
        } catch (NullPointerException npe) {
            // FontSizer fail - probably headless in test - carry on.
        }

        createNewReceivingAddressPanel = new CreateNewReceivingAddressPanel(this.worldcoinController, receiveWorldcoinPanel, this);
        
        setLayout(new BorderLayout());
        add(createNewReceivingAddressPanel, BorderLayout.CENTER);
    }

    public ReceiveWorldcoinPanel getReceiveWorldcoinPanel() {
        return receiveWorldcoinPanel;
    }
    
    public int getNumberOfAddressesToCreate() {
        return createNewReceivingAddressPanel.getNumberOfAddressesToCreate();
    }

    public JComboBox getNumberOfAddresses() {
        return createNewReceivingAddressPanel.getNumberOfAddresses();
    }
}