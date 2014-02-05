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
package org.wallet.functionaltests;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.wallet.ApplicationDataDirectoryLocator;
import org.wallet.Constants;
import org.wallet.CreateControllers;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.file.FileHandler;
import org.wallet.network.WorldcoinWalletService;
import org.wallet.viewsystem.simple.SimpleViewSystem;
import org.wallet.viewsystem.swing.action.CreateWalletSubmitAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * functional test to check that wallets can be created and deleted ok
 * 
 * @author jim
 * 
 */
public class CreateAndDeleteWalletsTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(CreateAndDeleteWalletsTest.class);

    private static File worldcoinWalletDirectory;

    private static WorldcoinController controller;

    private static SimpleViewSystem simpleViewSystem;

    @Before
    @Override
    public void setUp() throws IOException {
        // Get the system property runFunctionalTest to see if the functional
        // tests need running.
        String runFunctionalTests = System.getProperty(Constants.RUN_FUNCTIONAL_TESTS_PARAMETER);
        if (Boolean.TRUE.toString().equalsIgnoreCase(runFunctionalTests)) {

            worldcoinWalletDirectory = createWorldcoinWalletRuntime();

            // set the application data directory to be the one we just created
            ApplicationDataDirectoryLocator applicationDataDirectoryLocator = new ApplicationDataDirectoryLocator(worldcoinWalletDirectory);

            // Create WorldcoinWallet controller.
            final CreateControllers.Controllers controllers = CreateControllers.createControllers(applicationDataDirectoryLocator);
            controller = controllers.worldcoinController;

            log.debug("Creating Worldcoin service");
            // create the WorldcoinWalletService that connects to the worldcoin network
            WorldcoinWalletService worldcoinWalletService = new WorldcoinWalletService(controller);
            controller.setWorldcoinWalletService(worldcoinWalletService);

            // add the simple view system (no Swing)
            simpleViewSystem = new SimpleViewSystem();
            controllers.coreController.registerViewSystem(simpleViewSystem);

            // WorldcoinWallet runtime is now setup and running
            // Wait a little while to get two connections.
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCreateAndDeleteWalletsWithActions() throws Exception {
        // Get the system property runFunctionalTest to see if the functional
        // tests need running.
        String runFunctionalTests = System.getProperty(Constants.RUN_FUNCTIONAL_TESTS_PARAMETER);
        if (Boolean.TRUE.toString().equalsIgnoreCase(runFunctionalTests)) {

            String test1WalletPath = worldcoinWalletDirectory.getAbsolutePath() + File.separator + "actionTest1.wallet";
            String test2WalletPath = worldcoinWalletDirectory.getAbsolutePath() + File.separator + "actionTest2.wallet";

            // initially there is a blank WalletData
            assertEquals(1, controller.getModel().getPerWalletModelDataList().size());

            // create test1 wallet
            CreateWalletSubmitAction createNewWalletAction = new CreateWalletSubmitAction(controller, null, null);
            createNewWalletAction.createNewWallet(test1WalletPath);
            Thread.sleep(4000);
            assertEquals(1, controller.getModel().getPerWalletModelDataList().size());

            // create test2 wallet
            createNewWalletAction.createNewWallet(test2WalletPath);
            Thread.sleep(4000);
            assertEquals(2, controller.getModel().getPerWalletModelDataList().size());
        }
    }

    /**
     * Create a working, portable runtime of WorldcoinWallet in a temporary directory.
     * 
     * @return the temporary directory the wallet runtime has been created in
     */
    private File createWorldcoinWalletRuntime() throws IOException {
        File worldcoinWalletDirectory = FileHandler.createTempDirectory("CreateAndDeleteWalletsTest");
        String worldcoinWalletDirectoryPath = worldcoinWalletDirectory.getAbsolutePath();

        System.out.println("Building WorldcoinWallet runtime in : " + worldcoinWalletDirectory.getAbsolutePath());

        // Create an empty wallet.properties.
        File worldcoinwalletProperties = new File(worldcoinWalletDirectoryPath + File.separator + "worldcoin-wallet.properties");
        worldcoinwalletProperties.createNewFile();
        worldcoinwalletProperties.deleteOnExit();

        // Copy in the checkpoints stored in git - this is in source/main/resources/.
        File worldcoinwalletCheckpoints = new File(worldcoinWalletDirectoryPath + File.separator + "worldcoin-wallet.checkpoints");
        FileHandler.copyFile(new File("./src/main/resources/wallet.checkpoints"), worldcoinwalletCheckpoints);
        worldcoinwalletCheckpoints.deleteOnExit();

        return worldcoinWalletDirectory;
    }
}
