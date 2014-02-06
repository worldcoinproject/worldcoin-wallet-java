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
package org.wallet.file;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import junit.framework.TestCase;

import org.junit.Test;
import org.wallet.Constants;
import org.wallet.CreateControllers;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.exchange.ExchangeModel;
import org.wallet.viewsystem.swing.action.ExportTransactionsSubmitAction;

import com.google.worldcoin.core.Wallet;

public class ExportTransactionsSubmitActionTest extends TestCase {
    public static final String WALLETS_TESTDATA_DIRECTORY = "wallets";

    public static final String TEST_EXPORT_PREFIX = "testExport";

    public static final String PROTOBUF1_WALLET_FILE = "protobuf1.wallet";

    @Test
    public void testExportTransactions() throws Exception {
        // Create WorldcoinWallet controller.
        final CreateControllers.Controllers controllers = CreateControllers.createControllers();
        final WorldcoinController worldcoinController = controllers.worldcoinController;

        // Initialise currency converter.
        worldcoinController.getModel().setUserPreference(ExchangeModel.TICKER_FIRST_ROW_CURRENCY, "EUR");
        CurrencyConverter.INSTANCE.initialise(worldcoinController);
        CurrencyConverter.INSTANCE.setRate(BigDecimal.valueOf(10.0));

        ExportTransactionsSubmitAction action = new ExportTransactionsSubmitAction(worldcoinController, null);
        assertNotNull(action);

        // Load up the test wallet /wallets/protobuf1.
        // This has (at least) two transactions in it.
        File directory = new File(".");
        String currentPath = directory.getAbsolutePath();

        String testDirectory = currentPath + File.separator + Constants.TESTDATA_DIRECTORY + File.separator
                + WALLETS_TESTDATA_DIRECTORY;
        String testWalletFile = testDirectory + File.separator + PROTOBUF1_WALLET_FILE;

        // Load up the test wallet
        FileHandler fileHandler = new FileHandler(worldcoinController);
        Wallet testWallet = fileHandler.loadFromFile(new File(testWalletFile)).getWallet();

        assertNotNull(testWallet);
        assertTrue("Wrong number of transactions in wallet", testWallet.getTransactions(true).size() >= 2);

        // Write the transactions out to a file.
        File exportFile = File.createTempFile(TEST_EXPORT_PREFIX, ".csv");
        exportFile.delete();
        exportFile.deleteOnExit();

        // Check the file does not exist initially.
        assertTrue("Export file exists when it should not", !exportFile.exists());

        String exportTransactionsFilename = exportFile.getAbsolutePath();

        WalletData walletData = new WalletData();
        walletData.setWallet(testWallet);
        walletData.setWalletFilename(testWalletFile);

        action.exportTransactionsDoIt(walletData, exportTransactionsFilename);

        // Check the file does exist now.
        assertTrue("Export file does not exists when it should", exportFile.exists());

        FileInputStream fileInputStream = new FileInputStream(exportTransactionsFilename);
        DataInputStream dataInputStream = new DataInputStream(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream, "UTF-8"));

        // Read file line by line.
        String line0 = bufferedReader.readLine();
        String line1 = bufferedReader.readLine();
        String line2 = bufferedReader.readLine();

        // Close the input stream.
        dataInputStream.close();

        assertNotNull("No header row read in", line0);
        assertNotNull("No row 1 read in", line1);
        assertNotNull("No row 2 read in", line2);
        
        assertEquals("Header row incorrect", "Date,Description,Amount (WDC),Amount (\u20AC),Transaction Id", line0);

        // Note \u5317\u4EAC = Beijing in Chinese.
        String row1Expected =  "29 Jul 2013 10:23,\"Sent to \"\"unencrypted-1-\u5317\u4EAC\"\" (1CQH7Hp9nNQVDcKtFVwbA8tqPMNWDBvqE3)\",-0.015,-0.15,28916ed8592a4cf216d8eac7e5ccb5a08771f439e508ec2861b7ff612e15b827";
        String row2Expected = "29 Jul 2013 10:00,\"Received with \"\"protobuf 1.1.\u5317\u4EAC\"\" (1GtMdodCNN5ewFcEUxxVBziBrLtQzSuZvq)\",0.015,0.15,5eeabb42d0522c40cc63dace7746d5f82cd51292bc50a38c4dd68a854ec6cd77";
        assertEquals("Row 1 incorrect", row1Expected, line1);
        assertEquals("Row 2 incorrect", row2Expected, line2);
    }
}
