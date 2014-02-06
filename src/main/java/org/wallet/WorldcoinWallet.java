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

import com.alee.laf.WebLookAndFeel;
import com.google.worldcoin.core.StoredBlock;
import com.google.worldcoin.core.Wallet;
import com.google.worldcoin.kits.WalletAppKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallet.controller.Controller;
import org.wallet.controller.core.CoreController;
import org.wallet.controller.exchange.ExchangeController;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.file.BackupManager;
import org.wallet.file.FileHandler;
import org.wallet.file.WalletLoadException;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.core.CoreModel;
import org.wallet.model.exchange.ConnectHttps;
import org.wallet.model.exchange.ExchangeModel;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.network.*;
import org.wallet.platform.GenericApplication;
import org.wallet.platform.GenericApplicationFactory;
import org.wallet.platform.GenericApplicationSpecification;
import org.wallet.platform.listener.GenericOpenURIEvent;
import org.wallet.store.WalletVersionException;
import org.wallet.viewsystem.DisplayHint;
import org.wallet.viewsystem.ViewSystem;
import org.wallet.viewsystem.swing.ColorAndFontConstants;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.action.ExitAction;
import org.wallet.viewsystem.swing.view.components.FontSizer;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

/**
 * Main WorldcoinWallet entry class.
 *
 * @author jim
 */
public final class WorldcoinWallet {

    private static final Logger log = LoggerFactory.getLogger(WorldcoinWallet.class);

    private static Controller controller = null;

    private static CoreController coreController = null;
    private static WorldcoinController worldcoinController = null;
    private static ExchangeController exchangeController = null;

    private static String rememberedRawWorldcoinURI;

    /**
     * Utility class should not have a public constructor
     */
    private WorldcoinWallet() {
    }
    static WalletAppKit appKit = null;
    /**
     * Start WorldcoinWallet user interface.
     *
     * @param args String encoding of arguments ([0]= Worldcoin URI)
     */
    @SuppressWarnings("deprecation")
    public static void main(final String args[]) {
        // You should work with UI (including installing L&F) inside Event Dispatch Thread (EDT)

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Install WebLaF as application L&F
                //WebLookAndFeel.install ();



                log.info("Starting WorldcoinWallet at " + (new Date()).toGMTString());
                // Print out all the system properties.
                for (Map.Entry<?, ?> e : System.getProperties().entrySet()) {
                    log.debug(String.format("%s = %s", e.getKey(), e.getValue()));
                }

                ViewSystem swingViewSystem = null;
                // Enclosing try to enable graceful closure for unexpected errors.
                try {
                    // Set any bespoke system properties.
                    try {
                        // Fix for Windows / Java 7 / VPN bug.
                        System.setProperty("java.net.preferIPv4Stack", "true");

                        // Fix for version.txt not visible for Java 7
                        System.setProperty("jsse.enableSNIExtension", "false");
                    } catch (SecurityException se) {
                        log.error(se.getClass().getName() + " " + se.getMessage());
                    }

                    ApplicationDataDirectoryLocator applicationDataDirectoryLocator = new ApplicationDataDirectoryLocator();

                    // Load up the user preferences.
                    Properties userPreferences = FileHandler.loadUserPreferences(applicationDataDirectoryLocator);

                    // Create the controllers.
                    coreController = new CoreController(applicationDataDirectoryLocator);
                    controller = coreController;
                    worldcoinController = new WorldcoinController(coreController);
                    exchangeController = new ExchangeController(coreController);

                    log.info("Configuring native event handling");
                    GenericApplicationSpecification specification = new GenericApplicationSpecification();
                    specification.getOpenURIEventListeners().add(coreController);
                    specification.getPreferencesEventListeners().add(coreController);
                    specification.getAboutEventListeners().add(coreController);
                    specification.getQuitEventListeners().add(coreController);
                    GenericApplication genericApplication = GenericApplicationFactory.INSTANCE.buildGenericApplication(specification);

                    log.info("Checking to see if this is the primary WorldcoinWallet instance");
                    String rawURI = null;
                    if (args != null && args.length > 0) {
                        rawURI = args[0];
                        log.debug("The args[0] passed into WorldcoinWallet = '" + args[0] + "'");
                    }
                    if (!ApplicationInstanceManager.registerInstance(rawURI)) {
                        // Instance already running.
                        log.debug("Another instance of WorldcoinWallet is already running.  Exiting.");
                        System.exit(0);
                    }

                    final WorldcoinController finalController = worldcoinController;
                    ApplicationInstanceManager.setApplicationInstanceListener(new ApplicationInstanceListener() {
                        @Override
                        public void newInstanceCreated(String rawURI) {
                            final String finalRawUri = rawURI;
                            log.debug("New instance of WorldcoinWallet detected, rawURI = " + rawURI + " ...");
                            Runnable doProcessCommandLine = new Runnable() {
                                @Override
                                public void run() {
                                    processCommandLineURI(finalController, finalRawUri);
                                }
                            };

                            SwingUtilities.invokeLater(doProcessCommandLine);
                        }
                    });

                    Localiser localiser;
                    String userLanguageCode = userPreferences.getProperty(CoreModel.USER_LANGUAGE_CODE);
                    log.debug("userLanguageCode = {}", userLanguageCode);

                    if (userLanguageCode == null) {
                        // Initial install - no language info supplied - see if we can
                        // use the user default, else Localiser will set it to English.
                        localiser = new Localiser(Locale.getDefault());

                        userPreferences.setProperty(CoreModel.USER_LANGUAGE_CODE, localiser.getLocale().getLanguage());
                    } else {
                        if (CoreModel.USER_LANGUAGE_IS_DEFAULT.equals(userLanguageCode)) {
                            localiser = new Localiser(Locale.getDefault());
                        } else {
                            localiser = new Localiser(new Locale(userLanguageCode));
                        }
                    }
                    coreController.setLocaliser(localiser);

                    log.debug("WorldcoinWallet version = " + localiser.getVersionNumber());

                    log.debug("Creating model");

                    // Create the model.
                    // The model is set to the controller.
                    final CoreModel coreModel = new CoreModel(userPreferences);
                    final WorldcoinModel model = new WorldcoinModel(coreModel);
                    final ExchangeModel exchangeModel = new ExchangeModel(coreModel);
                    coreController.setModel(coreModel);
                    worldcoinController.setModel(model);
                    exchangeController.setModel(exchangeModel);

                    // Trust all HTTPS certificates.
                    ConnectHttps.trustAllCerts();

                    // Initialise currency converter.
                    CurrencyConverter.INSTANCE.initialise(finalController);

                    // Initialise replay manager.
                    ReplayManager.INSTANCE.initialise(worldcoinController, false);

                    log.debug("Setting look and feel");
                    try {
                        String lookAndFeel = userPreferences.getProperty(CoreModel.LOOK_AND_FEEL);

                        // If not set on Windows use 'Windows' L&F as system can be rendered as metal.
                        if ((lookAndFeel == null || lookAndFeel.equals(""))) {
                            lookAndFeel = "com.alee.laf.WebLookAndFeel";
                            userPreferences.setProperty(CoreModel.LOOK_AND_FEEL, lookAndFeel);
                        }

                        boolean lookSet = false;
                        if (lookAndFeel != null && !lookAndFeel.equals("")) {
                            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                                if (lookAndFeel.equalsIgnoreCase(info.getName())) {
                                    UIManager.setLookAndFeel(info.getClassName());
                                    lookSet = true;
                                    break;
                                }
                            }
                        }

                        if (!lookSet) {
                            UIManager.setLookAndFeel(WebLookAndFeel.class.getCanonicalName());
                        }
                    } catch (UnsupportedLookAndFeelException e) {
                        // Carry on.
                    } catch (ClassNotFoundException e) {
                        // Carry on.
                    } catch (InstantiationException e) {
                        // Carry on.
                    } catch (IllegalAccessException e) {
                        // Carry on.
                    }

                    // Initialise singletons.
                    ColorAndFontConstants.init();
                    FontSizer.INSTANCE.initialise(controller);
                    CurrencyConverter.INSTANCE.initialise(finalController);

                    // This is when the GUI is first displayed to the user.
                    log.debug("Creating user interface with initial view : " + controller.getCurrentView());
                    swingViewSystem = new WorldcoinWalletFrame(coreController, worldcoinController, exchangeController, genericApplication, controller.getCurrentView());

                    log.debug("Registering with controller");
                    coreController.registerViewSystem(swingViewSystem);

                    String userDataString = localiser.getString("worldcoin-wallet.userDataDirectory", new String[]{applicationDataDirectoryLocator.getApplicationDataDirectory()});
                    log.debug(userDataString);
                    Message directoryMessage1 = new Message(userDataString);
                    directoryMessage1.setShowInStatusBar(false);
                    MessageManager.INSTANCE.addMessage(directoryMessage1);

                    String installationDirString = localiser.getString("worldcoin-wallet.installationDirectory", new String[]{applicationDataDirectoryLocator.getInstallationDirectory()});
                    log.debug(installationDirString);
                    Message directoryMessage2 = new Message(installationDirString);
                    directoryMessage2.setShowInStatusBar(false);
                    MessageManager.INSTANCE.addMessage(directoryMessage2);

                    log.debug("Creating Worldcoin service");
                    // Create the WorldcoinWalletService that connects to the worldcoin network.
                    WorldcoinWalletService worldcoinWalletService = new WorldcoinWalletService(worldcoinController);
                    worldcoinController.setWorldcoinWalletService(worldcoinWalletService);

                    log.debug("Locating wallets");
                    // Find the active wallet filename in the wallet.properties.
                    String activeWalletFilename = userPreferences.getProperty(WorldcoinModel.ACTIVE_WALLET_FILENAME);

                    // Get the number of the early wallets - these are serialised and protobuf2
                    String numberOfEarlyWalletsAsString = userPreferences.getProperty(WorldcoinModel.NUMBER_OF_EARLY_WALLETS);
                    log.debug("When loading early wallets, there were " + numberOfEarlyWalletsAsString);

                    // Get the number of the protobuf3 wallets
                    String numberOfProtobuf3WalletsAsString = userPreferences.getProperty(WorldcoinModel.NUMBER_OF_PROTOBUF3_WALLETS);
                    log.debug("When loading protobuf3 wallets, there were " + numberOfProtobuf3WalletsAsString);

                    boolean useFastCatchup = false;

                    if (numberOfEarlyWalletsAsString == null || "".equals(numberOfEarlyWalletsAsString) || "null".equals(numberOfEarlyWalletsAsString)) {
                        // If this is missing then there is just the one wallet (old format
                        // properties or user has just started up for the first time).
                        useFastCatchup = true;
                        boolean thereWasAnErrorLoadingTheWallet = false;

                        try {
                            boolean backupWallet = false;
                            boolean moveSiblingFiles = false;

                            // If there is no active filename this is a new instance of WorldcoinWallet so backup the new wallet when created.
                            if (activeWalletFilename == null || "".equals(activeWalletFilename) || "null".equals(activeWalletFilename)) {
                                backupWallet = true;
                            } else {
                                // See if a data directory is missing - if so we will move in any wallet or key files and backup.
                                String topLevelWalletDirectory = BackupManager.INSTANCE.calculateTopLevelBackupDirectoryName(new File(activeWalletFilename));
                                moveSiblingFiles = !(new File(topLevelWalletDirectory).exists());
                                backupWallet = moveSiblingFiles;
                            }

                            // ActiveWalletFilename may be null on first time startup.
                            worldcoinController.addWalletFromFilename(activeWalletFilename);
                            List<WalletData> perWalletModelDataList = worldcoinController.getModel().getPerWalletModelDataList();
                            if (perWalletModelDataList != null && !perWalletModelDataList.isEmpty()) {
                                activeWalletFilename = perWalletModelDataList.get(0).getWalletFilename();
                                worldcoinController.getModel().setActiveWalletByFilename(activeWalletFilename);
                                log.debug("Created/loaded wallet '" + activeWalletFilename + "'");
                                MessageManager.INSTANCE.addMessage(new Message(controller.getLocaliser().getString(
                                        "worldcoinWallet.createdWallet", new Object[]{activeWalletFilename})));

                                if (backupWallet) {
                                    // Backup the wallet and wallet info.
                                    BackupManager.INSTANCE.backupPerWalletModelData(worldcoinController.getFileHandler(), perWalletModelDataList.get(0));
                                }
                                if (moveSiblingFiles) {
                                    // Move any timestamped key and wallet files into their appropriate directories
                                    BackupManager.INSTANCE.moveSiblingTimestampedKeyAndWalletBackups(activeWalletFilename);
                                }
                            }
                        } catch (WalletLoadException e) {
                            String message = controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                    new Object[]{activeWalletFilename, e.getMessage()});
                            MessageManager.INSTANCE.addMessage(new Message(message));
                            log.error(message);
                            thereWasAnErrorLoadingTheWallet = true;
                        } catch (WalletVersionException e) {
                            String message = controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                    new Object[]{activeWalletFilename, e.getMessage()});
                            MessageManager.INSTANCE.addMessage(new Message(message));
                            log.error(message);
                            thereWasAnErrorLoadingTheWallet = true;
                        } catch (IOException e) {
                            String message = controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                    new Object[]{activeWalletFilename, e.getMessage()});
                            MessageManager.INSTANCE.addMessage(new Message(message));
                            log.error(message);
                            thereWasAnErrorLoadingTheWallet = true;
                        } catch (Exception e) {
                            String message = controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                    new Object[]{activeWalletFilename, e.getMessage()});
                            MessageManager.INSTANCE.addMessage(new Message(message));
                            log.error(message);
                            thereWasAnErrorLoadingTheWallet = true;
                        } finally {
                            if (thereWasAnErrorLoadingTheWallet) {
                                // Clear the backup wallet filename - this prevents it being automatically overwritten.
                                if (worldcoinController.getModel().getActiveWalletInfo() != null) {
                                    worldcoinController.getModel().getActiveWalletInfo().put(WorldcoinModel.WALLET_BACKUP_FILE, "");
                                }
                            }
                            if (swingViewSystem instanceof WorldcoinWalletFrame) {
                                ((WorldcoinWalletFrame) swingViewSystem).getWalletsView().initUI();
                                ((WorldcoinWalletFrame) swingViewSystem).getWalletsView().displayView(DisplayHint.COMPLETE_REDRAW);
                            }
                            controller.fireDataChangedUpdateNow();
                        }
                    } else {
                        try {
                            List<String> walletFilenamesToLoad = new ArrayList<String>();
                            try {
                                int numberOfEarlyWallets = Integer.parseInt(numberOfEarlyWalletsAsString);
                                if (numberOfEarlyWallets > 0) {
                                    for (int i = 1; i <= numberOfEarlyWallets; i++) {
                                        // Look up ith wallet filename.
                                        String loopWalletFilename = userPreferences.getProperty(WorldcoinModel.EARLY_WALLET_FILENAME_PREFIX + i);
                                        if (!walletFilenamesToLoad.contains(loopWalletFilename)) {
                                            walletFilenamesToLoad.add(loopWalletFilename);
                                        }
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                // Carry on.
                            }
                            try {
                                int numberOfProtobuf3Wallets = Integer.parseInt(numberOfProtobuf3WalletsAsString);
                                if (numberOfProtobuf3Wallets > 0) {
                                    for (int i = 1; i <= numberOfProtobuf3Wallets; i++) {
                                        // Look up ith wallet filename.
                                        String loopWalletFilename = userPreferences.getProperty(WorldcoinModel.PROTOBUF3_WALLET_FILENAME_PREFIX + i);
                                        if (!walletFilenamesToLoad.contains(loopWalletFilename)) {
                                            walletFilenamesToLoad.add(loopWalletFilename);
                                        }
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                // Carry on.
                            }

                            // Load up the order the wallets are to appear in.
                            // There may be wallets in this list of types from the future but only load wallets we know about
                            boolean haveWalletOrder = false;
                            List<String> walletFilenameOrder = new ArrayList<String>();
                            try {
                                String walletOrderTotalAsString = userPreferences.getProperty(WorldcoinModel.WALLET_ORDER_TOTAL);
                                log.debug("When loading the wallet orders, there were " + walletOrderTotalAsString);

                                int walletOrderTotal = Integer.parseInt(walletOrderTotalAsString);
                                if (walletOrderTotal > 0) {
                                    haveWalletOrder = true;
                                    for (int i = 1; i <= walletOrderTotal; i++) {
                                        // Add the wallet filename order.
                                        String loopWalletFilename = userPreferences.getProperty(WorldcoinModel.WALLET_ORDER_PREFIX + i);
                                        if (!walletFilenameOrder.contains(loopWalletFilename)) {
                                            walletFilenameOrder.add(loopWalletFilename);
                                        }
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                // Carry on.
                            }

                            List<String> actualOrderToLoad = new ArrayList<String>();
                            if (haveWalletOrder) {
                                for (String orderWallet : walletFilenameOrder) {
                                    if (walletFilenamesToLoad.contains(orderWallet)) {
                                        // Add it.
                                        actualOrderToLoad.add(orderWallet);
                                    }
                                }
                                // There may be some extras so add them to the end.
                                for (String loadWallet : walletFilenamesToLoad) {
                                    if (!walletFilenameOrder.contains(loadWallet)) {
                                        // Add it.
                                        actualOrderToLoad.add(loadWallet);
                                    }
                                }
                            } else {
                                // Just load all the wallets, early then later.
                                for (String loadWallet : walletFilenamesToLoad) {
                                    if (!actualOrderToLoad.contains(loadWallet)) {
                                        // Add it.
                                        actualOrderToLoad.add(loadWallet);
                                    }
                                }
                            }

                            if (actualOrderToLoad.size() > 0) {
                                boolean thereWasAnErrorLoadingTheWallet = false;

                                ((WorldcoinWalletFrame) swingViewSystem).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                for (String actualOrder : actualOrderToLoad) {
                                    log.debug("Loading wallet from '{}'", actualOrder);
                                    Message message = new Message(controller.getLocaliser().getString("worldcoinWallet.openingWallet",
                                            new Object[]{actualOrder}));
                                    message.setShowInStatusBar(false);
                                    MessageManager.INSTANCE.addMessage(message);
                                    try {
                                        // Check if this is the first time this wallet has been opened post addition of data directories.
                                        String topLevelWalletDirectory = BackupManager.INSTANCE.calculateTopLevelBackupDirectoryName(new File(actualOrder));
                                        boolean firstUsageSinceWalletDirectoriesIntroduced = !(new File(topLevelWalletDirectory).exists());

                                        WalletData perWalletModelData = null;
                                        if (activeWalletFilename != null && activeWalletFilename.equals(actualOrder)) {
                                            perWalletModelData = worldcoinController.addWalletFromFilename(actualOrder);
                                            worldcoinController.getModel().setActiveWalletByFilename(actualOrder);
                                        } else {
                                            perWalletModelData = worldcoinController.addWalletFromFilename(actualOrder);
                                        }
                                        Message message2 = new Message(controller.getLocaliser().getString("worldcoinWallet.openingWalletIsDone",
                                                new Object[]{actualOrder}));
                                        message2.setShowInStatusBar(false);
                                        MessageManager.INSTANCE.addMessage(message2);

                                        if (firstUsageSinceWalletDirectoriesIntroduced) {
                                            if (perWalletModelData != null && perWalletModelData.getWallet() != null) {
                                                // Backup the wallet and wallet info.
                                                BackupManager.INSTANCE.backupPerWalletModelData(worldcoinController.getFileHandler(), perWalletModelData);

                                                // Move any timestamped key and wallet files into their appropriate directories
                                                BackupManager.INSTANCE.moveSiblingTimestampedKeyAndWalletBackups(actualOrder);
                                            }
                                        }
                                    } catch (WalletLoadException e) {
                                        message = new Message(controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                                new Object[]{actualOrder, e.getMessage()}));
                                        MessageManager.INSTANCE.addMessage(message);
                                        log.error(message.getText());
                                        thereWasAnErrorLoadingTheWallet = true;
                                    } catch (WalletVersionException e) {
                                        message = new Message(controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                                new Object[]{actualOrder, e.getMessage()}));
                                        MessageManager.INSTANCE.addMessage(message);
                                        log.error(message.getText());
                                        thereWasAnErrorLoadingTheWallet = true;
                                    } catch (IOException e) {
                                        message = new Message(controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                                new Object[]{actualOrder, e.getMessage()}));
                                        MessageManager.INSTANCE.addMessage(message);
                                        log.error(message.getText());
                                        thereWasAnErrorLoadingTheWallet = true;
                                    } catch (Exception e) {
                                        message = new Message(controller.getLocaliser().getString("openWalletSubmitAction.walletNotLoaded",
                                                new Object[]{actualOrder, e.getMessage()}));
                                        MessageManager.INSTANCE.addMessage(message);
                                        log.error(message.getText());
                                        thereWasAnErrorLoadingTheWallet = true;
                                    }

                                    if (thereWasAnErrorLoadingTheWallet) {
                                        WalletData loopData = worldcoinController.getModel().getPerWalletModelDataByWalletFilename(actualOrder);
                                        if (loopData != null) {
                                            // Clear the backup wallet filename - this prevents it being automatically overwritten.
                                            if (loopData.getWalletInfo() != null) {
                                                loopData.getWalletInfo().put(WorldcoinModel.WALLET_BACKUP_FILE, "");
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            // Carry on.
                        } finally {
                            if (swingViewSystem instanceof WorldcoinWalletFrame) {
                                ((WorldcoinWalletFrame) swingViewSystem).getWalletsView().initUI();
                                ((WorldcoinWalletFrame) swingViewSystem).getWalletsView().displayView(DisplayHint.COMPLETE_REDRAW);
                            }
                            controller.fireDataChangedUpdateNow();

                            ((WorldcoinWalletFrame) swingViewSystem).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }

                    log.debug("Checking for Worldcoin URI on command line");
                    // Check for a valid entry on the command line (protocol handler).
                    if (args != null && args.length > 0) {
                        for (int i = 0; i < args.length; i++) {
                            log.debug("Started with args[{}]: '{}'", i, args[i]);
                        }
                        processCommandLineURI(worldcoinController, args[0]);
                    } else {
                        log.debug("No Worldcoin URI provided as an argument");
                    }

                    // Indicate to the application that startup has completed.
                    coreController.setApplicationStarting(false);

                    // Check for any pending URI operations.
                    worldcoinController.handleOpenURI(rememberedRawWorldcoinURI);

                    // Check to see if there is a new version.
                    AlertManager.INSTANCE.initialise(worldcoinController, (WorldcoinWalletFrame) swingViewSystem);
                    AlertManager.INSTANCE.checkVersion();

                    log.debug("Downloading blockchain");
                    if (useFastCatchup) {
                        long earliestTimeSecs = worldcoinController.getModel().getActiveWallet().getEarliestKeyCreationTime();
                        worldcoinController.getWorldcoinWalletService().getPeerGroup().setFastCatchupTimeSecs(earliestTimeSecs);
                        log.debug("Using FastCatchup for blockchain sync with time of " + (new Date(earliestTimeSecs)).toString());
                    }

                    // Work out the late date/ block the wallets saw to see if it needs syncing
                    // or if we can use regular downloading.
                    int currentChainHeight = -1;
                    if (worldcoinController.getWorldcoinWalletService().getChain() != null) {
                        if (worldcoinController.getWorldcoinWalletService().getChain().getChainHead() != null) {
                            currentChainHeight = worldcoinController.getWorldcoinWalletService().getChain().getChainHead().getHeight();
                        }
                    }

                    log.debug("The current chain height is " + currentChainHeight);

                    List<WalletData> perWalletModelDataList = worldcoinController.getModel().getPerWalletModelDataList();
                    boolean needToSync = false;
                    int syncFromHeight = -1;

                    List<WalletData> replayPerWalletModelList = new ArrayList<WalletData>();
                    if (perWalletModelDataList != null) {
                        for (WalletData perWalletModelData : perWalletModelDataList) {
                            Wallet wallet = perWalletModelData.getWallet();
                            if (wallet != null) {
                                int lastBlockSeenHeight = wallet.getLastBlockSeenHeight();
                                log.debug("For wallet '" + perWalletModelData.getWalletFilename() + " the lastBlockSeenHeight was "
                                        + lastBlockSeenHeight);

                                // Check if we have both the lastBlockSeenHeight and the currentChainHeight.
                                if (lastBlockSeenHeight > 0 && currentChainHeight > 0) {
                                    if (lastBlockSeenHeight >= currentChainHeight) {
                                        // WorldcoinWallet is at or ahead of current chain - no
                                        // need to sync for this wallet.
                                    } else {
                                        // WorldcoinWallet is behind the current chain - need to sync.
                                        needToSync = true;

                                        replayPerWalletModelList.add(perWalletModelData);
                                        if (syncFromHeight == -1) {
                                            syncFromHeight = lastBlockSeenHeight;
                                        } else {
                                            syncFromHeight = Math.min(syncFromHeight, lastBlockSeenHeight);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    log.debug("needToSync = " + needToSync);

                    if (needToSync) {
                        StoredBlock syncFromStoredBlock = null;

                        WorldcoinWalletCheckpointManager checkpointManager = worldcoinController.getWorldcoinWalletService().getCheckpointManager();
                        if (checkpointManager != null) {
                            syncFromStoredBlock = checkpointManager.getCheckpointBeforeOrAtHeight(syncFromHeight);
                        }

                        ReplayTask replayTask;
                        if (syncFromStoredBlock == null) {
                            // Sync from genesis block.
                            replayTask = new ReplayTask(replayPerWalletModelList, null, 0);
                        } else {
                            Date syncDate = null;
                            if (syncFromStoredBlock.getHeader() != null) {
                                syncDate = new Date(syncFromStoredBlock.getHeader().getTimeSeconds() * 1000);
                            }
                            replayTask = new ReplayTask(replayPerWalletModelList, syncDate, syncFromStoredBlock.getHeight());
                        }
                        ReplayManager.INSTANCE.offerReplayTask(replayTask);
                    } else {
                        // Just sync the blockchain without a replay task being involved.
                        ReplayManager.INSTANCE.downloadBlockChain();
                    }
                } catch (Exception e) {
                    // An odd unrecoverable error occurred.
                    e.printStackTrace();

                    log.error("An unexpected error caused WorldcoinWallet to quit.");
                    log.error("The error was '" + e.getClass().getCanonicalName() + " " + e.getMessage() + "'");
                    e.printStackTrace();
                    log.error("Please read http://wallet.org/help_troubleshooting.html for help on troubleshooting.");

                    // Try saving any dirty wallets.
                    if (controller != null) {
                        ExitAction exitAction = new ExitAction(controller, (WorldcoinWalletFrame) swingViewSystem);
                        exitAction.actionPerformed(null);
                    }
                }
            }
        });
    }

    static void processCommandLineURI(WorldcoinController controller, String rawURI) {
        try {
            // Attempt to detect if the command line URI is valid.
            // Note that this is largely because IE6-8 strip URL encoding
            // when passing in URIs to a protocol handler.
            // However, there is also the chance that anyone could
            // hand-craft a URI and pass
            // it in with non-ASCII character encoding present in the label
            // This a really limited approach (no consideration of
            // "amount=10.0&label=Black & White")
            // but should be OK for early use cases.
            int queryParamIndex = rawURI.indexOf('?');
            if (queryParamIndex > 0 && !rawURI.contains("%")) {
                // Possibly encoded but more likely not
                String encodedQueryParams = URLEncoder.encode(rawURI.substring(queryParamIndex + 1), "UTF-8");
                rawURI = rawURI.substring(0, queryParamIndex) + "?" + encodedQueryParams;
                rawURI = rawURI.replaceAll("%3D", "=");
                rawURI = rawURI.replaceAll("%26", "&");
            }
            final URI uri;
            log.debug("Working with '{}' as a Worldcoin URI", rawURI);
            // Construct an OpenURIEvent to simulate receiving this from a
            // listener
            uri = new URI(rawURI);
            GenericOpenURIEvent event = new GenericOpenURIEvent() {
                @Override
                public URI getURI() {
                    return uri;
                }
            };
            controller.displayView(controller.getCurrentView());
            // Call the event which will attempt validation against the
            // Worldcoin URI specification.
            coreController.onOpenURIEvent(event);
        } catch (URISyntaxException e) {
            log.error("URI is malformed. Received: '{}'", rawURI);
        } catch (UnsupportedEncodingException e) {
            log.error("UTF=8 is not supported on this platform");
        }
    }

    public static Controller getController() {
        return controller;
    }

    public static CoreController getCoreController() {
        return coreController;
    }

    public static WorldcoinController getWorldcoinController() {
        return worldcoinController;
    }

    public static ExchangeController getExchangeController() {
        return exchangeController;
    }

    /**
     * Used in testing
     */
    public static void setCoreController(CoreController coreController) {
        WorldcoinWallet.controller = coreController;
        WorldcoinWallet.coreController = coreController;
    }

    public static void setWorldcoinController(WorldcoinController worldcoinController) {
        WorldcoinWallet.worldcoinController = worldcoinController;
    }

    public static void setExchangeController(ExchangeController exchangeController) {
        WorldcoinWallet.exchangeController = exchangeController;
    }

    public static String getRememberedRawWorldcoinURI() {
        return rememberedRawWorldcoinURI;
    }

    public static void setRememberedRawWorldcoinURI(String rememberedRawWorldcoinURI) {
        log.debug("Remembering the worldcoin URI to process of '" + rememberedRawWorldcoinURI + "'");
        WorldcoinWallet.rememberedRawWorldcoinURI = rememberedRawWorldcoinURI;
    }
}
