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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.wallet.controller.Controller;
import org.wallet.viewsystem.swing.view.panels.HelpContentsPanel;
import org.wallet.viewsystem.swing.view.panels.ReceiveWorldcoinPanel;

/**
 * This {@link Action} represents the swing copy receive address action
 */
public class CopyReceiveAddressAction extends AbstractAction {

    private static final long serialVersionUID = 191352235465057705L;

    private ReceiveWorldcoinPanel receiveWorldcoinPanel;

    /**
     * Creates a new {@link CopyReceiveAddressAction}.
     * 
     * @param copyIcon
     */
    public CopyReceiveAddressAction(Controller controller, ReceiveWorldcoinPanel receiveWorldcoinPanel, ImageIcon copyIcon) {
        super("", copyIcon);
        this.receiveWorldcoinPanel = receiveWorldcoinPanel;

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("copyAddressAction.tooltip")));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("copyAddressAction.mnemonicKey"));
    }

    /**
     * Copy receive address to clipboard
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // copy receive address to clipboard
        TextTransfer textTransfer = new TextTransfer();
        textTransfer.setClipboardContents(receiveWorldcoinPanel.getReceiveAddress());
    }
}