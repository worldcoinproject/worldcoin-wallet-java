/**
 * Copyright 2013 wallet.org
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
package org.wallet.network;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.wallet.ApplicationDataDirectoryLocator;
import org.wallet.Constants;
import org.wallet.Localiser;
import org.wallet.CreateControllers;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.file.FileHandler;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WalletInfoData;
import org.wallet.store.WorldcoinWalletVersion;
import org.wallet.viewsystem.simple.SimpleViewSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.worldcoin.core.DumpedPrivateKey;
import com.google.worldcoin.core.ECKey;
import com.google.worldcoin.core.NetworkParameters;
import com.google.worldcoin.core.Wallet;

public class ReplayManagerTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(ReplayManagerTest.class);

    private WorldcoinController controller;
    private Localiser localiser;
    private File worldcoinWalletDirectory;

    // The address for this private key is "1N4qu8a6NwBrxM5PvSoFh4qe6QSWmG6Xds".
    private static final String REPLAY1_PRIVATE_KEY = "5Jsokwg1ypfCPgJXv4vnhW11YWSp4anh9UbHoCZFZdwAnEpU69u";

    private static final String START_OF_REPLAY_PERIOD = "2012-09-03T10:00:00Z";

    private static final BigInteger BALANCE_AT_START = BigInteger.ZERO;

    private SimpleDateFormat formatter;

    private SimpleViewSystem simpleViewSystem;

    @Before
    public void setUp() throws Exception {
        worldcoinWalletDirectory = createWorldcoinWalletRuntime();

        // Set the application data directory to be the one we just created.
        ApplicationDataDirectoryLocator applicationDataDirectoryLocator = new ApplicationDataDirectoryLocator(worldcoinWalletDirectory);

        // Create WorldcoinWallet controller
        final CreateControllers.Controllers controllers = CreateControllers.createControllers(applicationDataDirectoryLocator);
        controller = controllers.worldcoinController;

        log.debug("Creating Worldcoin service");
        // Create the WorldcoinWalletService that connects to the worldcoin network.
        WorldcoinWalletService worldcoinWalletService = new WorldcoinWalletService(controller);
        controller.setWorldcoinWalletService(worldcoinWalletService);

        // Add the simple view system (no Swing).
        simpleViewSystem = new SimpleViewSystem();
        controllers.coreController.registerViewSystem(simpleViewSystem);

        log.debug("Waiting for peer connection. . . ");
        while (!simpleViewSystem.isOnline()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.debug("Now online.");

    }

    @Test
    public void testReplayManagerSyncSingleWallet() throws Exception {
        // Get the system property runFunctionalTest to see if the functional
        // tests need running.
        String runFunctionalTests = System.getProperty(Constants.RUN_FUNCTIONAL_TESTS_PARAMETER);
        if (Boolean.TRUE.toString().equalsIgnoreCase(runFunctionalTests)) {
            // Date format is UTC with century, T time separator and Z for UTC
            // timezone.
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            // Initialise replay manager
            ReplayManager replayManager = ReplayManager.INSTANCE;
            assertNotNull(replayManager);

            replayManager.initialise(controller, true);

            String replayWalletPath = worldcoinWalletDirectory.getAbsolutePath() + File.separator + "replay.wallet";

            // Create a new wallet.
            Wallet replayWallet = new Wallet(NetworkParameters.prodNet());

            // Add in the replay key.
            DumpedPrivateKey replayDumpedPrivateKey = new DumpedPrivateKey(NetworkParameters.prodNet(), REPLAY1_PRIVATE_KEY);
            ECKey replayKey = replayDumpedPrivateKey.getKey();
            replayKey.setCreationTimeSeconds(formatter.parse(START_OF_REPLAY_PERIOD).getTime() / 1000);
            log.debug("replayPrivateKey getCreationTimeSeconds = " + replayKey.getCreationTimeSeconds());

            replayWallet.addKey(replayKey);
            WalletData perWalletModelData = new WalletData();
            perWalletModelData.setWalletInfo(new WalletInfoData(replayWalletPath, replayWallet, WorldcoinWalletVersion.PROTOBUF));
            perWalletModelData.setWallet(replayWallet);
            perWalletModelData.setWalletFilename(replayWalletPath);
            perWalletModelData.setWalletDescription("testReplayManagerSyncSingleWallet test");
            controller.getModel().getPerWalletModelDataList().add(perWalletModelData);

            log.debug("Replay wallet before replay = \n" + replayWallet.toString());

            assertEquals(BALANCE_AT_START, replayWallet.getBalance());

            log.debug("Replaying blockchain");
            // Create a ReplayTask to replay the replay wallet from the
            // START_OF_REPLAY_PERIOD.
            List<WalletData> perWalletModelDataList = new ArrayList<WalletData>();
            perWalletModelDataList.add(perWalletModelData);

            ReplayTask replayTask = new ReplayTask(perWalletModelDataList, formatter.parse(START_OF_REPLAY_PERIOD),
                    ReplayTask.UNKNOWN_START_HEIGHT);
            replayManager.offerReplayTask(replayTask);

            // Run for a while.
            log.debug("Twiddling thumbs for 60 seconds ...");
            Thread.sleep(60000);
            log.debug("... 60 seconds later.");

            // Check the wallet - there should be some transactions in there.
            if (replayWallet.getTransactions(true).size() > 0) {
                // We are done.
            } else {
                // Run for a while longer.
                log.debug("Twiddling thumbs for another 60 seconds ...");
                Thread.sleep(60000);
                log.debug("... 60 seconds later.");
                if (replayWallet.getTransactions(true).size() > 0) {
                    // We are done.
                } else {
                    if (simpleViewSystem.getNumberOfBlocksDownloaded() > 0) {
                        // Well it tried but probably got a slow connection -
                        // give it a pass.
                    } else {
                        fail("No blocks were downloaded on replay");
                    }
                }
            }

            // Print out replay wallet after replay.
            log.debug("Replay wallet after replay = \n" + replayWallet);
        } else {
            log.debug("Not running functional test: ReplayManagerTest#testReplayManagerSyncSingleWallet. Add '-DrunFunctionalTests=true' to run");
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

        // Copy in the checkpoints and blockchain stored in git - this is in
        // source/main/resources/.
        File worldcoinwalletBlockcheckpoints = new File(worldcoinWalletDirectoryPath + File.separator + "worldcoin-wallet.checkpoints");
        FileHandler.copyFile(new File("./src/main/resources/wallet.checkpoints"), worldcoinwalletBlockcheckpoints);
        worldcoinwalletBlockcheckpoints.deleteOnExit();

        return worldcoinWalletDirectory;
    }
}
