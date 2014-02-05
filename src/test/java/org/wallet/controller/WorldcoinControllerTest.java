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
package org.wallet.controller;

import junit.framework.TestCase;
import org.junit.Test;
import org.wallet.CreateControllers;
import org.wallet.CreateControllers.Controllers;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.viewsystem.swing.action.ActionTestUtils;

public class WorldcoinControllerTest extends TestCase {
    public static final int MAXIMUM_BUSY_TIME = 5000;
    public static final int SLEEP_TIME = 500;

    @Test
    public void testWalletBusyListener() throws Exception {       
        // Create WorldcoinWallet controller
        Controllers controllers = CreateControllers.createControllers();
        WorldcoinController controller = controllers.worldcoinController;
        
        // Create a new wallet and put it in the model as the active wallet.
        ActionTestUtils.createNewActiveWallet(controller, "testWalletBusyListener", false, null);

        SimpleWalletBusyListener testWalletBusyListener = new SimpleWalletBusyListener();
        
        controller.registerWalletBusyListener(testWalletBusyListener);
        
        assertTrue("WorldcoinWallet busy not yet seen", !testWalletBusyListener.isWalletBusy());
        
        // Set the wallet to be busy
        controller.fireWalletBusyChange(true);

        assertTrue("WorldcoinWallet is now busy", testWalletBusyListener.isWalletBusy());

        // Set the wallet to no longer be busy
        controller.fireWalletBusyChange(false);

        assertTrue("WorldcoinWallet is no longer busy", !testWalletBusyListener.isWalletBusy());
    }
    
    public static void waitForWalletNotBusy(SimpleWalletBusyListener walletBusyListener) {
        int timeWaited = 0;
        while (walletBusyListener.isWalletBusy()) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                fail("WorldcoinWallet waiting was interrupted.");
            }
            timeWaited += SLEEP_TIME;
            if (timeWaited > MAXIMUM_BUSY_TIME) {
                fail("It took too long to for the wallet to stop being busy.");
            }
        }
    }
}
