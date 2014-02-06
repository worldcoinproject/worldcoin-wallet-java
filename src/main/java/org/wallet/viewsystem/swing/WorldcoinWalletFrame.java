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
package org.wallet.viewsystem.swing;

import com.google.worldcoin.core.ECKey;
import com.google.worldcoin.core.Sha256Hash;
import com.google.worldcoin.core.Transaction;
import com.google.worldcoin.core.Wallet;
import com.google.worldcoin.script.Script;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.worldcoinj.wallet.Protos.Wallet.EncryptionType;
import org.joda.money.Money;
import org.wallet.Localiser;
import org.wallet.controller.Controller;
import org.wallet.controller.core.CoreController;
import org.wallet.controller.exchange.ExchangeController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.exchange.CurrencyConverterListener;
import org.wallet.exchange.ExchangeRate;
import org.wallet.exchange.TickerTimerTask;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.worldcoin.WalletBusyListener;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.core.StatusEnum;
import org.wallet.model.exchange.ExchangeModel;
import org.wallet.network.ReplayManager;
import org.wallet.platform.GenericApplication;
import org.wallet.platform.listener.GenericQuitEventListener;
import org.wallet.platform.listener.GenericQuitResponse;
import org.wallet.store.WorldcoinWalletVersion;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.DisplayHint;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.ViewSystem;
import org.wallet.viewsystem.Viewable;
import org.wallet.viewsystem.swing.action.*;
import org.wallet.viewsystem.swing.view.ViewFactory;
import org.wallet.viewsystem.swing.view.components.*;
import org.wallet.viewsystem.swing.view.panels.HelpContentsPanel;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinConfirmPanel;
import org.wallet.viewsystem.swing.view.panels.ShowTransactionsPanel;
import org.wallet.viewsystem.swing.view.ticker.TickerTablePanel;
import org.wallet.viewsystem.swing.view.walletlist.SingleWalletPanel;
import org.wallet.viewsystem.swing.view.walletlist.WalletListPanel;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.math.BigInteger;
import java.util.List;
import java.util.Timer;


/*
 * JFrame displaying Swing version of WorldcoinWallet
 */
public class WorldcoinWalletFrame extends JFrame implements ViewSystem, ApplicationListener, WalletBusyListener, CurrencyConverterListener {

    private static final Logger log = LoggerFactory.getLogger(WorldcoinWalletFrame.class);

    private static final double PROPORTION_OF_VERTICAL_SCREEN_TO_FILL = 0.75D;
    private static final double PROPORTION_OF_HORIZONTAL_SCREEN_TO_FILL = 0.82D;

    public static final String EXAMPLE_LONG_FIELD_TEXT = "1JiM1UyTGqpLqgayxTPbWbcdVeoepmY6pK++++";
    public static final String EXAMPLE_MEDIUM_FIELD_TEXT = "Typical text 00.12345678 WDC (000.01 XYZ)";

    public static final int WALLET_WIDTH_DELTA = 25;

    public static final int SCROLL_BAR_DELTA = 20;

    public static final int WIDTH_OF_SPLIT_PANE_DIVIDER = 9;

    public static final int MENU_HORIZONTAL_INSET = 8;
    public static final int MENU_VERTICAL_INSET = 1;

    public static final int BALANCE_SPACER = 7;

    public static final String SPENDABLE_TEXT_IN_ENGLISH = "Spendable";

    private StatusBar statusBar;
    private StatusEnum online = StatusEnum.CONNECTING;
    public static final String SEPARATOR = " - ";

    private static final long serialVersionUID = 7621813615342923041L;

    private final Controller controller;
    private final CoreController coreController;
    private final WorldcoinController worldcoinController;
    private final ExchangeController exchangeController;
    
    private Localiser localiser;

    private String helpContext;

    public String getHelpContext() {
        return helpContext;
    }

    @Override
    public void setHelpContext(String helpContext) {
        this.helpContext = helpContext;
    }

    private WorldcoinWalletLabel estimatedBalanceLabelLabel;
    private BlinkLabel estimatedBalanceWDCLabel;
    private BlinkLabel estimatedBalanceFiatLabel;

    public BlinkLabel getEstimatedBalanceWDCLabel() {
        return estimatedBalanceWDCLabel;
    }

    public BlinkLabel getEstimatedBalanceFiatLabel() {
        return estimatedBalanceFiatLabel;
    }

    private HelpButton availableBalanceLabelButton;
    private HelpButton availableBalanceWDCButton;
    private HelpButton availableBalanceFiatButton;

    /**
     * list of wallets shown in left hand column
     */
    private WalletListPanel walletsView;

    private JSplitPane splitPane;

    private static final int TOOLTIP_DISMISSAL_DELAY = 12000; // millisecs

    /**
     * Provide the Application reference during construction
     */
    private final GenericApplication application;

    private final GenericQuitResponse worldcoinWalletFrameQuitResponse = new GenericQuitResponse() {
        @Override
        public void cancelQuit() {
            log.debug("Quit Canceled");
        }

        @Override
        public void performQuit() {
            log.debug("Performed Quit");
        }
    };
    
    final private GenericQuitEventListener quitEventListener;

    /**
     * the tabbed pane containing the views
     * 
     */
    private WorldcoinWalletTabbedPane viewTabbedPane;

    public Logger logger = LoggerFactory.getLogger(WorldcoinWalletFrame.class.getName());

    private ViewFactory viewFactory;

    private Timer fileChangeTimer;
    private HealthCheckTimerTask healthCheckTimerTask;

    private Timer tickerTimer1;
    private Timer tickerTimer2;
    private TickerTimerTask tickerTimerTask1;
    private TickerTimerTask tickerTimerTask2;

    private JPanel headerPanel;

    private TickerTablePanel tickerTablePanel;
    
    private WorldcoinWalletBusyAction addPasswordAction;
    private WorldcoinWalletBusyAction changePasswordAction;
    private WorldcoinWalletBusyAction removePasswordAction;
    
    private WorldcoinWalletBusyAction signMessageAction;
    
    private WorldcoinWalletBusyAction showImportPrivateKeysAction;
    private WorldcoinWalletBusyAction showExportPrivateKeysAction;
    private WorldcoinWalletBusyAction resetTransactionsAction;
 
    /**
     * For events coming from Peers condense the events into regular updates.
     * This is to prevent the UI thrashing with hundreds of events per second.
     */
    public static final int FIRE_DATA_CHANGED_UPDATE_LATER_DELAY_TIME = 1000; // milliseconds
 
    /**
     * Timer used to condense multiple updates
     */
    private static Timer fireDataChangedTimer;

    private static FireDataChangedTimerTask fireDataChangedTimerTask;

    @SuppressWarnings("deprecation")
    public WorldcoinWalletFrame(CoreController coreController, WorldcoinController worldcoinController, ExchangeController exchangeController, GenericApplication application, View initialView) {
        this.coreController = coreController;
        this.worldcoinController = worldcoinController;
        this.exchangeController = exchangeController;
        this.controller = this.coreController;
        
        this.quitEventListener = this.coreController;
        
        this.localiser = controller.getLocaliser();
        this.application = application;
        
        // Remap to command v and C on a Mac
        if (application != null && application.isMac()) {
            InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
        }
        
        ColorAndFontConstants.init();

        FontSizer.INSTANCE.initialise(controller);
        UIManager.put("ToolTip.font", FontSizer.INSTANCE.getAdjustedDefaultFont());

        setCursor(Cursor.WAIT_CURSOR);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String titleText = localiser.getString("worldcoinWalletFrame.title");
        if (this.worldcoinController.getModel().getActiveWallet() != null) {
            titleText = titleText + SEPARATOR + this.worldcoinController.getModel().getActivePerWalletModelData().getWalletDescription()
                    + SEPARATOR + this.worldcoinController.getModel().getActivePerWalletModelData().getWalletFilename();
        }
        setTitle(titleText);

        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_DISMISSAL_DELAY);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                quitEventListener.onQuitEvent(null, worldcoinWalletFrameQuitResponse);
            }
        });

        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        sizeAndCenter();

        viewFactory = new ViewFactory(this.worldcoinController, this.exchangeController, this);

        initUI(initialView);

        this.worldcoinController.registerWalletBusyListener(this);

         // Initialise the file change timer.
        fileChangeTimer = new Timer();
        healthCheckTimerTask = new HealthCheckTimerTask(this.worldcoinController);
        fileChangeTimer.schedule(healthCheckTimerTask, HealthCheckTimerTask.INITIAL_DELAY, HealthCheckTimerTask.DEFAULT_REPEAT_RATE);

         // Initialise the tickers.
        tickerTimer1 = new Timer();
        tickerTimerTask1 = new TickerTimerTask(this.exchangeController, this, true);
        tickerTimer1.schedule(tickerTimerTask1, TickerTimerTask.INITIAL_DELAY, TickerTimerTask.DEFAULT_REPEAT_RATE);
        
        tickerTimer2 = new Timer();
        tickerTimerTask2 = new TickerTimerTask(this.exchangeController, this, false);
        tickerTimer2.schedule(tickerTimerTask2, TickerTimerTask.INITIAL_DELAY + TickerTimerTask.TASK_SEPARATION, TickerTimerTask.DEFAULT_REPEAT_RATE);

        // Initialise status bar.
        statusBar.initialise();

        estimatedBalanceLabelLabel.setFocusable(false);
        estimatedBalanceWDCLabel.setFocusable(false);
        estimatedBalanceFiatLabel.setFocusable(false);

        availableBalanceLabelButton.setFocusable(false);
        availableBalanceWDCButton.setFocusable(false);
        availableBalanceFiatButton.setFocusable(false);
               
        updateHeader();

        calculateDividerPosition();
 
        WorldcoinWalletTabbedPane.setEnableUpdates(true);
        
        CurrencyConverter.INSTANCE.addCurrencyConverterListener(this);
        
        displayView(null != initialView ? initialView : View.DEFAULT_VIEW());

        pack();

        setVisible(true);
        
        fireDataChangedTimerTask = new FireDataChangedTimerTask(this);
        fireDataChangedTimer = new Timer();
        fireDataChangedTimer.scheduleAtFixedRate(fireDataChangedTimerTask, FIRE_DATA_CHANGED_UPDATE_LATER_DELAY_TIME, FIRE_DATA_CHANGED_UPDATE_LATER_DELAY_TIME);
    }

    public GenericApplication getApplication() {
        return application;
    }

    private void sizeAndCenter() {
        // Get the screen size as a java dimension.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int height = (int) (screenSize.height * PROPORTION_OF_VERTICAL_SCREEN_TO_FILL);
        int width = (int) (screenSize.width * PROPORTION_OF_HORIZONTAL_SCREEN_TO_FILL);

        // Set the jframe height and width.
        setPreferredSize(new Dimension(width, height));
        double startVerticalPositionRatio = (1 - PROPORTION_OF_VERTICAL_SCREEN_TO_FILL) / 2;
        double startHorizontalPositionRatio = (1 - PROPORTION_OF_HORIZONTAL_SCREEN_TO_FILL) / 2;
        setLocation((int) (width * startHorizontalPositionRatio), (int) (height * startVerticalPositionRatio));
    }

    private void initUI(View initialView) {
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        contentPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        GridBagConstraints constraints = new GridBagConstraints();
        GridBagConstraints constraints2 = new GridBagConstraints();

        // Set the application icon.
        ImageIcon imageIcon = ImageLoader.createImageIcon(ImageLoader.WORLDCOIN_WALLET_ICON_FILE);
        if (imageIcon != null) {
            setIconImage(imageIcon.getImage());
        }

        headerPanel = new JPanel();
        headerPanel.setOpaque(true);
        headerPanel.setBackground(ColorAndFontConstants.BACKGROUND_COLOR);
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        JPanel balancePanel = createBalancePanel();
        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 0;
        constraints2.gridy = 0;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.weightx = 1.0;
        constraints2.weighty = 1.0;
        constraints2.anchor = GridBagConstraints.LINE_START;

        headerPanel.add(balancePanel, constraints2);

        addMenuBar(constraints, contentPane);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.LINE_START;
        contentPane.add(headerPanel, constraints);

        // Create the wallet list panel.
        walletsView = new WalletListPanel(this.worldcoinController, this);

        // Create the tabbedpane that holds the views.
        viewTabbedPane = new WorldcoinWalletTabbedPane(controller);
        viewTabbedPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

        // Add the send worldcoin tab.
        JPanel sendWorldcoinOutlinePanel = new JPanel(new BorderLayout());
        Viewable sendWorldcoinView = viewFactory.getView(View.SEND_WORLDCOIN_VIEW);
        sendWorldcoinOutlinePanel.add((JPanel) sendWorldcoinView, BorderLayout.CENTER);
        viewTabbedPane.addTab(sendWorldcoinView.getViewTitle(), sendWorldcoinView.getViewIcon(), sendWorldcoinView.getViewTooltip(),
                sendWorldcoinOutlinePanel);

        // Add the receive worldcoin tab.
        JPanel receiveWorldcoinOutlinePanel = new JPanel(new BorderLayout());
        Viewable receiveWorldcoinView = viewFactory.getView(View.RECEIVE_WORLDCOIN_VIEW);
        receiveWorldcoinOutlinePanel.add((JPanel) receiveWorldcoinView, BorderLayout.CENTER);
        viewTabbedPane.addTab(receiveWorldcoinView.getViewTitle(), receiveWorldcoinView.getViewIcon(),
                receiveWorldcoinView.getViewTooltip(), receiveWorldcoinOutlinePanel);

        // Add the transactions tab.
        JPanel transactionsOutlinePanel = new JPanel(new BorderLayout());
        Viewable transactionsView = viewFactory.getView(View.TRANSACTIONS_VIEW);
        transactionsOutlinePanel.add((JPanel) transactionsView, BorderLayout.CENTER);
        viewTabbedPane.addTab(transactionsView.getViewTitle(), transactionsView.getViewIcon(), transactionsView.getViewTooltip(),
                transactionsOutlinePanel);
        
        if (initialView == View.SEND_WORLDCOIN_VIEW) {
            viewTabbedPane.setSelectedIndex(0);
        } else if (initialView == View.RECEIVE_WORLDCOIN_VIEW) {
            viewTabbedPane.setSelectedIndex(1);
        } else if (initialView == View.TRANSACTIONS_VIEW) {
            viewTabbedPane.setSelectedIndex(2);
        } 

        // Create a split pane with the two scroll panes in it.
        if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, walletsView, viewTabbedPane);
        } else {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewTabbedPane, walletsView);
            splitPane.setResizeWeight(1.0);
        }

        splitPane.setOneTouchExpandable(false);
        splitPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.windowBorder));
        splitPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        splitPane.setOpaque(true);
        
        BasicSplitPaneDivider divider = ( ( javax.swing.plaf.basic.BasicSplitPaneUI)splitPane.getUI()).getDivider();
        divider.setDividerSize(WIDTH_OF_SPLIT_PANE_DIVIDER);
       
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1000.0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        contentPane.add(splitPane, constraints);

        calculateDividerPosition();
        
        // Cannot get the RTL wallets drawing nicely so switch off adjustment.
        splitPane.setEnabled(ComponentOrientation.LEFT_TO_RIGHT.equals(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())));
        
        statusBar = new StatusBar(this.worldcoinController, this);
        statusBar.updateOnlineStatusText(online);
        MessageManager.INSTANCE.addMessageListener(statusBar);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 2;
        contentPane.add(statusBar, constraints);
    }

    private JPanel createBalancePanel() {
        // Change the 'available to spend' text in English to 'Spendable'.
        // This is not in the localisation files as it is a synonym and I did not want the localisers to have to reenter their text.
        String spendableText;
        if (controller.getLocaliser().getLocale().getLanguage().equals("en")) {
            spendableText = SPENDABLE_TEXT_IN_ENGLISH;
        } else {
            spendableText = controller.getLocaliser().getString("worldcoinWalletFrame.availableToSpend2");
        }
        FontMetrics fontMetrics = this.getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont());
        int spendableWidth = fontMetrics.stringWidth(spendableText);
        int spendableHeight = fontMetrics.getHeight();

        tickerTablePanel = new TickerTablePanel(this, this.exchangeController);

        //HorizontalGradientPanel headerPanel = new HorizontalGradientPanel(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        JPanel headerPanel = new JPanel();

        int heightOfBalances =  2 * spendableHeight + 3 * BALANCE_SPACER -2 + 3 * ColorAndFontConstants.WORLDCOIN_WALLET_LARGE_FONT_INCREASE;
        int heightOfHeaderToUse = Math.max(heightOfBalances, tickerTablePanel.getIdealHeight());

        headerPanel.setMinimumSize(new Dimension(700, heightOfHeaderToUse));
        headerPanel.setPreferredSize(new Dimension(700, heightOfHeaderToUse));
        headerPanel.setOpaque(true);
        headerPanel.setBackground(ColorAndFontConstants.MID_BACKGROUND_COLOR);
        headerPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.windowBorder));
        GridBagConstraints constraints = new GridBagConstraints();

        // Top row filler.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.01;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(WorldcoinWalletTitledPanel.createStent(BALANCE_SPACER, BALANCE_SPACER), constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 0.01;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;

        int stentWidth = Math.max(spendableWidth, fontMetrics.stringWidth(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel")));

        headerPanel.add(WorldcoinWalletTitledPanel.createStent(stentWidth, 1), constraints);

        estimatedBalanceLabelLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel"), JTextField.RIGHT);
        estimatedBalanceLabelLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel.tooltip")));
        estimatedBalanceLabelLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFontWithDelta(3 * ColorAndFontConstants.WORLDCOIN_WALLET_LARGE_FONT_INCREASE));

        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.weightx = 0.6;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        headerPanel.add(estimatedBalanceLabelLabel, constraints);
        headerPanel.add(WorldcoinWalletTitledPanel.createStent(spendableWidth, spendableHeight), constraints);

        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.weightx = 0.01;
        constraints.weighty = 0.6;
        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(WorldcoinWalletTitledPanel.createStent(BALANCE_SPACER), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.01;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(WorldcoinWalletTitledPanel.createStent(BALANCE_SPACER, BALANCE_SPACER - 2), constraints);

        estimatedBalanceWDCLabel = new BlinkLabel(controller, true);
        estimatedBalanceWDCLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel.tooltip")));
        estimatedBalanceWDCLabel.setBorder(BorderFactory.createEmptyBorder());

        constraints.gridx = 5;
        constraints.gridy = 1;
        constraints.weightx = 0.6;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(estimatedBalanceWDCLabel, constraints);

        estimatedBalanceFiatLabel = new BlinkLabel(controller, true);
        estimatedBalanceFiatLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel.tooltip")));
        estimatedBalanceFiatLabel.setBorder(BorderFactory.createEmptyBorder());

        constraints.gridx = 6;
        constraints.gridy = 0;
        constraints.weightx = 0.01;
        constraints.weighty = 0.6;
        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(WorldcoinWalletTitledPanel.createStent(12), constraints);

        constraints.gridx = 7;
        constraints.gridy = 1;
        constraints.weightx = 0.6;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(estimatedBalanceFiatLabel, constraints);

        Action availableBalanceHelpAction = new HelpContextAction(controller, null, "worldcoinWalletFrame.availableToSpend2",
                "worldcoinWalletFrame.availableToSpend.tooltip", "worldcoinWalletFrame.helpMenuText", HelpContentsPanel.HELP_AVAILABLE_TO_SPEND_URL);
        if (controller.getLocaliser().getLocale().getLanguage().equals("en")) {
            availableBalanceHelpAction.putValue(Action.NAME, SPENDABLE_TEXT_IN_ENGLISH);
        }
        availableBalanceLabelButton = new HelpButton(availableBalanceHelpAction, controller);
        availableBalanceLabelButton.setHorizontalAlignment(JLabel.RIGHT);
        availableBalanceLabelButton.setBorder(BorderFactory.createEmptyBorder());
        
        String tooltipText = HelpContentsPanel.createMultilineTooltipText(new String[] {
                controller.getLocaliser().getString("worldcoinWalletFrame.availableToSpend.tooltip"), "\n",
                controller.getLocaliser().getString("worldcoinWalletFrame.helpMenuTooltip") });
        availableBalanceLabelButton.setToolTipText(tooltipText);
        availableBalanceLabelButton.setBorder(BorderFactory.createEmptyBorder());

        constraints.gridx = 3;
        constraints.gridy = 3;
        constraints.weightx = 0.6;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;

        constraints.anchor = GridBagConstraints.LINE_END;
        headerPanel.add(availableBalanceLabelButton, constraints);
        headerPanel.add(WorldcoinWalletTitledPanel.createStent(spendableWidth, spendableHeight), constraints);

        constraints.gridx = 5;
        constraints.gridy = 3;
        constraints.weightx = 0.6;
        constraints.weighty = 0.01;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        availableBalanceWDCButton = new HelpButton(availableBalanceHelpAction, controller);
        availableBalanceWDCButton.setBorder(BorderFactory.createEmptyBorder());
        availableBalanceWDCButton.setToolTipText(tooltipText);

        headerPanel.add(availableBalanceWDCButton, constraints);

        constraints.gridx = 7;
        constraints.gridy = 3;
        constraints.weightx = 0.6;
        constraints.weighty = 0.01;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;

        constraints.anchor = GridBagConstraints.LINE_START;
        availableBalanceFiatButton = new HelpButton(availableBalanceHelpAction, controller);
        availableBalanceFiatButton.setBorder(BorderFactory.createEmptyBorder());
        availableBalanceFiatButton.setToolTipText(tooltipText);

        //availableBalanceFiatButton.setBorder(BorderFactory.createLineBorder(Color.RED));

        headerPanel.add(availableBalanceFiatButton, constraints);

        // Bottom Filler
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 0.01;
        constraints.weighty = 10;
        constraints.fill = GridBagConstraints.BOTH;

        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(WorldcoinWalletTitledPanel.createStent(BALANCE_SPACER, BALANCE_SPACER), constraints);

        JPanel forcerBottom = new JPanel();
        forcerBottom.setOpaque(false);
        //forcerBottom.setBorder(BorderFactory.createLineBorder(Color.CYAN));
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 0.01;
        constraints.weighty = 1000;

        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(forcerBottom, constraints);

        JPanel forcer1 = new JPanel();
        forcer1.setOpaque(false);
        //forcer1.setBorder(BorderFactory.createLineBorder(Color.CYAN));

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 8;
        constraints.gridy = 2;
        constraints.weightx = 1000;
        constraints.weighty = 1000;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        headerPanel.add(forcer1, constraints);

        JPanel forcer2 = new JPanel();
        forcer2.setOpaque(false);
        //forcer2.setBorder(BorderFactory.createLineBorder(Color.YELLOW));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 8;
        constraints.gridy = 1;
        constraints.weightx = 10000;
        constraints.weighty = 0.01;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        headerPanel.add(forcer2, constraints);

        // Initially invisible.
        availableBalanceLabelButton.setVisible(false);
        availableBalanceLabelButton.setEnabled(false);
        availableBalanceWDCButton.setVisible(false);
        availableBalanceWDCButton.setEnabled(false);
        availableBalanceFiatButton.setVisible(false);
        availableBalanceFiatButton.setEnabled(false);

        JPanel filler3 = new JPanel();
        filler3.setOpaque(false);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 8;
        constraints.gridy = 0;
        constraints.weightx = 1000;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 2;

        constraints.anchor = GridBagConstraints.LINE_START;
        headerPanel.add(filler3, constraints);

        // Add ticker panel.
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 9;
        constraints.gridy = 0;
        constraints.weightx = 10;
        constraints.weighty = 10000;
        constraints.gridwidth = 1;
        constraints.gridheight = 5;

        constraints.anchor = GridBagConstraints.CENTER;
        headerPanel.add(tickerTablePanel, constraints);

        // Add a little stent to keep it off the right hand edge.
        int stent = 6; // A reasonable default.
        Insets tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets");
        if (tabAreaInsets != null) {
            stent = tabAreaInsets.right;
        }
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 10;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
        headerPanel.add(WorldcoinWalletTitledPanel.createStent(stent), constraints);

        return headerPanel;
    }

    /**
     * @param constraints
     * @param contentPane
     */
    private void addMenuBar(GridBagConstraints constraints, Container contentPane) {
        ComponentOrientation componentOrientation = ComponentOrientation.getOrientation(controller.getLocaliser().getLocale());

        // Create the menu bar.
        JMenuBar menuBar = new JMenuBar();
        menuBar.setComponentOrientation(componentOrientation);
        menuBar.setOpaque(false);

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());

        // Build the File menu.
        JMenu fileMenu = new JMenu(localiser.getString("worldcoinWalletFrame.fileMenuText"));
        fileMenu.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        fileMenu.setComponentOrientation(componentOrientation);
        fileMenu.setMnemonic(mnemonicUtil.getMnemonic("worldcoinWalletFrame.fileMenuMnemonic"));
        tweakAppearance(fileMenu);
        menuBar.add(fileMenu);

        // Build the Trade menu.
        JMenu tradeMenu = new JMenu(localiser.getString("worldcoinWalletFrame.tradeMenuText"));
        tradeMenu.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        tradeMenu.setComponentOrientation(componentOrientation);
        tradeMenu.setMnemonic(mnemonicUtil.getMnemonic("worldcoinWalletFrame.tradeMenuMnemonic"));
        tweakAppearance(tradeMenu);
        menuBar.add(tradeMenu);

        // Build the View menu.
        JMenu viewMenu = new JMenu(localiser.getString("worldcoinWalletFrame.viewMenuText"));
        viewMenu.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        viewMenu.setComponentOrientation(componentOrientation);
        viewMenu.setMnemonic(mnemonicUtil.getMnemonic("worldcoinWalletFrame.viewMenuMnemonic"));
        tweakAppearance(viewMenu);
        menuBar.add(viewMenu);

        // Build the Tools menu.
        JMenu toolsMenu = new JMenu(localiser.getString("worldcoinWalletFrame.toolsMenuText"));
        toolsMenu.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        toolsMenu.setComponentOrientation(componentOrientation);
        toolsMenu.setMnemonic(mnemonicUtil.getMnemonic("worldcoinWalletFrame.toolsMenuMnemonic"));
        tweakAppearance(toolsMenu);
        menuBar.add(toolsMenu);

        // Build the Help menu.
        JMenu helpMenu = new JMenu(localiser.getString("worldcoinWalletFrame.helpMenuText"));
        helpMenu.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        helpMenu.setComponentOrientation(componentOrientation);
        helpMenu.setMnemonic(mnemonicUtil.getMnemonic("worldcoinWalletFrame.helpMenuMnemonic"));
        tweakAppearance(helpMenu);
        menuBar.add(helpMenu);
        // Create new wallet action.
        CreateWalletSubmitAction createNewWalletAction = new CreateWalletSubmitAction(this.worldcoinController,
                ImageLoader.createImageIcon(ImageLoader.CREATE_NEW_ICON_FILE), this);
        JMenuItem menuItem = new JMenuItem(createNewWalletAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        fileMenu.add(menuItem);

        // Open wallet action.
        OpenWalletAction openWalletAction = new OpenWalletAction(this.worldcoinController,
                ImageLoader.createImageIcon(ImageLoader.OPEN_WALLET_ICON_FILE), this);
        menuItem = new JMenuItem(openWalletAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        fileMenu.add(menuItem);

        CloseWalletAction closeWalletAction = new CloseWalletAction(this.worldcoinController,
                ImageLoader.createImageIcon(ImageLoader.CLOSE_WALLET_ICON_FILE), this);
        menuItem = new JMenuItem(closeWalletAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        fileMenu.add(menuItem);

        fileMenu.addSeparator();
        
        // Add password action.
        addPasswordAction = new WorldcoinWalletBusyAction(this.worldcoinController, ImageLoader.ADD_PASSWORD_ICON_FILE, "addPasswordAction.text",
                "addPasswordAction.tooltip", "addPasswordAction.mnemonic", View.ADD_PASSWORD_VIEW);
        menuItem = new JMenuItem(addPasswordAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        fileMenu.add(menuItem);

        // Change password action.
        changePasswordAction = new WorldcoinWalletBusyAction(this.worldcoinController, ImageLoader.CHANGE_PASSWORD_ICON_FILE, "changePasswordAction.text",
                "changePasswordAction.tooltip", "changePasswordAction.mnemonic", View.CHANGE_PASSWORD_VIEW);
        menuItem = new JMenuItem(changePasswordAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        fileMenu.add(menuItem);

        // Remove password action.
        removePasswordAction = new WorldcoinWalletBusyAction(this.worldcoinController, ImageLoader.REMOVE_PASSWORD_ICON_FILE, "removePasswordAction.text",
                "removePasswordAction.tooltip", "removePasswordAction.mnemonic", View.REMOVE_PASSWORD_VIEW);
        menuItem = new JMenuItem(removePasswordAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        fileMenu.add(menuItem);

        // Exit action.
        if (application != null && !application.isMac()) {
            // Non Macs have an Exit Menu item.
            fileMenu.addSeparator();
            {
                AbstractAction exitAction = new AbstractExitAction(this.controller) {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        quitEventListener.onQuitEvent(null, worldcoinWalletFrameQuitResponse);
                    }
                };
                
                menuItem = new JMenuItem(exitAction);
            }
            menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
            menuItem.setComponentOrientation(componentOrientation);
            fileMenu.add(menuItem);
        }

        // Show welcome action.
        WorldcoinWalletAction showWelcomeAction = new WorldcoinWalletAction(controller, ImageLoader.WELCOME_ICON_FILE, "welcomePanel.text",
                "welcomePanel.title", "welcomePanel.mnemonic", View.WELCOME_VIEW);
        menuItem = new JMenuItem(showWelcomeAction);
        showWelcomeAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("welcomePanel.title")));

        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        helpMenu.add(menuItem);

        WorldcoinWalletAction showHelpContentsAction;
        if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
            showHelpContentsAction = new WorldcoinWalletAction(controller, ImageLoader.HELP_CONTENTS_ICON_FILE,
                "showHelpContentsAction.text", "showHelpContentsAction.tooltip", "showHelpContentsAction.mnemonic",
                View.HELP_CONTENTS_VIEW);
        } else {
            showHelpContentsAction = new WorldcoinWalletAction(controller, ImageLoader.HELP_CONTENTS_RTL_ICON_FILE,
                    "showHelpContentsAction.text", "showHelpContentsAction.tooltip", "showHelpContentsAction.mnemonic",
                    View.HELP_CONTENTS_VIEW);
        }
        showHelpContentsAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("showHelpContentsAction.tooltip")));

        menuItem = new JMenuItem(showHelpContentsAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        helpMenu.add(menuItem);

        if (application != null && !application.isMac()) {
            // Non Macs have a Help About menu item.
            WorldcoinWalletAction helpAboutAction = new WorldcoinWalletAction(controller, ImageLoader.WORLDCOIN_WALLET_SMALL_ICON_FILE,
                    "helpAboutAction.text", "helpAboutAction.tooltip", "helpAboutAction.mnemonic", View.HELP_ABOUT_VIEW);
            helpAboutAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("helpAboutAction.tooltip")));

            menuItem = new JMenuItem(helpAboutAction);
            menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
            menuItem.setComponentOrientation(componentOrientation);
            helpMenu.add(menuItem);
        }

        // View Transactions action.
        WorldcoinWalletAction showTransactionsAction = new WorldcoinWalletAction(controller, ImageLoader.TRANSACTIONS_ICON_FILE,
                "showTransactionsAction.text", "showTransactionsAction.tooltip", "showTransactionsAction.mnemonic",
                View.TRANSACTIONS_VIEW);
        showTransactionsAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("showTransactionsAction.tooltip")));

        menuItem = new JMenuItem(showTransactionsAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        viewMenu.add(menuItem);

        // View Charts action.
        WorldcoinWalletAction showChartsAction = new WorldcoinWalletAction(controller, ImageLoader.CHART_LINE_ICON_FILE,
                "chartsPanelAction.text", "chartsPanelAction.tooltip", "chartsPanelAction.mnemonic",
                View.CHARTS_VIEW);
        showChartsAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("chartsPanelAction.tooltip")));

        menuItem = new JMenuItem(showChartsAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        viewMenu.add(menuItem);

        // Show messages action.
        WorldcoinWalletAction showMessagesAction = new WorldcoinWalletAction(controller, ImageLoader.MESSAGES_ICON_FILE, "messagesPanel.text",
                "messagesPanel.tooltip", "messagesPanel.mnemonic", View.MESSAGES_VIEW);
        showMessagesAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("messagesPanel.tooltip")));

        menuItem = new JMenuItem(showMessagesAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        viewMenu.add(menuItem);

        // Send worldcoin action.
        WorldcoinWalletAction sendWorldcoinAction = new WorldcoinWalletAction(controller, ImageLoader.SEND_WORLDCOIN_ICON_FILE,
                "sendWorldcoinAction.text", "sendWorldcoinAction.tooltip", "sendWorldcoinAction.mnemonic", View.SEND_WORLDCOIN_VIEW);
        sendWorldcoinAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("sendWorldcoinAction.tooltip")));

        menuItem = new JMenuItem(sendWorldcoinAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        tradeMenu.add(menuItem);

        WorldcoinWalletAction receiveWorldcoinAction = new WorldcoinWalletAction(controller, ImageLoader.RECEIVE_WORLDCOIN_ICON_FILE,
                "receiveWorldcoinAction.text", "receiveWorldcoinAction.tooltip", "receiveWorldcoinAction.mnemonic",
                View.RECEIVE_WORLDCOIN_VIEW);
        receiveWorldcoinAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("receiveWorldcoinAction.tooltip")));

        menuItem = new JMenuItem(receiveWorldcoinAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        tradeMenu.add(menuItem);

        // Show preferences.
        if (application != null && !application.isMac()) {
            // Non Macs have a Preferences menu item.
            WorldcoinWalletAction showPreferencesAction = new WorldcoinWalletAction(controller, ImageLoader.PREFERENCES_ICON_FILE,
                    "showPreferencesAction.text", "showPreferencesAction.tooltip", "showPreferencesAction.mnemonic",
                    View.PREFERENCES_VIEW);
            showPreferencesAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("showPreferencesAction.tooltip")));

            menuItem = new JMenuItem(showPreferencesAction);
            menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
            menuItem.setComponentOrientation(componentOrientation);
            viewMenu.add(menuItem);
        }

        viewMenu.addSeparator();

        // Show ticker.
        String viewTicker = controller.getModel().getUserPreference(ExchangeModel.TICKER_SHOW);
        boolean isTickerVisible = !Boolean.FALSE.toString().equals(viewTicker);

        String tickerKey;
        if (isTickerVisible) {
            tickerKey = "worldcoinWalletFrame.ticker.hide.text";
        } else {
            tickerKey = "worldcoinWalletFrame.ticker.show.text";

        }
        final JMenuItem showTicker = new JMenuItem(controller.getLocaliser().getString(tickerKey));
        showTicker.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        showTicker.setComponentOrientation(componentOrientation);
        showTicker.setIcon(ImageLoader.createImageIcon(ImageLoader.MONEY_ICON_FILE));

        if (tickerTablePanel != null) {
            tickerTablePanel.setVisible(isTickerVisible);
        }
        
        final WorldcoinWalletFrame thisFrame = this;

        showTicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (tickerTablePanel != null) {
                    if (tickerTablePanel.isVisible()) {
                        tickerTablePanel.setVisible(false);
                        controller.getModel().setUserPreference(ExchangeModel.TICKER_SHOW, Boolean.FALSE.toString());
                        showTicker.setText(controller.getLocaliser().getString("worldcoinWalletFrame.ticker.show.text"));
                    } else {
                        tickerTablePanel.setVisible(true);
                        controller.getModel().setUserPreference(ExchangeModel.TICKER_SHOW, Boolean.TRUE.toString());
                        showTicker.setText(controller.getLocaliser().getString("worldcoinWalletFrame.ticker.hide.text"));
                        // Cancel any existing timer.
                        if (tickerTimer1 != null) {
                            tickerTimer1.cancel();
                        }                        
                        if (tickerTimer2 != null) {
                            tickerTimer2.cancel();
                        }
                        // Start ticker timer.
                        tickerTimer1 = new Timer();
                        tickerTimer1.schedule(new TickerTimerTask(exchangeController, thisFrame, true), 0, TickerTimerTask.DEFAULT_REPEAT_RATE);
                        
                        boolean showSecondRow = Boolean.TRUE.toString().equals(
                                controller.getModel().getUserPreference(ExchangeModel.TICKER_SHOW_SECOND_ROW));
                        
                        if (showSecondRow) {
                            tickerTimer2 = new Timer();
                            tickerTimer2.schedule(new TickerTimerTask(exchangeController, thisFrame, false), TickerTimerTask.TASK_SEPARATION, TickerTimerTask.DEFAULT_REPEAT_RATE);
                        }
                    }
                }
            }
        });

        viewMenu.add(showTicker);
        
        // Sign message.
        signMessageAction = new WorldcoinWalletBusyAction(this.worldcoinController, ImageLoader.MESSAGE_SIGN_ICON_FILE, "signMessageAction.text",
                "signMessageAction.tooltip", "signMessageAction.mnemonic", View.SIGN_MESSAGE_VIEW);
        menuItem = new JMenuItem(signMessageAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        toolsMenu.add(menuItem);

        // Verify message.
        WorldcoinWalletAction verifyMessageAction = new WorldcoinWalletAction(this.worldcoinController, ImageLoader.MESSAGE_VERIFY_ICON_FILE, "verifyMessageAction.text",
                "verifyMessageAction.tooltip", "verifyMessageAction.mnemonic", View.VERIFY_MESSAGE_VIEW);
        verifyMessageAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("verifyMessageAction.tooltip")));

        menuItem = new JMenuItem(verifyMessageAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        toolsMenu.add(menuItem);

        toolsMenu.addSeparator();

        // Import private keys.
        showImportPrivateKeysAction = new WorldcoinWalletBusyAction(this.worldcoinController, ImageLoader.IMPORT_PRIVATE_KEYS_ICON_FILE,
                "showImportPrivateKeysAction.text", "showImportPrivateKeysAction.tooltip", "showImportPrivateKeysAction.mnemonic",
                View.SHOW_IMPORT_PRIVATE_KEYS_VIEW);
        menuItem = new JMenuItem(showImportPrivateKeysAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        toolsMenu.add(menuItem);

        // Export private keys.
        showExportPrivateKeysAction = new WorldcoinWalletBusyAction(this.worldcoinController, ImageLoader.EXPORT_PRIVATE_KEYS_ICON_FILE,
                "showExportPrivateKeysAction.text", "showExportPrivateKeysAction.tooltip", "showExportPrivateKeysAction.mnemonic",
                View.SHOW_EXPORT_PRIVATE_KEYS_VIEW);
        menuItem = new JMenuItem(showExportPrivateKeysAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        toolsMenu.add(menuItem);

        toolsMenu.addSeparator();

        resetTransactionsAction = new WorldcoinWalletBusyAction(this.worldcoinController, ImageLoader.RESET_TRANSACTIONS_ICON_FILE,
                "resetTransactionsAction.text", "resetTransactionsAction.tooltip", "resetTransactionsAction.mnemonic",
                View.RESET_TRANSACTIONS_VIEW);
        resetTransactionsAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("resetTransactionsAction.tooltip")));

        menuItem = new JMenuItem(resetTransactionsAction);
        menuItem.setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());
        menuItem.setComponentOrientation(componentOrientation);
        toolsMenu.add(menuItem);

        setJMenuBar(menuBar);

        return;
    }

    private void tweakAppearance(JMenu menu) {
        menu.setMargin(new Insets(MENU_HORIZONTAL_INSET, MENU_VERTICAL_INSET, MENU_HORIZONTAL_INSET, MENU_VERTICAL_INSET));
    }

    /**
     * Recreate all views.
     */
    @Override
    public void recreateAllViews(final boolean initUI, final View initialView) {
        // if initUI set, do an invokeLater or else it can sometimes leave the menu items in the Mac header row.
        if (EventQueue.isDispatchThread() && !initUI) {
            recreateAllViewsOnSwingThread(initUI, initialView);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    recreateAllViewsOnSwingThread(initUI, initialView);
                }
            });
        }
    }

    private void recreateAllViewsOnSwingThread(final boolean initUI, View initialView) {
        ColorAndFontConstants.init();

        // Close down current view.
        if (controller.getCurrentView() != View.UNKNOWN_VIEW) {
            navigateAwayFromView(controller.getCurrentView());
        }

        if (initUI) {
            // Remember and replay task and remove any listeners.
            List<WalletData> replayPerWalletModelDataList = null;
            if (ReplayManager.INSTANCE.getCurrentReplayTask() != null) {
                replayPerWalletModelDataList = ReplayManager.INSTANCE.getCurrentReplayTask().getPerWalletModelDataToReplay();
            }
            ReplayManager.INSTANCE.removeDownloadListeners(replayPerWalletModelDataList);
            
            // Remove the WalletBusyListeners.
            this.worldcoinController.logNumberOfWalletBusyListeners();
            this.worldcoinController.clearWalletBusyListeners();
            
            this.localiser = controller.getLocaliser();
            Container contentPane = getContentPane();
            viewFactory.initialise();
            contentPane.removeAll();
            viewTabbedPane.removeAllTabs();
            initUI(null);
            
            // TODO check task is still running by taskid.
            if (replayPerWalletModelDataList != null) {
                ReplayManager.INSTANCE.addDownloadListeners(replayPerWalletModelDataList);
            }
                
            if (initialView != null && !(initialView == View.TRANSACTIONS_VIEW) && !(initialView == View.SEND_WORLDCOIN_VIEW)
                    && !(initialView == View.RECEIVE_WORLDCOIN_VIEW)) {
                JPanel currentTabPanel = new JPanel(new BorderLayout());
                Viewable currentView = viewFactory.getView(initialView);
                currentTabPanel.add((JPanel) currentView, BorderLayout.CENTER);
                viewTabbedPane.addTab(currentView.getViewTitle(), currentView.getViewIcon(), currentView.getViewTooltip(),
                        currentTabPanel, true);
            }
 
            try {
                applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
            } catch (ClassCastException cce) {
                // Look and feel exception - ignore.
            }
        }

        statusBar.refreshOnlineStatusText();

        updateHeader();

        // Tell the wallets list to display.
        if (walletsView != null) {
            walletsView.displayView(DisplayHint.COMPLETE_REDRAW);
        }

        if (tickerTablePanel != null) {
            tickerTablePanel.update();
        }

        // Tell all the tabs in the tabbedPane to update.
        if (viewTabbedPane != null) {
            for (int i = 0; i < viewTabbedPane.getTabCount(); i++) {
                JPanel tabComponent = (JPanel) viewTabbedPane.getComponentAt(i);
                Component[] components = tabComponent.getComponents();
                if (components != null && components.length > 0 && components[0] instanceof Viewable) {
                    Viewable loopView = ((Viewable) components[0]);
                    loopView.displayView(DisplayHint.COMPLETE_REDRAW);
                    if (initialView != null && loopView.getViewId().toString().equals(initialView.toString())) {
                        viewTabbedPane.setSelectedIndex(i);
                    }
                }
            }
        }
        
        this.worldcoinController.logNumberOfWalletBusyListeners();
    }

    /**
     * Display next view.
     */
    @Override
    public void displayView(View viewToDisplay) {
        log.debug("Displaying view '" + viewToDisplay + "'");
        // Open wallet view obselete - show transactions
        if (View.OPEN_WALLET_VIEW == viewToDisplay) {
            viewToDisplay = View.TRANSACTIONS_VIEW;
        }
        // Create Bulk addreses obselete - show transactions
        if (View.CREATE_BULK_ADDRESSES_VIEW == viewToDisplay) {
            viewToDisplay = View.TRANSACTIONS_VIEW;
        }

        // Show wallets view always on display.
        if (View.YOUR_WALLETS_VIEW == viewToDisplay) {
            walletsView.displayView(DisplayHint.COMPLETE_REDRAW);
            return;
        }

        controller.setCurrentView(viewToDisplay);

        final Viewable nextViewFinal = viewFactory.getView(viewToDisplay);

        if (nextViewFinal == null) {
            log.debug("Cannot display view " + viewToDisplay);
            return;
        }

        if (EventQueue.isDispatchThread()) {
            displayViewOnSwingThread(nextViewFinal);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    displayViewOnSwingThread(nextViewFinal);
                }
            });
        }
    }
    
    private void displayViewOnSwingThread(final Viewable nextViewFinal) {
        String viewName = nextViewFinal.getViewId().toString();
        boolean foundTab = false;
        if (viewTabbedPane.getTabCount() > 0) {
            //log.debug("viewTabbedPane " + System.identityHashCode(viewTabbedPane) + " initally has " + viewTabbedPane.getTabCount() + " tabs.");
            for (int i = 0; i < viewTabbedPane.getTabCount(); i++) {
                JPanel tabComponent = (JPanel) viewTabbedPane.getComponentAt(i);
                if (tabComponent != null) {
                    Component[] childComponents = tabComponent.getComponents();
                    String tabName = null;
                    if (childComponents != null && childComponents.length > 0 && childComponents[0] instanceof Viewable) {
                        tabName= ((Viewable) childComponents[0]).getViewId().toString();
                    }
                    if (viewName != null && viewName.equals(tabName)) {
                        foundTab = true;
                        ((JPanel) viewTabbedPane.getComponentAt(i)).removeAll();
                        ((JPanel) viewTabbedPane.getComponentAt(i)).add((JPanel) nextViewFinal);
                        viewTabbedPane.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        if (!foundTab && nextViewFinal instanceof JPanel) {
            JPanel tabOutlinePanel = new JPanel(new BorderLayout());
            tabOutlinePanel.add((JPanel) nextViewFinal, BorderLayout.CENTER);
            viewTabbedPane.addTab(nextViewFinal.getViewTitle(), nextViewFinal.getViewIcon(),
                    nextViewFinal.getViewTooltip(), tabOutlinePanel, true);
            viewTabbedPane.setSelectedComponent(tabOutlinePanel);
        }

        nextViewFinal.displayView(DisplayHint.COMPLETE_REDRAW);

        //log.debug("viewTabbedPane " + System.identityHashCode(viewTabbedPane) + " finally has " + viewTabbedPane.getTabCount() + " tabs.");
        this.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Navigate away from view - this may be on another thread hence the
     * SwingUtilities.invokeLater.
     */
    @Override
    public void navigateAwayFromView(View viewToNavigateAwayFrom) {
        if (View.YOUR_WALLETS_VIEW == viewToNavigateAwayFrom) {
            // Do nothing
            return;
        }

        final Viewable viewToNavigateAwayFromFinal = viewFactory.getView(viewToNavigateAwayFrom);

        if (viewToNavigateAwayFromFinal != null) {
            if (EventQueue.isDispatchThread()) {
                viewToNavigateAwayFromFinal.navigateAwayFromView();
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        viewToNavigateAwayFromFinal.navigateAwayFromView();
                    }
                });
            }
        }
    }

    @Override
    public void setOnlineStatus(StatusEnum statusEnum) {
        online = statusEnum;
        if (statusBar != null) {
            statusBar.updateOnlineStatusText(statusEnum);
        }    
    }
    

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        if (EventQueue.isDispatchThread()) {
            updateMenuItemsOnWalletChange();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateMenuItemsOnWalletChange();
                }
            });
        }
    }
    
    private void updateMenuItemsOnWalletChange() {
        signMessageAction.setEnabled(!this.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
        showImportPrivateKeysAction.setEnabled(!this.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
        showExportPrivateKeysAction.setEnabled(!this.worldcoinController.getModel().getActivePerWalletModelData().isBusy());
        resetTransactionsAction.setEnabled(!this.worldcoinController.getModel().getActivePerWalletModelData().isBusy());

        if (this.worldcoinController.getModel().getActiveWallet() == null) {
            // Cannot do anything password related.
            addPasswordAction.setEnabled(false);
            changePasswordAction.setEnabled(false);
            removePasswordAction.setEnabled(false);
        } else {
            if (this.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
                addPasswordAction.setEnabled(false);
                changePasswordAction.setEnabled(false);
                removePasswordAction.setEnabled(false);
            } else {
                if (this.worldcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
                    addPasswordAction.setEnabled(false);
                    changePasswordAction.setEnabled(true);
                    removePasswordAction.setEnabled(true);
                } else {
                    if (this.worldcoinController.getModel().getActiveWalletInfo() == null ||
                            this.worldcoinController.getModel().getActiveWalletInfo().getWalletVersion() == WorldcoinWalletVersion.SERIALIZED) {
                        addPasswordAction.setEnabled(false);
                    } else {
                        addPasswordAction.setEnabled(true);
                    }
                    changePasswordAction.setEnabled(false);
                    removePasswordAction.setEnabled(false);
                }
            }
        }
        
        if (this.worldcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            String walletIsBusyText = HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("worldcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.worldcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())}));
            signMessageAction.putValue(Action.SHORT_DESCRIPTION, walletIsBusyText);
            showImportPrivateKeysAction.putValue(Action.SHORT_DESCRIPTION, walletIsBusyText);
            showExportPrivateKeysAction.putValue(Action.SHORT_DESCRIPTION, walletIsBusyText);
            resetTransactionsAction.putValue(Action.SHORT_DESCRIPTION, walletIsBusyText);
            addPasswordAction.putValue(Action.SHORT_DESCRIPTION, walletIsBusyText);
            changePasswordAction.putValue(Action.SHORT_DESCRIPTION, walletIsBusyText);
            removePasswordAction.putValue(Action.SHORT_DESCRIPTION, walletIsBusyText);

        } else {
            signMessageAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("signMessageAction.tooltip")));
            showImportPrivateKeysAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("showImportPrivateKeysAction.tooltip")));
            showExportPrivateKeysAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("showExportPrivateKeysAction.tooltip")));
            resetTransactionsAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("resetTransactionsAction.tooltip")));
            addPasswordAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("addPasswordAction.tooltip")));
            changePasswordAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("changePasswordAction.tooltip")));
            removePasswordAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipTextForMenuItem(controller.getLocaliser().getString("removePasswordAction.tooltip")));            
        }
    }

    @Override
    /**
     * Update due to a block being downloaded.
     * This typically comes in from a Peer.
     */
    public void blockDownloaded() {
        // Update transaction screen in case status icons have changed.
        if (View.TRANSACTIONS_VIEW == controller.getCurrentView()) {
            ShowTransactionsPanel.updateTransactions();
        }
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction transaction, BigInteger prevBalance, BigInteger newBalance) {
        fireDataChangedUpdateLater(DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED);
    }

    @Override
    public void onCoinsSent(Wallet wallet, Transaction transaction, BigInteger prevBalance, BigInteger newBalance) {
        fireDataChangedUpdateLater(DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED);
    }

    /**
     * One of the wallets has been reorganised due to a block chain reorganise
     */
    @Override
    public void onReorganize(Wallet wallet) {
        log.info("WorldcoinWallet has been reorganised.");
        recreateAllViews(false, controller.getCurrentView());
    }

    @Override
    public void onTransactionConfidenceChanged(Wallet wallet, Transaction transaction) {
        if (controller.getCurrentView() == View.TRANSACTIONS_VIEW) {
            ShowTransactionsPanel.updateTransactions(); 
        } else if (controller.getCurrentView() == View.SEND_WORLDCOIN_VIEW) {
            final int numberOfPeers = (transaction == null || transaction.getConfidence() == null) ? 0 : transaction.getConfidence().getBroadcastByCount();
            //log.debug("numberOfPeers = " + numberOfPeers);
            final Sha256Hash transactionHash = (transaction == null) ? null : transaction.getHash();
            //log.debug((transaction != null && transaction.getConfidence() != null) ? transaction.getConfidence().toString() : "No transaction confidence for tx");
            SendWorldcoinConfirmPanel.updatePanelDueToTransactionConfidenceChange(transactionHash, numberOfPeers);
        }
    }

    @Override
    public void fireFilesHaveBeenChangedByAnotherProcess(WalletData perWalletModelData) {
        if (this.worldcoinController.getModel().getActiveWalletFilename() != null
                && this.worldcoinController.getModel().getActiveWalletFilename().equals(perWalletModelData.getWalletFilename())) {
            Message message = new Message(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("singleWalletPanel.dataHasChanged.tooltip.1") + " "
                    + controller.getLocaliser().getString("singleWalletPanel.dataHasChanged.tooltip.2")), true);
            MessageManager.INSTANCE.addMessage(message);
        }
        fireDataChangedUpdateNow(DisplayHint.COMPLETE_REDRAW);
    }

    /**
     * Mark that the UI needs to be updated as soon as possible.
     */
    @Override
    public void fireDataChangedUpdateNow(final DisplayHint displayHint) {
        if (EventQueue.isDispatchThread()) {
            fireDataChangedOnSwingThread(displayHint);   
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireDataChangedOnSwingThread(displayHint); 
                }
            });
        }
    }  

    /**
     * Mark that the UI needs updating the next time the fireDataChangedTimer fires.
     */
    @Override
    public void fireDataChangedUpdateLater(DisplayHint displayHint) {
        if (fireDataChangedTimerTask != null) {
            fireDataChangedTimerTask.setFireDataChanged(true);
        }    
    }
    
    /**
     * Actually update the UI.
     * (Called back from the FireDataChangedTimerTask).
     */
    private void fireDataChangedOnSwingThread(DisplayHint displayHint) {
        updateHeader();

        // Update the password related menu items.
        updateMenuItemsOnWalletChange();
        
        // Tell the wallets list to display.
        if (walletsView != null) {
            walletsView.displayView(displayHint);
        }

        // Tell the current view to update itself.
        Viewable currentViewView = viewFactory.getView(controller.getCurrentView());
        if (currentViewView != null) {
            currentViewView.displayView(displayHint);
        }
    }

    /**
     * Update the Ticker Panel after the exchange data has changed.
     */
    public void fireExchangeDataChanged() {
        updateHeader();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tickerTablePanel.update();
            }
        });
    }

    public void updateHeader(final String syncMessage, final double syncPercent) {
        final boolean filesHaveBeenChangeByAnotherProcess = this.worldcoinController.getModel().getActivePerWalletModelData() != null && this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess();
        final boolean isBusy = this.worldcoinController.getModel().getActivePerWalletModelData().isBusy();
        if (EventQueue.isDispatchThread()) {
            updateHeaderOnSwingThread(filesHaveBeenChangeByAnotherProcess, null, null, isBusy, syncMessage, syncPercent);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateHeaderOnSwingThread(filesHaveBeenChangeByAnotherProcess, null, null, isBusy, syncMessage, syncPercent);
                }
            });
        }
    }
    
    public void updateHeader() {
        final BigInteger finalEstimatedBalance = this.worldcoinController.getModel().getActiveWalletEstimatedBalance();
        final BigInteger finalAvailableToSpend = this.worldcoinController.getModel().getActiveWalletAvailableBalance();
        final boolean filesHaveBeenChangeByAnotherProcess = this.worldcoinController.getModel().getActivePerWalletModelData() != null && this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess();
        final boolean isBusy = this.worldcoinController.getModel().getActivePerWalletModelData().isBusy();
        
        if (EventQueue.isDispatchThread()) {
            updateHeaderOnSwingThread(filesHaveBeenChangeByAnotherProcess, finalEstimatedBalance, finalAvailableToSpend, isBusy, null, -1);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateHeaderOnSwingThread(filesHaveBeenChangeByAnotherProcess, finalEstimatedBalance, finalAvailableToSpend, isBusy, null, -1);
                }
            });
        }
    }
        
    private void updateHeaderOnSwingThread(final boolean filesHaveBeenChangedByAnotherProcess, final BigInteger estimatedBalance, final BigInteger availableToSpend, final boolean isBusy, final String syncMessage, final double syncPercent) {
        if (filesHaveBeenChangedByAnotherProcess) {
            // Files have been changed by another process - blank totals
            // and put 'Updates stopped' message.
            estimatedBalanceLabelLabel.setText(controller.getLocaliser().getString("singleWalletPanel.dataHasChanged.text"));
            estimatedBalanceWDCLabel.setText(" ");
            estimatedBalanceFiatLabel.setText(" ");
            setUpdatesStoppedTooltip(estimatedBalanceLabelLabel);
            availableBalanceLabelButton.setText(" ");
            availableBalanceWDCButton.setText(" ");
            availableBalanceFiatButton.setText(" ");
        } else {
            if (isBusy) {
                estimatedBalanceLabelLabel.setText(" ");
                estimatedBalanceLabelLabel.setToolTipText(null);
                if (syncMessage != null && !syncMessage.isEmpty()) {
                    estimatedBalanceWDCLabel.setText(syncMessage);
                    estimatedBalanceWDCLabel.setToolTipText(syncMessage);
                }
                if (syncPercent > -1) {
                    estimatedBalanceFiatLabel.setText("(" + (int) syncPercent + "%)");
                    estimatedBalanceFiatLabel.setToolTipText(syncMessage);
                } else if (syncPercent == 0 || (syncMessage != null && !syncMessage.isEmpty())) {
                    estimatedBalanceFiatLabel.setText(" ");
                    estimatedBalanceFiatLabel.setToolTipText(null);
                } else {
                    estimatedBalanceFiatLabel.setToolTipText(null);
                }
                availableBalanceWDCButton.setText(" ");
                availableBalanceFiatButton.setText(" ");
                availableBalanceLabelButton.setEnabled(false);
                availableBalanceWDCButton.setEnabled(false);
                availableBalanceFiatButton.setEnabled(false);
                availableBalanceLabelButton.setVisible(false);
                availableBalanceWDCButton.setVisible(false);
                availableBalanceFiatButton.setVisible(false);
            } else {
                estimatedBalanceLabelLabel.setText(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel"));
                estimatedBalanceLabelLabel.setToolTipText(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel.tooltip"));
                estimatedBalanceWDCLabel.setText(controller.getLocaliser().worldcoinValueToString(estimatedBalance, true, false));
                estimatedBalanceWDCLabel.setToolTipText(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel.tooltip"));
                estimatedBalanceFiatLabel.setToolTipText(controller.getLocaliser().getString("worldcoinWalletFrame.balanceLabel.tooltip"));
                if (CurrencyConverter.INSTANCE.getRate() != null && CurrencyConverter.INSTANCE.isShowingFiat()) {
                    Money fiat = CurrencyConverter.INSTANCE.convertFromWDCToFiat(estimatedBalance);
                    estimatedBalanceFiatLabel.setText("(" + CurrencyConverter.INSTANCE.getFiatAsLocalisedString(fiat) + ")");
                } else {
                    estimatedBalanceFiatLabel.setText(" ");
                }

                if (availableToSpend != null && availableToSpend.equals(estimatedBalance)) {
                    availableBalanceWDCButton.setText(" ");
                    availableBalanceFiatButton.setText(" ");
                    availableBalanceLabelButton.setEnabled(false);
                    availableBalanceWDCButton.setEnabled(false);
                    availableBalanceFiatButton.setEnabled(false);
                    availableBalanceLabelButton.setVisible(false);
                    availableBalanceWDCButton.setVisible(false);
                    availableBalanceFiatButton.setVisible(false);
                } else {
                    availableBalanceWDCButton
                            .setText(controller.getLocaliser().worldcoinValueToString(availableToSpend, true, false));
                    if (CurrencyConverter.INSTANCE.getRate() != null && CurrencyConverter.INSTANCE.isShowingFiat()) {
                        Money fiat = CurrencyConverter.INSTANCE.convertFromWDCToFiat(availableToSpend);
                        if (fiat != null) {
                            availableBalanceFiatButton.setText("(" + CurrencyConverter.INSTANCE.getFiatAsLocalisedString(fiat)
                                    + ")");
                        }
                    } else {
                        availableBalanceFiatButton.setText(" ");
                    }
                    availableBalanceLabelButton.setEnabled(true);
                    availableBalanceWDCButton.setEnabled(true);
                    availableBalanceFiatButton.setEnabled(true);
                    availableBalanceLabelButton.setVisible(true);
                    availableBalanceWDCButton.setVisible(true);
                    availableBalanceFiatButton.setVisible(true);
                }
            }

            String titleText = localiser.getString("worldcoinWalletFrame.title");
            if (this.worldcoinController.getModel().getActiveWallet() != null) {
                titleText = titleText + SEPARATOR + this.worldcoinController.getModel().getActivePerWalletModelData().getWalletDescription()
                        + SEPARATOR + this.worldcoinController.getModel().getActivePerWalletModelData().getWalletFilename();
            }
            setTitle(titleText);
        }
    }

    // Macify application methods.

    @Override
    @Deprecated
    public void handleAbout(ApplicationEvent event) {
        controller.displayView(View.HELP_ABOUT_VIEW);
        event.setHandled(true);
    }

    @Override
    @Deprecated
    public void handleOpenApplication(ApplicationEvent event) {
        // Ok, we know our application started.
        // Not much to do about that..
    }

    @Override
    @Deprecated
    public void handleOpenFile(ApplicationEvent event) {
        // TODO i18n required.
        JOptionPane.showMessageDialog(this, "Sorry, opening of files with double click is not yet implemented.  WorldcoinWallet was "
                + event.getFilename());
    }

    @Override
    @Deprecated
    public void handlePreferences(ApplicationEvent event) {
        controller.displayView(View.PREFERENCES_VIEW);
    }

    @Override
    @Deprecated
    public void handlePrintFile(ApplicationEvent event) {
        // TODO i18n required.
        JOptionPane.showMessageDialog(this, "Sorry, printing not implemented");
    }

    @Override
    @Deprecated
    public void handleQuit(ApplicationEvent event) {
        throw new UnsupportedOperationException("Deprecated.");
    }

    @Override
    public void handleReOpenApplication(ApplicationEvent event) {
        setVisible(true);
    }

    public void setUpdatesStoppedTooltip(JComponent component) {
        // Multiline tool tip text.
        String toolTipText = "<html><font face=\"sansserif\">";
        toolTipText = toolTipText + controller.getLocaliser().getString("singleWalletPanel.dataHasChanged.tooltip.1") + "<br>";
        toolTipText = toolTipText + controller.getLocaliser().getString("singleWalletPanel.dataHasChanged.tooltip.2") + "<br>";
        toolTipText = toolTipText + "</font></html>";
        component.setToolTipText(toolTipText);
    }

    public void bringToFront() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                toFront();
                repaint();
            }
        });
    }
    
    public void calculateDividerPosition() {
        int dividerPosition = SingleWalletPanel.calculateNormalWidth((JComponent) (walletsView)) + WALLET_WIDTH_DELTA;
        if (((WalletListPanel) walletsView).getScrollPane().getVerticalScrollBar().isVisible()) {
            dividerPosition += SCROLL_BAR_DELTA;
        }
        if (walletsView != null && walletsView.getPreferredSize() != null && walletsView.getPreferredSize().width > dividerPosition) {
            dividerPosition = walletsView.getPreferredSize().width;
        }
        
        if (ComponentOrientation.RIGHT_TO_LEFT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
            int width = getWidth();
            if (width == 0) {
                width = (int) this.getPreferredSize().getWidth();
            }
            dividerPosition = width - dividerPosition; // - WalletListPanel.LEFT_BORDER - WalletListPanel.RIGHT_BORDER - 2;
        } 
        splitPane.setEnabled(true);
        splitPane.setDividerLocation(dividerPosition);
        splitPane.setEnabled(ComponentOrientation.LEFT_TO_RIGHT.equals(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())));
    }

    public WalletListPanel getWalletsView() {
        return walletsView;
    }

    public void onDeadTransaction(Wallet wallet, Transaction deadTx, Transaction replacementTx) {
    }

    public JPanel getHeaderPanel() {
        return headerPanel;
    }

    public TickerTablePanel getTickerTablePanel() {
        return tickerTablePanel;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    @Override
    public void onWalletChanged(Wallet wallet) {
        fireDataChangedUpdateNow(DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED);
    }

    @Override
    public void lostExchangeRate(ExchangeRate exchangeRate) {
        updateHeader();
    }

    @Override
    public void foundExchangeRate(ExchangeRate exchangeRate) {
        updateHeader();
    }

    @Override
    public void updatedExchangeRate(ExchangeRate exchangeRate) {
        updateHeader();
    }

    public Timer getTickerTimer1() {
        return tickerTimer1;
    }

    public void setTickerTimer1(Timer tickerTimer1) {
        this.tickerTimer1 = tickerTimer1;
    }
    
    public Timer getTickerTimer2() {
        return tickerTimer2;
    }

    public void setTickerTimer2(Timer tickerTimer2) {
        this.tickerTimer2 = tickerTimer2;
    }
    
    public TickerTimerTask getTickerTimerTask1() {
        return tickerTimerTask1;
    }
    public TickerTimerTask getTickerTimerTask2() {
        return tickerTimerTask2;
    }

    public void setTickerTimerTask1(TickerTimerTask tickerTimerTask1) {
        this.tickerTimerTask1 = tickerTimerTask1;
    }

    public void setTickerTimerTask2(TickerTimerTask tickerTimerTask2) {
        this.tickerTimerTask2 = tickerTimerTask2;
    }

    public HealthCheckTimerTask getHealthCheckTimerTask() {
        return healthCheckTimerTask;
    }

    @Override
    public void onKeysAdded(Wallet wallet, List<ECKey> keys) {
    }

    @Override
    public void onScriptsAdded(Wallet wallet, List<Script> scripts) {
        try {
            throw new Exception("Huck:  not implemented yet");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
