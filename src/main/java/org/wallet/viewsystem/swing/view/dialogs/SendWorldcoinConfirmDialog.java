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

import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinConfirmPanel;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FontMetrics;

import javax.swing.ImageIcon;

import org.wallet.controller.Controller;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.components.FontSizer;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletDialog;

import com.google.worldcoin.core.Wallet.SendRequest;

/**
 * The send worldcoin confirm dialog.
 */
public class SendWorldcoinConfirmDialog extends WorldcoinWalletDialog {

    private static final long serialVersionUID = 191435612345057705L;

    private static final int HEIGHT_DELTA = 150;
    private static final int WIDTH_DELTA = 400;
        
    private WorldcoinWalletFrame mainFrame;
    private SendWorldcoinConfirmPanel sendWorldcoinConfirmPanel;
    
    private final Controller controller;
    private final WorldcoinController worldcoinController;
    
    private final SendRequest sendRequest;

    /**
     * Creates a new {@link SendWorldcoinConfirmDialog}.
     */
    public SendWorldcoinConfirmDialog(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame, SendRequest sendRequest) {
        super(mainFrame, worldcoinController.getLocaliser().getString("sendWorldcoinConfirmView.title"));
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        this.mainFrame = mainFrame;
        this.sendRequest = sendRequest;

        ImageIcon imageIcon = ImageLoader.createImageIcon(ImageLoader.WORLDCOIN_WALLET_ICON_FILE);
        if (imageIcon != null) {
            setIconImage(imageIcon.getImage());
        }
        
        initUI();
        
        sendWorldcoinConfirmPanel.getCancelButton().requestFocusInWindow();
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
    }

    /**
     * Initialise worldcoin confirm dialog.
     */
    public void initUI() {
        FontMetrics fontMetrics = getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());
        
        if (mainFrame != null) {
            int minimumHeight = fontMetrics.getHeight() * 11 + HEIGHT_DELTA;
            int minimumWidth = Math.max(fontMetrics.stringWidth(WorldcoinWalletFrame.EXAMPLE_LONG_FIELD_TEXT), fontMetrics.stringWidth(controller.getLocaliser().getString("sendWorldcoinConfirmView.message"))) + WIDTH_DELTA;
            setMinimumSize(new Dimension(minimumWidth, minimumHeight));
            positionDialogRelativeToParent(this, 0.5D, 0.47D);
        }
        
        sendWorldcoinConfirmPanel = new SendWorldcoinConfirmPanel(this.worldcoinController, mainFrame, this, sendRequest);
        sendWorldcoinConfirmPanel.setOpaque(false);
        
        setLayout(new BorderLayout());
        add(sendWorldcoinConfirmPanel, BorderLayout.CENTER);
    }
}