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

import junit.framework.TestCase;

import org.junit.Test;

import org.wallet.CreateControllers;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.model.worldcoin.WalletAddressBookData;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.viewsystem.swing.ColorAndFontConstants;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinPanel;
import org.wallet.viewsystem.swing.view.components.FontSizer;

public class DeleteSendingAddressActionTest extends TestCase {
    private static final String LABEL1 = "example 1";
    private static final String ADDRESS1 = "129mVqKUmJ9uwPxKJBnNdABbuaaNfho4Ha";
       
    private static final String LABEL2 = "example 2";
    private static final String ADDRESS2 = "12fJtQZZfFsyKFwaeeNV1Nvg8JftVbXE4E";
       
    @Test
    public void testDeleteSendingAddress() throws Exception {
        final CreateControllers.Controllers controllers = CreateControllers.createControllers();
        WorldcoinController controller = controllers.worldcoinController;
        
        // Create a new wallet and put it in the model as the active wallet.
        ActionTestUtils.createNewActiveWallet(controller, "testDeleteSendingAddress", false, null);

        ColorAndFontConstants.init();
        FontSizer.INSTANCE.initialise(controller);
        SendWorldcoinPanel sendWorldcoinPanel = new SendWorldcoinPanel(controller, null);
        DeleteSendingAddressSubmitAction deleteSubmitAction = new DeleteSendingAddressSubmitAction(controller, sendWorldcoinPanel, null);

        assertNotNull("deleteSubmitAction was not created successfully", deleteSubmitAction);
        assertEquals("Wrong number of send addresses at beginning", 0, sendWorldcoinPanel.getAddressesTableModel().getRowCount());
       
        // Execute the deleteSubmitAction - there are no sending addresses so nothing should happen.
        deleteSubmitAction.actionPerformed(null);
        assertEquals("Wrong number of send addresses after initial delete", 0, sendWorldcoinPanel.getAddressesTableModel().getRowCount());
        
        // Create two sending addresses.
        WalletData perWalletModelData = controller.getModel().getActivePerWalletModelData();
        perWalletModelData.getWalletInfo().addSendingAddress(new WalletAddressBookData(LABEL1, ADDRESS1));
        perWalletModelData.getWalletInfo().addSendingAddress(new WalletAddressBookData(LABEL2, ADDRESS2));

        assertEquals("Wrong number of send addresses on wallet model after adding new send addresses", 2, perWalletModelData.getWalletInfo().getSendingAddresses().size());
        assertEquals("Wrong number of send addresses on table after adding new send addresses", 2, sendWorldcoinPanel.getAddressesTableModel().getRowCount());

        sendWorldcoinPanel.getAddressesTableModel().fireTableStructureChanged();

        // Delete the second sending address - the first should remain and be selected.
        sendWorldcoinPanel.getAddressesTable().setRowSelectionInterval(1,1);
        deleteSubmitAction.actionPerformed(null);
        assertEquals("Wrong number of send addresses on wallet model after deleting send address", 1, perWalletModelData.getWalletInfo().getSendingAddresses().size());
        assertEquals("Wrong number of send addresses on table after deleting send address", 1, sendWorldcoinPanel.getAddressesTableModel().getRowCount());
        assertEquals("Wrong send address deleted", LABEL1, (String)sendWorldcoinPanel.getAddressesTableModel().getValueAt(0, 0));
    }
}
