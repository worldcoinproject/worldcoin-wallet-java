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
import org.junit.Before;
import org.junit.Test;
import org.wallet.ApplicationDataDirectoryLocator;
import org.wallet.Constants;
import org.wallet.CreateControllers;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.file.FileHandler;
import org.wallet.network.WorldcoinWalletService;
import org.wallet.viewsystem.simple.SimpleViewSystem;
import org.wallet.viewsystem.swing.view.components.FontSizer;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinConfirmPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SendWorldcoinNowActionTest extends TestCase {

    private static final String EXPECTED_ENTER_THE_WALLET_PASSWORD = "Enter the wallet password";
    private static final String EXPECTED_TEST_SEND_FAILED_ERROR = " test - send failed";
    private static final String EXPECTED_SEND_FAILED = "The send of your worldcoin failed.";
    private static final String EXPECTED_THE_WALLET_IS_BUSY = "The wallet is busy with the task \"\"";
    private static final String EXPECTED_YOUR_WORLDCOIN_WERE_SENT_SUCCESSFULLY = "Your worldcoin were sent successfully.";
    private static final String EXPECTED_SENDING_WORLDCOIN = "Sending worldcoin...";
    public static final CharSequence TEST_PASSWORD1 = "my hovercraft has eels";
    public static final CharSequence WALLET_PASSWORD = "testing testing 123";
    private static final int DELAY_TO_COMPLETE_OPERATION = 12000; // milliseconds
    private static final int DELAY_TO_UPDATE_MESSAGES = 4000; // milliseconds
    
    private File worldcoinWalletDirectory;

    private static final Logger log = LoggerFactory.getLogger(SendWorldcoinNowActionTest.class);

    private WorldcoinController controller;

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

            // Add the simple view system (no Swing).
            SimpleViewSystem simpleViewSystem = new SimpleViewSystem();
            controllers.coreController.registerViewSystem(simpleViewSystem);

            // WorldcoinWallet runtime is now setup and running
            // Wait for a peer connection.
            log.debug("Waiting for peer connection. . . ");
            while (!simpleViewSystem.isOnline()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("Now online.");

            // Wait a little longer to get a second connection.
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    @Test
    public void testSendWorldcoinWithNonEncryptedWallet() throws Exception {
        // Get the system property runFunctionalTest to see if the functional
        // tests need running.
        String runFunctionalTests = System.getProperty(Constants.RUN_FUNCTIONAL_TESTS_PARAMETER);
        if (Boolean.TRUE.toString().equalsIgnoreCase(runFunctionalTests)) {

            // Create a new unencrypted wallet and put it in the model as the
            // active wallet.
            ActionTestUtils.createNewActiveWallet(controller, "testAddReceivingAddressesWithNonEncryptedWallet", false, null);

            // Create a new SendWorldcoinNowSubmitAction to test.
            FontSizer.INSTANCE.initialise(controller);
            SendWorldcoinConfirmPanel sendWorldcoinConfirmPanel = new SendWorldcoinConfirmPanel(controller, null, null, null);
            SendWorldcoinNowAction sendWorldcoinNowAction = sendWorldcoinConfirmPanel.getSendWorldcoinNowAction();

            assertNotNull("sendWorldcoinNowAction was not created successfully", sendWorldcoinNowAction);
            assertTrue("WorldcoinWallet password was enabled when it should not be", !sendWorldcoinConfirmPanel.isWalletPasswordFieldEnabled());

            // Set the action up to use test parameters and succeed-on-send.
            sendWorldcoinNowAction.setTestParameters(true, true);

            // Execute - this should give the sending message or sent message.
            sendWorldcoinNowAction.actionPerformed(null);

            // Wait a while and the message should be that it has completed the
            // send.
            Thread.sleep(DELAY_TO_COMPLETE_OPERATION);

            // Worldcoins should now be sent
            assertEquals("Wrong message - expecting success on messageText1", EXPECTED_YOUR_WORLDCOIN_WERE_SENT_SUCCESSFULLY,
                    sendWorldcoinConfirmPanel.getMessageText1());
            assertEquals("Wrong message - expecting success on messageText2", "", sendWorldcoinConfirmPanel.getMessageText2().trim());

            // Set the action up to use test parameters and fail-on-send.
            sendWorldcoinNowAction.setTestParameters(true, false);

            // Wait for peer connections
            Thread.sleep(6000);

            // Execute - this should give the sending or failed message.
            sendWorldcoinNowAction.actionPerformed(null);
            Thread.sleep(DELAY_TO_UPDATE_MESSAGES);
            assertTrue(
                    "Wrong message - expecting sending/sent on messageText1 was '" + sendWorldcoinConfirmPanel.getMessageText1()
                    + "'",
                    "".equals(sendWorldcoinConfirmPanel.getMessageText1().trim())
                    || EXPECTED_SENDING_WORLDCOIN.equals(sendWorldcoinConfirmPanel.getMessageText1())
                    || EXPECTED_SEND_FAILED.equals(sendWorldcoinConfirmPanel.getMessageText1()));
            assertEquals("Wrong message - expecting sending on messageText2", EXPECTED_TEST_SEND_FAILED_ERROR,
                    sendWorldcoinConfirmPanel.getMessageText2());

            // Wait a while and the message should be that it has failed the send.
            Thread.sleep(DELAY_TO_COMPLETE_OPERATION);

            assertEquals("Wrong message - expecting success on messageText1", EXPECTED_SEND_FAILED,
                    sendWorldcoinConfirmPanel.getMessageText1());
            assertEquals("Wrong message - expecting success on messageText2", EXPECTED_TEST_SEND_FAILED_ERROR,
                    sendWorldcoinConfirmPanel.getMessageText2());
        }
    }

    @Test
    public void testSendWorldcoinWithEncryptedWallet() throws Exception {
        // Get the system property runFunctionalTest to see if the functional
        // tests need running.
        String runFunctionalTests = System.getProperty(Constants.RUN_FUNCTIONAL_TESTS_PARAMETER);
        if (Boolean.TRUE.toString().equalsIgnoreCase(runFunctionalTests)) {

            // Create a new encrypted wallet and put it in the model as the
            // active wallet.
            ActionTestUtils.createNewActiveWallet(controller, "testAddReceivingAddressesWithNonEncryptedWallet", true,
                    WALLET_PASSWORD);

            // Create a new SendWorldcoinNowSubmitAction to test.
            FontSizer.INSTANCE.initialise(controller);
            SendWorldcoinConfirmPanel sendWorldcoinConfirmPanel = new SendWorldcoinConfirmPanel(controller, null, null, null);
            SendWorldcoinNowAction sendWorldcoinNowAction = sendWorldcoinConfirmPanel.getSendWorldcoinNowAction();

            assertNotNull("sendWorldcoinNowAction was not created successfully", sendWorldcoinNowAction);
            assertTrue("WorldcoinWallet password was disabled when it should not be", sendWorldcoinConfirmPanel.isWalletPasswordFieldEnabled());

            // Set the action up to use test parameters and succeed-on-send.
            sendWorldcoinNowAction.setTestParameters(true, true);

            // Wait for peer connections
            Thread.sleep(6000);

            // Execute - this should complain that the wallet password is not
            // set.
            sendWorldcoinNowAction.actionPerformed(null);
            // Wait a while and the message should be that it has completed the
            // send.
            Thread.sleep(DELAY_TO_UPDATE_MESSAGES);

            assertTrue(
                    "Wrong message - expecting no wallet password on messageText1, was '"
                    + sendWorldcoinConfirmPanel.getMessageText1() + "'",
                    EXPECTED_ENTER_THE_WALLET_PASSWORD.equals(sendWorldcoinConfirmPanel.getMessageText1()));

            // Set the wallet password.
            sendWorldcoinConfirmPanel.setWalletPassword(WALLET_PASSWORD);

            // Execute
            sendWorldcoinNowAction.actionPerformed(null);

            // Wait a while and the message should be that it has completed the
            // send.
            Thread.sleep(DELAY_TO_COMPLETE_OPERATION);

            // Worldcoins should now be sent
            assertEquals("Wrong message - expecting success on messageText1, sendWorldcoinConfirmPanel = " + System.identityHashCode(sendWorldcoinConfirmPanel), EXPECTED_YOUR_WORLDCOIN_WERE_SENT_SUCCESSFULLY,
                    sendWorldcoinConfirmPanel.getMessageText1());
            assertEquals("Wrong message - expecting success on messageText2", "", sendWorldcoinConfirmPanel.getMessageText2().trim());

            // Set the action up to use test parameters and fail-on-send.
            sendWorldcoinNowAction.setTestParameters(true, false);

            // Execute - this should complain that the wallet password is not
            // set.
            sendWorldcoinNowAction.actionPerformed(null);
            // Wait a while and the message should be that it has completed the
            // send.
            Thread.sleep(DELAY_TO_UPDATE_MESSAGES);

            assertTrue(
                    "Wrong message - expecting no wallet password on messageText1, was '"
                    + sendWorldcoinConfirmPanel.getMessageText1() + "'",
                    EXPECTED_ENTER_THE_WALLET_PASSWORD.equals(sendWorldcoinConfirmPanel.getMessageText1()));

            // Set the wallet password.
            sendWorldcoinConfirmPanel.setWalletPassword(WALLET_PASSWORD);

            // Execute - this should give the sending or failed message.
            sendWorldcoinNowAction.actionPerformed(null);
            assertTrue(
                    "Wrong message - expecting sending/failed/ wallet busy on messageText1 was '" + sendWorldcoinConfirmPanel.getMessageText1()
                    + "'",
                    "".equals(sendWorldcoinConfirmPanel.getMessageText1().trim())
                    || EXPECTED_SENDING_WORLDCOIN.equals(sendWorldcoinConfirmPanel.getMessageText1())
                    || EXPECTED_SEND_FAILED.equals(sendWorldcoinConfirmPanel.getMessageText1())
                    || EXPECTED_THE_WALLET_IS_BUSY.equals(sendWorldcoinConfirmPanel.getMessageText1()));
            assertTrue(
                    "Wrong message - expecting blank/errormessage on messageText2 was '"
                    + sendWorldcoinConfirmPanel.getMessageText2() + "'",
                    "".equals(sendWorldcoinConfirmPanel.getMessageText2().trim())
                    || EXPECTED_TEST_SEND_FAILED_ERROR.equals(sendWorldcoinConfirmPanel.getMessageText2())
                    || EXPECTED_SEND_FAILED.equals(sendWorldcoinConfirmPanel.getMessageText1()));

            // Wait a while and the message should be that it has failed the send.
            Thread.sleep(DELAY_TO_COMPLETE_OPERATION);

            assertEquals("Wrong message - expecting success on messageText1", EXPECTED_SEND_FAILED,
                    sendWorldcoinConfirmPanel.getMessageText1());
            assertEquals("Wrong message - expecting success on messageText2", EXPECTED_TEST_SEND_FAILED_ERROR,
                    sendWorldcoinConfirmPanel.getMessageText2());
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
