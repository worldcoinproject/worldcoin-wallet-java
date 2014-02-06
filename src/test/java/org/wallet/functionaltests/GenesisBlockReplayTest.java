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
import org.junit.Test;
import org.wallet.ApplicationDataDirectoryLocator;
import org.wallet.Constants;
import org.wallet.CreateControllers;
import org.wallet.file.FileHandler;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.network.WorldcoinWalletService;
import org.wallet.network.ReplayManager;
import org.wallet.network.ReplayTask;
import org.wallet.viewsystem.simple.SimpleViewSystem;
import org.wallet.viewsystem.swing.action.ActionTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Functional test to check that replay from the genesis block works ok.
 * 
 * See bug report: https://github.com/jim618/wallet/issues/21
 * 
 * @author jim
 * 
 */
public class GenesisBlockReplayTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(GenesisBlockReplayTest.class);

    private SimpleDateFormat formatter;

    @Test
    public void testReplayFromGenesisBlock() throws Exception {

        // Get the system property runFunctionalTest to see if the functional tests need running.
        String runFunctionalTests = System.getProperty(Constants.RUN_FUNCTIONAL_TESTS_PARAMETER);
        if (Boolean.TRUE.toString().equalsIgnoreCase(runFunctionalTests)) {

            // Date format is UTC with century, T time separator and Z for UTC timezone.
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            File worldcoinWalletDirectory = createWorldcoinWalletRuntime();

            // Set the application data directory to be the one we just created.
            ApplicationDataDirectoryLocator applicationDataDirectoryLocator = new ApplicationDataDirectoryLocator(worldcoinWalletDirectory);

            // Create WorldcoinWallet controller.
            final CreateControllers.Controllers controllers = CreateControllers.createControllers(applicationDataDirectoryLocator);

            log.debug("Creating Worldcoin service");
            // Create the WorldcoinWalletService that connects to the worldcoin network.
            WorldcoinWalletService worldcoinWalletService = new WorldcoinWalletService(controllers.worldcoinController);
            controllers.worldcoinController.setWorldcoinWalletService(worldcoinWalletService);

            // Add the simple view system (no Swing).
            SimpleViewSystem simpleViewSystem = new SimpleViewSystem();
            controllers.coreController.registerViewSystem(simpleViewSystem);
            
            ReplayManager.INSTANCE.initialise(controllers.worldcoinController, true);

            //
            // WorldcoinWallet runtime is now setup and running.
            //

            // Wait for a peer connection.
            log.debug("Waiting for peer connection. . . ");
            while (!simpleViewSystem.isOnline()) {
                Thread.sleep(1000);
            }
            log.debug("Now online.");

            // Create a new  wallet and put it in the model as the active wallet.
            ActionTestUtils.createNewActiveWallet(controllers.worldcoinController, "testReplayFromGenesisBlock", false,
                    null);

            log.debug("Replaying blockchain from genesis block");
            List<WalletData> perWalletModelDataList = new ArrayList<WalletData>();
            perWalletModelDataList.add(controllers.worldcoinController.getModel().getActivePerWalletModelData());
            ReplayTask replayTask = new ReplayTask(perWalletModelDataList, null, 0);
            ReplayManager.INSTANCE.offerReplayTask(replayTask);

            // Run for a minute.
            log.debug("Twiddling thumbs for 60 seconds ...");
            Thread.sleep(60000);
            log.debug("... 60 seconds later later.");

            // Check the blockstore has added the downloaded blocks.
            assertNotNull("No worldcoinWalletService after replay", worldcoinWalletService);
            assertNotNull("No blockStore after replay",  worldcoinWalletService.getBlockStore());
            //assertNotNull("No blockStore file after replay",  worldcoinWalletService.getBlockStore().getFile());
            //assertTrue("Block size is too short", BLOCKSIZE_AFTER_REPLAY <=  worldcoinWalletService.getBlockStore().getFile().length());

            // Tidy up.
            try {
                if (worldcoinWalletService.getPeerGroup() != null) {
                    worldcoinWalletService.getPeerGroup().stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.debug("Not running functional test: GenesisBlockReplayTest#testReplayFromGenesisBlock. Add '-DrunFunctionalTests=true' to run");
        }
    }

    /**
     * Create a working, portable runtime of WorldcoinWallet in a temporary directory.
     * 
     * @return the temporary directory the wallet runtime has been created in
     */
    private File createWorldcoinWalletRuntime() throws IOException {
        File worldcoinWalletDirectory = FileHandler.createTempDirectory("wallet");
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
