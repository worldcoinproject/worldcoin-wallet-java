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

import java.awt.Cursor;
import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.dialogs.DeleteSendingAddressConfirmDialog;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinPanel;

/**
 * This {@link Action} show the delete sending address confirmation dialog.
 */
public class DeleteSendingAddressAction extends WorldcoinWalletSubmitAction {
    private static final long serialVersionUID = 1333933460523457765L;

    private WorldcoinWalletFrame mainFrame;
    private SendWorldcoinPanel sendWorldcoinPanel;

    /**
     * Creates a new {@link DeleteSendingAddressAction}.
     */
    public DeleteSendingAddressAction(WorldcoinController worldcoinController, WorldcoinWalletFrame mainFrame, SendWorldcoinPanel sendWorldcoinPanel) {
        super(worldcoinController, "deleteSendingAddressSubmitAction.text", "deleteSendingAddressSubmitAction.tooltip", "deleteSendingAddressSubmitAction.mnemonic", null);
        this.mainFrame = mainFrame;
        this.sendWorldcoinPanel = sendWorldcoinPanel;
    }

    /**
     * Prompt for deletion of a sending address.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }
        mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setEnabled(false);
  
        try {
            DeleteSendingAddressConfirmDialog deleteSendingAddressConfirmDialog = new DeleteSendingAddressConfirmDialog(super.worldcoinController, mainFrame, sendWorldcoinPanel);
            deleteSendingAddressConfirmDialog.setVisible(true);
        } finally {
            setEnabled(true);
            mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
}
