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
package org.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Main WorldcoinWalletInExecutableJar entry class for when running in an executable jar - put console
 * output to a file
 */
public final class WorldcoinWalletInExecutableJar {

    public static final String OUTPUT_DIRECTORY = "log";
    public static final String CONSOLE_OUTPUT_FILENAME = "wallet.log";

    private static Logger log = LoggerFactory.getLogger(WorldcoinWalletInExecutableJar.class);

    /**
     * Utility class should not have a public constructor
     */
    private WorldcoinWalletInExecutableJar() {
    }

    /**
     * Start wallet user interface when running in a jar.
     * This will adjust the logging framework output to ensure that console output is sent
     * to a file appender in the client.
     * @param args The optional command line arguments ([0] can be a Worldcoin URI
     */
    public static void main(String args[]) {
        // Redirect the console output to a file.
        PrintStream fileStream;
        try {
            // Get the current data directory
            ApplicationDataDirectoryLocator applicationDataDirectoryLocator = new ApplicationDataDirectoryLocator();
            
            String outputDirectory;
            String consoleOutputFilename;
            
            if ("".equals(applicationDataDirectoryLocator.getApplicationDataDirectory())) {
                // Use defaults
                outputDirectory = OUTPUT_DIRECTORY;
                consoleOutputFilename = OUTPUT_DIRECTORY + File.separator + CONSOLE_OUTPUT_FILENAME;
            } else {
                // Use defined data directory as the root
                outputDirectory = applicationDataDirectoryLocator.getApplicationDataDirectory() + File.separator
                        + OUTPUT_DIRECTORY;
                consoleOutputFilename = applicationDataDirectoryLocator.getApplicationDataDirectory() + File.separator
                        + OUTPUT_DIRECTORY + File.separator + CONSOLE_OUTPUT_FILENAME;
            }
            
            log = LoggerFactory.getLogger(WorldcoinWalletInExecutableJar.class);

            // create output directory
            (new File(outputDirectory)).mkdir();

            // create output console log
            (new File(consoleOutputFilename)).createNewFile();

            fileStream = new PrintStream(new FileOutputStream(consoleOutputFilename, true));

            if (fileStream != null) {
                // Redirecting console output to file
                System.setOut(fileStream);
                // Redirecting runtime exceptions to file
                System.setErr(fileStream);
            }
        } catch (FileNotFoundException e) {
            if (log != null) {
                log.error("Error in IO Redirection", e);
            }
        } catch (Exception e) {
            // Gets printed in the file.
            if (log != null) {
                log.debug("Error in redirecting output & exceptions to file", e);
            }
        } finally {
            // call the main WorldcoinWalletInExecutableJar code
            WorldcoinWallet.main(args);
        }
    }
}
