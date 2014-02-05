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
package org.wallet.viewsystem.swing.view.panels;

import com.google.worldcoin.core.Address;
import com.google.worldcoin.uri.WorldcoinURI;
import com.google.worldcoin.uri.WorldcoinURIParseException;
import org.joda.money.Money;
import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.*;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.model.worldcoin.WalletAddressBookData;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WalletInfoData;
import org.wallet.model.core.CoreModel;
import org.wallet.qrcode.QRCodeEncoderDecoder;
import org.wallet.qrcode.QRCodeGenerator;
import org.wallet.store.WorldcoinWalletVersion;
import org.wallet.utils.ImageLoader;
import org.wallet.utils.WhitespaceTrimmer;
import org.wallet.viewsystem.DisplayHint;
import org.wallet.viewsystem.Viewable;
import org.wallet.viewsystem.dataproviders.WorldcoinFormDataProvider;
import org.wallet.viewsystem.dataproviders.CopyQRCodeImageDataProvider;
import org.wallet.viewsystem.swing.ColorAndFontConstants;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.action.CopyQRCodeImageAction;
import org.wallet.viewsystem.swing.action.MnemonicUtil;
import org.wallet.viewsystem.swing.action.PasteSwatchAction;
import org.wallet.viewsystem.swing.action.ZoomAction;
import org.wallet.viewsystem.swing.view.ImageSelection;
import org.wallet.viewsystem.swing.view.components.*;
import org.wallet.viewsystem.swing.view.models.AddressBookTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.*;
import java.util.List;


/**
 * Abstract parent class for SendWorldcoinPanel and ReceiveWorldcoinPanel
 * 
 * @author jim
 * 
 */
public abstract class AbstractTradePanel extends JPanel implements Viewable, CopyQRCodeImageDataProvider, WorldcoinFormDataProvider, CurrencyConverterListener {

    public boolean isShowSidePanel() {
        return showSidePanel;
    }

    public void setShowSidePanel(boolean showSidePanel) {
        AbstractTradePanel.showSidePanel = showSidePanel;
    }

    private static final long serialVersionUID = 7227169670412230264L;

    private static final Logger log = LoggerFactory.getLogger(AbstractTradePanel.class);

    private static final int MINIMUM_QRCODE_PANEL_HORIZONTAL_SPACING = 30;
    private static final int MINIMUM_QRCODE_PANEL_VERTICAL_SPACING = 80;

    private static final int TABLE_BORDER = 3;

    protected static final int PREFERRED_NUMBER_OF_LABEL_ROWS = 3;

    protected WorldcoinWalletFrame mainFrame;

    protected final Controller controller;
    protected final WorldcoinController worldcoinController;

    protected WorldcoinWalletTextArea labelTextArea;

    protected WorldcoinWalletTextField amountWDCTextField;
    protected WorldcoinWalletTextField amountFiatTextField;
    protected WorldcoinWalletLabel amountEqualsLabel;
    protected WorldcoinWalletLabel amountUnitFiatLabel;
    
    protected Money parsedAmountWDC = null;
    protected Money parsedAmountFiat = null;
    protected JLabel notificationLabel;

    protected JPanel upperPanel;

    protected AddressBookTableModel addressesTableModel;

    protected JTable addressesTable;

    protected WorldcoinWalletTextField addressTextField;

    protected int selectedAddressRowModel;

    protected SelectionListener addressesListener;

    protected WorldcoinWalletButton createNewButton;
    protected WorldcoinWalletButton deleteButton;

    protected Action  createNewAddressAction;
    protected Action  deleteAddressAction;

    protected JLabel titleLabel;

    protected WorldcoinWalletLabel qrCodeLabel;
    protected JScrollPane qrCodeScrollPane;

    public final static int QR_CODE_LEFT_COLUMN = 11;

    protected static final int QRCODE_WIDTH = 140;
    protected static final int QRCODE_HEIGHT = 140;

    protected static final int TEXTFIELD_VERTICAL_DELTA = 16;
    protected static final int HELP_BUTTON_INDENT = 6;

    private static final int STENT_DELTA = 4;

    protected FontMetrics fontMetrics;
    protected int separatorSize;
    protected int smallSeparatorSize;

    protected WorldcoinWalletLabel displayUsingLabel;
    protected WorldcoinWalletButton copyQRCodeImageButton;
    protected WorldcoinWalletButton pasteSwatchButton;
    protected WorldcoinWalletButton zoomButton;
    protected JPanel qrCodeButtonPanelStent1;
    protected JPanel qrCodeButtonPanelStent2;
    protected JPanel qrCodeButtonPanelStent3;
    protected JPanel qrCodeButtonPanelStent4;

    protected JPanel forcer2;

    protected WorldcoinWalletButton sidePanelButton;
    protected static boolean showSidePanel = false;

    private final AbstractTradePanel thisAbstractTradePanel;

    private QRCodeGenerator qrCodeGenerator;
    
    private JScrollPane addressesScrollPane;

    /**
     * map that maps one of the key constants in this class to the actual key to
     * use for localisation
     * 
     * this map is filled up in the constructors of the concrete impls
     */
    protected Map<String, String> localisationKeyConstantToKeyMap;

    protected String ADDRESSES_TITLE = "addressesTitle";
    protected String CREATE_NEW_TOOLTIP = "createNewTooltip";
    protected String DELETE_TOOLTIP = "deleteTooltip";

    public AbstractTradePanel(WorldcoinWalletFrame mainFrame, WorldcoinController worldcoinController) {
        this.mainFrame = mainFrame;
        
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        
        this.thisAbstractTradePanel = this;

        try {
            setFont(FontSizer.INSTANCE.getAdjustedDefaultFontWithDelta(2 * ColorAndFontConstants.WORLDCOIN_WALLET_LARGE_FONT_INCREASE));

            Font font = FontSizer.INSTANCE.getAdjustedDefaultFont();

            fontMetrics = this.getFontMetrics(font);

            separatorSize = (int) (fontMetrics.getHeight() * 0.5);
            smallSeparatorSize = (int) (fontMetrics.getHeight() * 0.2);

            localisationKeyConstantToKeyMap = new HashMap<String, String>();
            populateLocalisationMap();

            initUI();

            labelTextArea.requestFocusInWindow();

            displaySidePanel();

            applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

            CurrencyConverter.INSTANCE.addCurrencyConverterListener(this);
        } catch (Exception e) {
            log.error("Error in construction of AbstractTradePanel");
        }
    }

    /**
     * is it the receive worldcoin panel (return true) or the send worldcoin panel
     * (return false)
     */
    protected abstract boolean isReceiveWorldcoin();

    public abstract String getAddressConstant();

    public abstract String getLabelConstant();

    public abstract String getAmountConstant();

    protected abstract Action getCreateNewAddressAction();

    protected abstract Action getDeleteAddressAction();

    /**
     * method for concrete impls to populate the localisation map
     */
    protected abstract void populateLocalisationMap();
    
    public abstract void checkDeleteSendingEnabled();   

    /**
     * get the layout stent for all the keys on the left hand side of the panel
     */
    protected int calculateStentWidth() {
        String[] keys = new String[] { "sendWorldcoinPanel.addressLabel", "sendWorldcoinPanel.labelLabel",
                "sendWorldcoinPanel.amountLabel", "receiveWorldcoinPanel.addressLabel", "receiveWorldcoinPanel.labelLabel",
                "receiveWorldcoinPanel.amountLabel" };

        return WorldcoinWalletTitledPanel.calculateStentWidthForKeys(controller.getLocaliser(), keys, this) + STENT_DELTA;
    }

    /**
     * get a localisation string - the key varies according to the concrete impl
     */
    protected String getLocalisationString(String keyConstant, Object[] data) {
        String stringToReturn = "";
        // get the localisation key
        if (localisationKeyConstantToKeyMap != null && keyConstant != null) {
            String key = localisationKeyConstantToKeyMap.get(keyConstant);
            stringToReturn = controller.getLocaliser().getString(key, data);
        }
        return stringToReturn;
    }

    protected void initUI() {
        setMinimumSize(new Dimension(550, 220));
        setLayout(new GridBagLayout());
        setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

        String showSidePanelText = controller.getModel().getUserPreference(WorldcoinModel.SHOW_SIDE_PANEL);
        if (!Boolean.FALSE.toString().equals(showSidePanelText)) {
            showSidePanel = true;
        }

        GridBagConstraints constraints = new GridBagConstraints();
        GridBagConstraints constraints2 = new GridBagConstraints();

        upperPanel = new JPanel(new GridBagLayout());
        upperPanel.setOpaque(false);
        createFormPanel(upperPanel, constraints2);
        createQRCodePanel(upperPanel, constraints2);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.5;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(upperPanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 2.0;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(createAddressesPanel(), constraints);
        
        CurrencyConverter.INSTANCE.updateFormatters();
        updateFiatAmount();
    }

    protected void createFormPanelStentsAndForcers(JPanel panel, GridBagConstraints constraints) {
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0.01;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;

        panel.add(WorldcoinWalletTitledPanel.createStent(calculateStentWidth(), separatorSize), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(WorldcoinWalletTitledPanel.createStent(WorldcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS, separatorSize),
                constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(
                WorldcoinWalletTitledPanel.createStent(WorldcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS, fontMetrics.getHeight()
                        * PREFERRED_NUMBER_OF_LABEL_ROWS), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(
                WorldcoinWalletTitledPanel.createStent(WorldcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS, separatorSize),
                constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(WorldcoinWalletTitledPanel.createStent(fontMetrics.stringWidth(WorldcoinWalletFrame.EXAMPLE_LONG_FIELD_TEXT), separatorSize),
                constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(WorldcoinWalletTitledPanel.createStent(WorldcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS, separatorSize),
                constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 5;
        constraints.gridy = 0;
        constraints.weightx = 0.1;
        constraints.weighty = 0.01;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(WorldcoinWalletTitledPanel.createStent(separatorSize), constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 7;
        constraints.gridy = 0;
        constraints.weightx = 0.1;
        constraints.weighty = 0.01;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(WorldcoinWalletTitledPanel.createStent(separatorSize), constraints);

        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.gridx = 10;
        constraints.gridy = 1;
        constraints.weightx = 0.02;
        constraints.weighty = 10000;
        constraints.gridwidth = 1;
        constraints.gridheight = 8;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel dashedStent = WorldcoinWalletTitledPanel.createStent(smallSeparatorSize);
        dashedStent.setBorder(new DashedBorder(controller.getLocaliser().getLocale()));
        panel.add(dashedStent, constraints);

        JPanel forcer1 = new JPanel();
        forcer1.setOpaque(false);
        //forcer1.setBorder(BorderFactory.createLineBorder(Color.BLUE));

        forcer1.setMaximumSize(new Dimension(QRCODE_WIDTH, 1));
        forcer1.setPreferredSize(new Dimension(QRCODE_WIDTH, 1));
        forcer1.setMinimumSize(new Dimension((int) (QRCODE_WIDTH * 0.1), 1));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 9;
        constraints.gridy = 7;
        constraints.weightx = 10000;
        constraints.weighty = 4.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        panel.add(forcer1, constraints);

        forcer2 = new JPanel();
        forcer2.setOpaque(false);
        forcer2.setMaximumSize(new Dimension((int) (QRCODE_WIDTH * 1.2), 1));
        forcer2.setPreferredSize(new Dimension(QRCODE_WIDTH, 1));
        forcer2.setMinimumSize(new Dimension((int) (QRCODE_WIDTH * 1.0), 1));
        //forcer2.setBorder(BorderFactory.createLineBorder(Color.CYAN));
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = QR_CODE_LEFT_COLUMN;
        constraints.gridy = 7;
        constraints.weightx = 1;
        constraints.weighty = 1.0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        panel.add(forcer2, constraints);
    }

    public void displaySidePanel() {
        setQRCodePanelVisible(showSidePanel);

        if (sidePanelButton == null) {
            return;
        }

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());

        if (showSidePanel) {
            // show less
            if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
                sidePanelButton.setIcon(ImageLoader.createImageIcon(ImageLoader.SIDE_PANEL_HIDE_ICON_FILE));
            } else {
                sidePanelButton.setIcon(ImageLoader.createImageIcon(ImageLoader.SIDE_PANEL_HIDE_RTL_ICON_FILE));
            }
            sidePanelButton.setText("");
            sidePanelButton.setBorderPainted(false);
            sidePanelButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinPanel.showLess.tooltip")));
            sidePanelButton.setMnemonic(mnemonicUtil.getMnemonic(controller.getLocaliser().getString(
                    "sendWorldcoinPanel.showLess.mnemonic")));
            sidePanelButton.setVerticalTextPosition(JLabel.BOTTOM);
            sidePanelButton.setHorizontalTextPosition(JLabel.LEFT);
        } else {
            // show more
            if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
                sidePanelButton.setIcon(ImageLoader.createImageIcon(ImageLoader.SIDE_PANEL_SHOW_ICON_FILE));
            } else {
                sidePanelButton.setIcon(ImageLoader.createImageIcon(ImageLoader.SIDE_PANEL_SHOW_RTL_ICON_FILE));
            }
            sidePanelButton.setText("");
            sidePanelButton.setBorderPainted(false);
            sidePanelButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinPanel.showMore.tooltip")));
            sidePanelButton.setMnemonic(mnemonicUtil.getMnemonic(controller.getLocaliser().getString(
                    "sendWorldcoinPanel.showMore.mnemonic")));
            sidePanelButton.setVerticalTextPosition(JLabel.BOTTOM);
            sidePanelButton.setHorizontalTextPosition(JLabel.LEFT);
        }
    }

    protected abstract JPanel createFormPanel(JPanel panel, GridBagConstraints constraints);

    protected abstract void loadForm();

    protected JPanel createAddressesHeaderPanel() {
        JPanel addressesHeaderPanel = new JPanel();
        addressesHeaderPanel.setOpaque(true);
        addressesHeaderPanel.setBackground(ColorAndFontConstants.MID_BACKGROUND_COLOR);

        addressesHeaderPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SystemColor.windowBorder));
        addressesHeaderPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.01;
        constraints.weighty = 0.01;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressesHeaderPanel.add(WorldcoinWalletTitledPanel.createStent(HELP_BUTTON_INDENT), constraints);

        createNewAddressAction = getCreateNewAddressAction();
        createNewButton = new WorldcoinWalletButton(createNewAddressAction, controller);
        createNewButton.setText(controller.getLocaliser().getString("crudButton.new"));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0.1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressesHeaderPanel.add(createNewButton, constraints);

        int offset;
        deleteAddressAction = getDeleteAddressAction();
        if (isReceiveWorldcoin()) {
            // Put in a stent
            WorldcoinWalletButton dummyDeleteButton = new WorldcoinWalletButton(deleteAddressAction, controller);
            JPanel deleteButtonStent = WorldcoinWalletTitledPanel.createStent(dummyDeleteButton.getPreferredSize().width, dummyDeleteButton.getPreferredSize().height);
            offset = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 2;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            constraints.weightx = 0.1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.LINE_START;
            addressesHeaderPanel.add(deleteButtonStent, constraints);
        } else {
            deleteButton = new WorldcoinWalletButton(deleteAddressAction, controller);
            offset = 1;
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 2;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            constraints.weightx = 0.1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.LINE_START;
            addressesHeaderPanel.add(deleteButton, constraints);           
        }

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2 + offset;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        addressesHeaderPanel.add(WorldcoinWalletTitledPanel.createStent(HELP_BUTTON_INDENT * 2), constraints);

        titleLabel = new JLabel();
        titleLabel.setHorizontalTextPosition(JLabel.CENTER);
        titleLabel.setText(getLocalisationString(ADDRESSES_TITLE, null));
        titleLabel.setFont(FontSizer.INSTANCE.getAdjustedDefaultFontWithDelta(ColorAndFontConstants.WORLDCOIN_WALLET_LARGE_FONT_INCREASE));

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3 + offset;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressesHeaderPanel.add(titleLabel, constraints);

        JPanel filler2 = new JPanel();
        filler2.setOpaque(false);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1000;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressesHeaderPanel.add(filler2, constraints);

        return addressesHeaderPanel;
    }

    protected JPanel createAddressesPanel() {
        JPanel addressPanel = new JPanel();
        addressPanel.setOpaque(true);
        addressPanel.setBackground(ColorAndFontConstants.BACKGROUND_COLOR);

        addressPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        addressesTableModel = new AddressBookTableModel(this.worldcoinController, isReceiveWorldcoin());
        addressesTable = new JTable(addressesTableModel);
        addressesTable.setOpaque(true);
        addressesTable.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        addressesTable.setShowGrid(false);
        addressesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        addressesTable.setRowSelectionAllowed(true);
        addressesTable.setColumnSelectionAllowed(false);
        addressesTable.setRowHeight(getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight() + ShowTransactionsPanel.HEIGHT_DELTA);

        // row sorter
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(addressesTable.getModel());
        addressesTable.setRowSorter(rowSorter);

        // sort by date descending
        List<TableRowSorter.SortKey> sortKeys = new ArrayList<TableRowSorter.SortKey>();
        sortKeys.add(new TableRowSorter.SortKey(0, SortOrder.ASCENDING));
        rowSorter.setSortKeys(sortKeys);
        Comparator<String> comparator = new Comparator<String>() {
            
            Collator collator;
            
            @Override
            public int compare(String o1, String o2) {
                if (collator == null) {
                    collator = Collator.getInstance(controller.getLocaliser().getLocale());
                }
                if (o1 == null) {
                    return 1;
                } else if (o2 == null) {
                    return -1;
                } else if ("".equals(o1) && !"".equals(o2)) {
                    return 1;
                } else if ("".equals(o2) && !"".equals(o1)) {
                    return -1;
                } else {
                    return collator.compare(o1, o2);
                }
            }
        };
        rowSorter.setComparator(0, comparator);

        // justify column headers
        TableCellRenderer renderer = addressesTable.getTableHeader().getDefaultRenderer();
        JLabel label = (JLabel) renderer;
        if (ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()).isLeftToRight()) {
            label.setHorizontalAlignment(JLabel.LEFT);
        } else {
            label.setHorizontalAlignment(JLabel.RIGHT);
        }
        addressesTable.getTableHeader().setFont(FontSizer.INSTANCE.getAdjustedDefaultFont());

        TableColumn tableColumn = addressesTable.getColumnModel().getColumn(0); // label
        tableColumn.setPreferredWidth(40);
        if (ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()).isLeftToRight()) {
            tableColumn.setCellRenderer(new LeftJustifiedRenderer());
        } else {
            tableColumn.setCellRenderer(new RightJustifiedRenderer());
        }

        // description leading justified (set explicitly as it does not seem to
        // work otherwise)
        tableColumn = addressesTable.getColumnModel().getColumn(1); // address
        tableColumn.setPreferredWidth(120);
        // addresses leading justified (set explicitly as it does not seem to
        // work otherwise)
        if (ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()).isLeftToRight()) {
            tableColumn.setCellRenderer(new LeftJustifiedRenderer());
        } else {
            tableColumn.setCellRenderer(new RightJustifiedRenderer());
        }

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.05;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressPanel.add(createAddressesHeaderPanel(), constraints);

        addressesScrollPane = new JScrollPane(addressesTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setupScrollPane();

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        addressPanel.add(addressesScrollPane, constraints);

        // add on a selection listener
        addressesListener = new SelectionListener();
        addressesTable.getSelectionModel().addListSelectionListener(addressesListener);

        return addressPanel;
    }
    
    private void setupScrollPane() {
        addressesScrollPane.getViewport().setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        addressesScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        addressesScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        addressesScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);

    }

    protected void setQRCodePanelVisible(boolean visible) {
        if (displayUsingLabel != null) {
            displayUsingLabel.setVisible(visible);
        }

        if (qrCodeScrollPane != null) {
            qrCodeScrollPane.setVisible(visible);
        }

        if (copyQRCodeImageButton != null) {
            copyQRCodeImageButton.setVisible(visible);
        }

        if (pasteSwatchButton != null) {
            pasteSwatchButton.setVisible(visible);
        }

        if (zoomButton != null) {
            zoomButton.setVisible(visible);
        }

        if (qrCodeButtonPanelStent1 != null) {
            qrCodeButtonPanelStent1.setVisible(visible);
        }

        if (qrCodeButtonPanelStent2 != null) {
            qrCodeButtonPanelStent2.setVisible(visible);
        }

        if (qrCodeButtonPanelStent3 != null) {
            qrCodeButtonPanelStent3.setVisible(visible);
        }
        
        if (qrCodeButtonPanelStent4 != null) {
            qrCodeButtonPanelStent4.setVisible(visible);
        }

        if (forcer2 != null) {
            forcer2.setVisible(visible);
        }

        if (qrCodeLabel != null) {
            qrCodeLabel.invalidate();
            qrCodeLabel.validate();
        }
        if (upperPanel != null) {
            upperPanel.invalidate();
            upperPanel.validate();
        }
        thisAbstractTradePanel.repaint();
    }

    class SelectionListener implements ListSelectionListener {
        boolean enabled;

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        SelectionListener() {
            enabled = true;
        }

        @Override
        public void valueChanged(ListSelectionEvent event) {
            if (enabled) {
                int viewRow = addressesTable.getSelectedRow();
                if (viewRow >= 0) {
                    selectedAddressRowModel = addressesTable.convertRowIndexToModel(viewRow);
                    WalletAddressBookData rowData = addressesTableModel.getAddressBookDataByRow(selectedAddressRowModel,
                            thisAbstractTradePanel.isReceiveWorldcoin());
                    if (rowData != null) {
                        worldcoinController.getModel().setActiveWalletPreference(thisAbstractTradePanel.getAddressConstant(),
                                rowData.getAddress());
                        worldcoinController.getModel().setActiveWalletPreference(thisAbstractTradePanel.getLabelConstant(),
                                rowData.getLabel());
                        if (addressTextField != null) {
                            addressTextField.setText(rowData.getAddress());
                        }
                        labelTextArea.setText(rowData.getLabel());

                        String amountForQRCode = "";
                        if (parsedAmountWDC != null && parsedAmountWDC.getAmount() != null) {
                            amountForQRCode = controller.getLocaliser().worldcoinValueToStringNotLocalised(parsedAmountWDC.getAmount().toBigInteger(), false, false);
                        }
                        displayQRCode(rowData.getAddress(), amountForQRCode, labelTextArea.getText());
                    }
                }
            }
        }
    }

    static class LeftJustifiedRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1549115L;

        WorldcoinWalletLabel label = new WorldcoinWalletLabel("");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(new Insets(1, TABLE_BORDER, 1, TABLE_BORDER)));

            label.setText((String) value);

            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                Color backgroundColor = (row % 2 == 1 ? ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR
                        : ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                label.setBackground(backgroundColor);
                label.setForeground(table.getForeground());
            }
            return label;
        }
    }

    static class RightJustifiedRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 2299545L;

        WorldcoinWalletLabel label = new WorldcoinWalletLabel("");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(new Insets(1, TABLE_BORDER, 1, TABLE_BORDER)));

            label.setText((String) value);

            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                Color backgroundColor = (row % 2 == 1 ? ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR
                        : ColorAndFontConstants.ALTERNATE_TABLE_COLOR);
                label.setBackground(backgroundColor);
                label.setForeground(table.getForeground());
            }
            return label;
        }
    }

    protected void createQRCodePanel(JPanel panel, GridBagConstraints constraints) {
        qrCodeLabel = new WorldcoinWalletLabel("", JLabel.CENTER);
        qrCodeLabel.setVerticalTextPosition(JLabel.BOTTOM);
        qrCodeLabel.setHorizontalTextPosition(JLabel.CENTER);
        qrCodeLabel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        qrCodeLabel.setOpaque(true);
        qrCodeLabel.setMinimumSize(new Dimension(QRCODE_WIDTH, QRCODE_HEIGHT));

        setDragLabelTextAndTooltip();

        // copy/ drag image support
        if (isReceiveWorldcoin()) {
            qrCodeLabel.setTransferHandler(new ImageSelection(this, false));
        } else {
            qrCodeLabel.setTransferHandler(new ImageSelection(this, true));
        }

        // drag support
        MouseListener listener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                JComponent comp = (JComponent) me.getSource();
                TransferHandler handler = comp.getTransferHandler();
                handler.exportAsDrag(comp, me, TransferHandler.COPY);
            }
        };
        qrCodeLabel.addMouseListener(listener);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = QR_CODE_LEFT_COLUMN;
        constraints.gridy = 3;
        constraints.weightx = 1;
        constraints.weighty = 0.5;
        constraints.gridwidth = 5;
        constraints.gridheight = 3;
        constraints.anchor = GridBagConstraints.BASELINE;

        qrCodeScrollPane = new JScrollPane(qrCodeLabel);
        qrCodeScrollPane.setOpaque(true);
        qrCodeScrollPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        qrCodeScrollPane.getViewport().setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        qrCodeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        qrCodeScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        qrCodeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        qrCodeScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        qrCodeScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        qrCodeScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);

        panel.add(qrCodeScrollPane, constraints);

        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.gridx = QR_CODE_LEFT_COLUMN + 6;
        constraints.gridy = 3;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 5;
        constraints.gridheight = 3;
        constraints.anchor = GridBagConstraints.BASELINE;
        
        JPanel verticalStent = WorldcoinWalletTitledPanel.createStent(1, QRCODE_HEIGHT);
        //verticalStent.setBorder(BorderFactory.createLineBorder(Color.MAGENTA));
        panel.add(verticalStent, constraints);

        createQRCodeButtonPanel(panel, constraints);
    }

    private void setDragLabelTextAndTooltip() {
        if (!isReceiveWorldcoin()) {
            Icon icon = qrCodeLabel.getIcon();
            if (icon == null || icon.getIconHeight() == -1) {
                qrCodeLabel.setText(createCenteredMultilineLabelText(controller.getLocaliser().getString("sendWorldcoinPanel.dragWorldcoinLabelQRcode.text")));
            } else {
                qrCodeLabel.setText("");                
            }
            qrCodeLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinPanel.dragWorldcoinLabelQRcode.tooltip")));
        }
    }

    protected void updateQRCodePanel() {
        String address = null;

        if (addressTextField != null) {
            address = addressTextField.getText();
            if (!WhitespaceTrimmer.trim(address).equals(address)) {
                address = WhitespaceTrimmer.trim(address);
                addressTextField.setText(address);
            }
        }
        String amount = null;
        if (amountWDCTextField != null) {
            CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDC(amountWDCTextField.getText());
            if (converterResult.isWdcMoneyValid()) {
                amount = controller.getLocaliser().worldcoinValueToStringNotLocalised(parsedAmountWDC.getAmount().toBigInteger(), false, false);
            }
        }
        String label = "";
        if (labelTextArea != null) {
            label = labelTextArea.getText();
        }
        displayQRCode(address, amount, label);
        qrCodeLabel.invalidate();
        upperPanel.invalidate();
        qrCodeLabel.validate();
        upperPanel.validate();
        thisAbstractTradePanel.repaint();
    }

    protected void createQRCodeButtonPanel(JPanel panel, GridBagConstraints constraints) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        buttonPanel.setOpaque(false);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = QR_CODE_LEFT_COLUMN;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.BELOW_BASELINE_LEADING;
        panel.add(buttonPanel, constraints);

        GridBagConstraints constraints2 = new GridBagConstraints();

        CopyQRCodeImageAction copyQRCodeImageAction = new CopyQRCodeImageAction(controller, this,
                ImageLoader.createImageIcon(ImageLoader.COPY_ICON_FILE));
        copyQRCodeImageButton = new WorldcoinWalletButton(copyQRCodeImageAction, controller);

        PasteSwatchAction pasteSwatchAction = new PasteSwatchAction(this.worldcoinController, this,
                ImageLoader.createImageIcon(ImageLoader.PASTE_ICON_FILE));
        pasteSwatchButton = new WorldcoinWalletButton(pasteSwatchAction, controller);

        qrCodeButtonPanelStent1 = WorldcoinWalletTitledPanel.createStent(smallSeparatorSize);
        qrCodeButtonPanelStent2 = WorldcoinWalletTitledPanel.createStent(smallSeparatorSize);
        qrCodeButtonPanelStent3 = WorldcoinWalletTitledPanel.createStent(smallSeparatorSize);

        ZoomAction zoomAction = new ZoomAction(this.worldcoinController, ImageLoader.createImageIcon(ImageLoader.ZOOM_ICON_FILE), mainFrame, this);
        zoomButton = new WorldcoinWalletButton(zoomAction, controller);
        zoomButton.setText("");

        qrCodeButtonPanelStent4 = WorldcoinWalletTitledPanel.createStent((int)zoomButton.getPreferredSize().getWidth(), (int)zoomButton.getPreferredSize().getHeight());

        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 1;
        constraints2.gridy = 0;
        constraints2.weightx = 0.1;
        constraints2.weighty = 1.0;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.CENTER;
        buttonPanel.add(qrCodeButtonPanelStent1);

        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 2;
        constraints2.gridy = 0;
        constraints2.weightx = 0.1;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.BELOW_BASELINE_TRAILING;
        buttonPanel.add(copyQRCodeImageButton, constraints2);

        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 3;
        constraints2.gridy = 0;
        constraints2.weightx = 0.1;
        constraints2.weighty = 1.0;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.CENTER;
        buttonPanel.add(qrCodeButtonPanelStent2);

        constraints2.fill = GridBagConstraints.BOTH;
        constraints2.gridx = 4;
        constraints2.gridy = 0;
        constraints2.weightx = 0.1;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.BELOW_BASELINE_TRAILING;
        if (isReceiveWorldcoin()) {
            buttonPanel.add(zoomButton, constraints2);
            
            constraints2.fill = GridBagConstraints.NONE;
            constraints2.gridx = 5;
            constraints2.gridy = 0;
            constraints2.weightx = 0.1;
            constraints2.weighty = 1.0;
            constraints2.gridwidth = 1;
            constraints2.gridheight = 1;
            constraints2.anchor = GridBagConstraints.CENTER;
            buttonPanel.add(qrCodeButtonPanelStent3, constraints2);

            constraints2.fill = GridBagConstraints.NONE;
            constraints2.gridx = 6;
            constraints2.gridy = 0;
            constraints2.weightx = 0.1;
            constraints2.weighty = 1.0;
            constraints2.gridwidth = 1;
            constraints2.gridheight = 1;
            constraints2.anchor = GridBagConstraints.BELOW_BASELINE_TRAILING;
            buttonPanel.add(qrCodeButtonPanelStent4, constraints2);
        } else {
            buttonPanel.add(pasteSwatchButton, constraints2);

            constraints2.fill = GridBagConstraints.NONE;
            constraints2.gridx = 5;
            constraints2.gridy = 0;
            constraints2.weightx = 0.1;
            constraints2.weighty = 1.0;
            constraints2.gridwidth = 1;
            constraints2.gridheight = 1;
            constraints2.anchor = GridBagConstraints.CENTER;
            buttonPanel.add(qrCodeButtonPanelStent3, constraints2);

            constraints2.fill = GridBagConstraints.NONE;
            constraints2.gridx = 6;
            constraints2.gridy = 0;
            constraints2.weightx = 0.1;
            constraints2.weighty = 1.0;
            constraints2.gridwidth = 1;
            constraints2.gridheight = 1;
            constraints2.anchor = GridBagConstraints.BELOW_BASELINE_TRAILING;
            buttonPanel.add(zoomButton, constraints2);
        }

        JPanel forcerQR = new JPanel();
        forcerQR.setOpaque(false);
        constraints2.fill = GridBagConstraints.HORIZONTAL;
        constraints2.gridx = 7;
        constraints2.gridy = 0;
        constraints2.weightx = 50.0;
        constraints2.weighty = 1.0;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        buttonPanel.add(forcerQR, constraints2);
    }

    protected JPanel createAmountPanel() {
        JPanel amountPanel = new JPanel();
        amountPanel.setOpaque(false);
        amountPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        amountPanel.setLayout(new GridBagLayout());

        GridBagConstraints constraints2 = new GridBagConstraints();
        int longFieldWidth = fontMetrics.stringWidth(WorldcoinWalletFrame.EXAMPLE_LONG_FIELD_TEXT);

        amountWDCTextField = new WorldcoinWalletTextField("", 10, controller);
        amountWDCTextField.setHorizontalAlignment(JTextField.TRAILING);
        amountWDCTextField.setMinimumSize(new Dimension((int) (longFieldWidth * 0.45), getFontMetrics(
                FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight()
                + TEXTFIELD_VERTICAL_DELTA));
        amountWDCTextField.setPreferredSize(new Dimension((int) (longFieldWidth * 0.45), getFontMetrics(
                FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight()
                + TEXTFIELD_VERTICAL_DELTA));
        amountWDCTextField.setMaximumSize(new Dimension((int) (longFieldWidth * 0.45), getFontMetrics(
                FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight()
                + TEXTFIELD_VERTICAL_DELTA));
        amountWDCTextField.addKeyListener(new AmountWDCKeyListener());
        
        constraints2.fill = GridBagConstraints.HORIZONTAL;
        constraints2.gridx = 0;
        constraints2.gridy = 0;
        constraints2.weightx = 2.0;
        constraints2.weighty = 0.3;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        amountPanel.add(amountWDCTextField, constraints2);

        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 1;
        constraints2.gridy = 0;
        constraints2.weightx = 0.1;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.CENTER;
        amountPanel.add(WorldcoinWalletTitledPanel.createStent(3), constraints2);

        WorldcoinWalletLabel amountUnitWDCLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString("sendWorldcoinPanel.amountUnitLabel"));
        amountUnitWDCLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        amountUnitWDCLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendWorldcoinPanel.amountUnitLabel.tooltip")));
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 2;
        constraints2.gridy = 0;
        constraints2.weightx = 0.1;
        constraints2.weighty = 0.3;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.CENTER;
        amountPanel.add(amountUnitWDCLabel, constraints2);

        amountEqualsLabel = new WorldcoinWalletLabel("   =   "); // 3 spaces either side
        amountEqualsLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        amountEqualsLabel.setFocusable(false);
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 3;
        constraints2.gridy = 0;
        constraints2.weightx = 0.03;
        constraints2.weighty = 0.3;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.CENTER;
        amountPanel.add(amountEqualsLabel, constraints2);
        amountPanel.add(WorldcoinWalletTitledPanel.createStent(amountEqualsLabel.getPreferredSize().width, amountEqualsLabel.getPreferredSize().height), constraints2);

        amountFiatTextField = new WorldcoinWalletTextField("", 8, controller);
        amountFiatTextField.setHorizontalAlignment(JTextField.TRAILING);
        amountFiatTextField.setMinimumSize(new Dimension((int) (longFieldWidth * 0.2), getFontMetrics(
                FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight()
                + TEXTFIELD_VERTICAL_DELTA));
        amountFiatTextField.setPreferredSize(new Dimension((int) (longFieldWidth * 0.2), getFontMetrics(
                FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight()
                + TEXTFIELD_VERTICAL_DELTA));
        amountFiatTextField.setMaximumSize(new Dimension((int) (longFieldWidth * 0.2), getFontMetrics(
                FontSizer.INSTANCE.getAdjustedDefaultFont()).getHeight()
                + TEXTFIELD_VERTICAL_DELTA));
        //amountFiatTextField.addKeyListener(new QRCodeKeyListener());
        amountFiatTextField.addKeyListener(new AmountFiatKeyListener());

        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 6;
        constraints2.gridy = 0;
        constraints2.weightx = 1.0;
        constraints2.weighty = 0.3;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        amountPanel.add(amountFiatTextField, constraints2);
        amountPanel.add(WorldcoinWalletTitledPanel.createStent(amountFiatTextField.getPreferredSize().width, amountFiatTextField.getPreferredSize().height), constraints2);

        CurrencyInfo currencyInfo = CurrencyConverter.INSTANCE.getCurrencyCodeToInfoMap().get(CurrencyConverter.INSTANCE.getCurrencyUnit().getCurrencyCode());
        if (currencyInfo == null) {
            // Create a default currency info with the raw currency code as a suffix, including a separator space
            currencyInfo = new CurrencyInfo(CurrencyConverter.INSTANCE.getCurrencyUnit().getCurrencyCode(), CurrencyConverter.INSTANCE.getCurrencyUnit().getCurrencyCode(), false);
            currencyInfo.setHasSeparatingSpace(true);
        }
        amountUnitFiatLabel = new WorldcoinWalletLabel("");
        int fiatCurrencySymbolPosition = 4;   // Prefix is default.
        int stentPosition = 5;
        amountUnitFiatLabel.setText(currencyInfo.getCurrencySymbol());
        if (!currencyInfo.isPrefix()) {
            stentPosition = 7;
            fiatCurrencySymbolPosition = 8;
        }
        
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = stentPosition;
        constraints2.gridy = 0;
        constraints2.weightx = 0.1;
        constraints2.weighty = 0.1;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.CENTER;
        amountPanel.add(WorldcoinWalletTitledPanel.createStent(3), constraints2);

        amountUnitFiatLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = fiatCurrencySymbolPosition;
        constraints2.gridy = 0;
        constraints2.weightx = 0.1;
        constraints2.weighty = 0.3;
        constraints2.gridwidth = 1;
        constraints2.gridheight = 1;
        constraints2.anchor = GridBagConstraints.LINE_START;
        amountPanel.add(amountUnitFiatLabel, constraints2);  
        amountPanel.add(WorldcoinWalletTitledPanel.createStent(amountUnitFiatLabel.getPreferredSize().width, amountUnitFiatLabel.getPreferredSize().height), constraints2);

        // Make fiat fields visible if currency is available.
        boolean fiatIsVisible = CurrencyConverter.INSTANCE.isShowingFiat() && CurrencyConverter.INSTANCE.getRate() != null;
        amountUnitFiatLabel.setVisible(fiatIsVisible);
        amountFiatTextField.setVisible(fiatIsVisible);
        amountEqualsLabel.setVisible(fiatIsVisible);
        
        return amountPanel;
    }
    
    @Override
    public void displayView(DisplayHint displayHint) {
        if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
            return;
        }
        
        loadForm();
        setupScrollPane();
        getAddressesTableModel().fireTableDataChanged();
        selectRows();

        // Disable any new changes if another process has changed the wallet.
        if (this.worldcoinController.getModel().getActivePerWalletModelData() != null
                && this.worldcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
            // files have been changed by another process - disallow edits
            mainFrame.setUpdatesStoppedTooltip(labelTextArea);
            labelTextArea.setEditable(false);
            labelTextArea.setEnabled(false);
            mainFrame.setUpdatesStoppedTooltip(amountWDCTextField);
            amountWDCTextField.setEditable(false);
            amountWDCTextField.setEnabled(false);
            if (amountFiatTextField != null) {
                amountFiatTextField.setEditable(false);
                amountFiatTextField.setEnabled(false);
            }
            
            if (createNewButton != null) {
                createNewButton.setEnabled(false);
                mainFrame.setUpdatesStoppedTooltip(createNewButton);
            }
            if (deleteButton != null) {
                deleteButton.setEnabled(false);
                mainFrame.setUpdatesStoppedTooltip(deleteButton);
            }
            if (pasteSwatchButton != null) {
                pasteSwatchButton.setEnabled(false);
                mainFrame.setUpdatesStoppedTooltip(pasteSwatchButton);
            }
        } else {
            labelTextArea.setToolTipText(null);
            labelTextArea.setEditable(true);
            labelTextArea.setEnabled(true);
            amountWDCTextField.setToolTipText(null);
            amountWDCTextField.setEditable(true);
            amountWDCTextField.setEnabled(true);
            if (amountFiatTextField != null) {
                amountFiatTextField.setToolTipText(null);
                amountFiatTextField.setEditable(true);
                amountFiatTextField.setEnabled(true);
            }
            if (createNewButton != null) {
                createNewButton.setEnabled(true);
                createNewButton.setToolTipText(HelpContentsPanel.createTooltipText(getLocalisationString(CREATE_NEW_TOOLTIP, null)));
            }
            if (deleteButton != null) {
                checkDeleteSendingEnabled();
//                boolean deleteEnable = false;
//                if (addressesTableModel != null) {
//                    deleteEnable = addressesTableModel.getRowCount() > 0;
//                }
//                deleteButton.setEnabled(deleteEnable);
                deleteButton.setToolTipText(HelpContentsPanel.createTooltipText(getLocalisationString(DELETE_TOOLTIP, null)));
            }
            if (pasteSwatchButton != null) {
                pasteSwatchButton.setEnabled(true);
                pasteSwatchButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("pasteSwatchAction.tooltip")));
            }
        }
        
        if (CurrencyConverter.INSTANCE.isShowingFiat() && CurrencyConverter.INSTANCE.getRate() != null) {
            if (amountFiatTextField != null) {
                amountFiatTextField.setVisible(true);
                amountEqualsLabel.setVisible(true);
                amountUnitFiatLabel.setVisible(true);
            }
            if (amountWDCTextField != null) {
                CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDC(amountWDCTextField.getText());
                if (converterResult.isWdcMoneyValid()) {
                    parsedAmountWDC = converterResult.getWdcMoney();
                    if (notificationLabel != null) {
                        notificationLabel.setText("");
                    } 
                } else {
                    parsedAmountWDC = null;
                    if (notificationLabel != null) {
                        notificationLabel.setText(converterResult.getWdcMessage());
                    } 
                } 
            }
            updateFiatAmount();
        } else {
            if (amountFiatTextField != null) {
                amountFiatTextField.setVisible(false);
                amountEqualsLabel.setVisible(false);
                amountUnitFiatLabel.setVisible(false);
            }
        }
        updateQRCodePanel();
        displaySidePanel();
    }

    @Override
    public void navigateAwayFromView() {
    }

    protected class QRCodeKeyListener implements KeyListener {
        /** Handle the key typed event from the text field. */
        @Override
        public void keyTyped(KeyEvent e) {
        }

        /** Handle the key-pressed event from the text field. */
        @Override
        public void keyPressed(KeyEvent e) {
            // do nothing
        }

        /** Handle the key-released event from the text field. */
        @Override
        public void keyReleased(KeyEvent e) {
            String address = null;
            if (addressTextField != null) {
                address = addressTextField.getText();
                if (!WhitespaceTrimmer.trim(address).equals(address)) {
                    address = WhitespaceTrimmer.trim(address);
                    addressTextField.setText(address);
                }

            }
            String amount = "";
            if (amountWDCTextField != null) {
                CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDC(amountWDCTextField.getText());
                
                if (converterResult.isWdcMoneyValid()) {
                    parsedAmountWDC = converterResult.getWdcMoney();
                    amount = controller.getLocaliser().worldcoinValueToStringNotLocalised(parsedAmountWDC.getAmount().toBigInteger(), false, false);
                    if (notificationLabel != null) {
                        notificationLabel.setText("");
                    }
                } else {
                    parsedAmountWDC = null;
                    if (notificationLabel != null) {
                        notificationLabel.setText(converterResult.getWdcMessage());
                    }
                }
            }
            String label = labelTextArea.getText();
            WalletAddressBookData addressBookData = new WalletAddressBookData(label, address);

            WalletInfoData walletInfo = worldcoinController.getModel().getActiveWalletInfo();
            if (walletInfo == null) {
                walletInfo = new WalletInfoData(worldcoinController.getModel().getActiveWalletFilename(), worldcoinController.getModel().getActiveWallet(), WorldcoinWalletVersion.PROTOBUF_ENCRYPTED);
                worldcoinController.getModel().setActiveWalletInfo(walletInfo);
            }
            address = WhitespaceTrimmer.trim(address);
            addressesTableModel.setAddressBookDataByRow(addressBookData, selectedAddressRowModel, isReceiveWorldcoin());

            selectRowInTableFromModelRow(selectedAddressRowModel);

            worldcoinController.getModel().setActiveWalletPreference(thisAbstractTradePanel.getAddressConstant(), address);
            worldcoinController.getModel().setActiveWalletPreference(thisAbstractTradePanel.getLabelConstant(), label);
            worldcoinController.getModel().setActiveWalletPreference(thisAbstractTradePanel.getAmountConstant(), amount);
            worldcoinController.getModel().getActivePerWalletModelData().setDirty(true);

            displayQRCode(address, amount, label);
        }
    }
    

    protected class AmountWDCKeyListener implements KeyListener {
        /** Handle the key typed event in the amount WDC field */
        @Override
        public void keyTyped(KeyEvent e) {
        }

        /** Handle the key-pressed event in the amount WDC field */
        @Override
        public void keyPressed(KeyEvent e) {
            // do nothing
        }

        /** Handle the key-released event in the amount WDC field */
        @Override
        public void keyReleased(KeyEvent e) {
            String address = null;
            if (addressTextField != null) {
                address = addressTextField.getText();
                if (!WhitespaceTrimmer.trim(address).equals(address)) {
                    address = WhitespaceTrimmer.trim(address);
                    addressTextField.setText(address);
                }
            }
            String amount;
            
            if (amountWDCTextField != null) {
                CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDC(amountWDCTextField.getText());
                
                if (converterResult.isWdcMoneyValid()) {
                    parsedAmountWDC = converterResult.getWdcMoney();
                    amount = controller.getLocaliser().worldcoinValueToStringNotLocalised(parsedAmountWDC.getAmount().toBigInteger(), false, false);
                    if (notificationLabel != null) {
                        notificationLabel.setText("");
                    }
                    
                    String label = labelTextArea.getText();
                    worldcoinController.getModel().setActiveWalletPreference(thisAbstractTradePanel.getAmountConstant(), amount);
                    worldcoinController.getModel().getActivePerWalletModelData().setDirty(true);

                    updateFiatAmount();
                    displayQRCode(address, amount, label);
                } else {
                    parsedAmountWDC = null;
                    if (notificationLabel != null) {
                        notificationLabel.setText(converterResult.getWdcMessage());
                    }    
                    updateFiatAmount();
                    // Invalid amount o blank the QR code to avoid confusion
                    displayQRCode(null, null, null);
                }
            }
        }
    }
    
    protected class AmountFiatKeyListener implements KeyListener {
        /** Handle the key typed event in the amount Fiat field */
        @Override
        public void keyTyped(KeyEvent e) {
        }

        /** Handle the key-pressed event in the amount Fiat field */
        @Override
        public void keyPressed(KeyEvent e) {
            // do nothing
        }

        /** Handle the key-released event in the amount Fiat field */
        @Override
        public void keyReleased(KeyEvent e) {
            String address = null;
            if (addressTextField != null) {
                address = addressTextField.getText();
                if (!WhitespaceTrimmer.trim(address).equals(address)) {
                    address = WhitespaceTrimmer.trim(address);
                    addressTextField.setText(address);
                }
            }
            String label = labelTextArea.getText();
            String amountFiat = amountFiatTextField.getText();
            String amountWDCAsString = updateWDCAmount(amountFiat);

            displayQRCode(address, amountWDCAsString, label);
        }
    }
    
    protected void updateFiatAmount() {
        // Convert the WDC into fiat and populate the fiat amount label.
        if (CurrencyConverter.INSTANCE.getRate() != null && CurrencyConverter.INSTANCE.isShowingFiat()) {
            try {
                if (parsedAmountWDC != null) {
                    parsedAmountFiat = CurrencyConverter.INSTANCE.convertFromWDCToFiat(parsedAmountWDC.getAmount().toBigInteger());
                    String fiatText = CurrencyConverter.INSTANCE.getFiatAsLocalisedString(parsedAmountFiat, false, false);
                    if (amountFiatTextField != null) {
                        amountFiatTextField.setText(fiatText);
                    }
                } else {
                    if (amountFiatTextField != null) {
                        amountFiatTextField.setText("");
                    }                    
                }
            } catch (NumberFormatException nfe) {
                log.debug("updateFieldAmount: " + nfe.getClass().getName() + " " + nfe.getMessage());
            }
        }
    }

    protected String updateWDCAmount(String amountFiat) {
        CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.convertFromFiatToWDC(amountFiat);
        if (converterResult.isFiatMoneyValid() && converterResult.isWdcMoneyValid()) {
            parsedAmountWDC = converterResult.getWdcMoney();
            String amountWDCAsString = controller.getLocaliser().worldcoinValueToStringNotLocalised(parsedAmountWDC.getAmount().toBigInteger(), false,
                    false);
            worldcoinController.getModel().setActiveWalletPreference(thisAbstractTradePanel.getAmountConstant(), amountWDCAsString);
            worldcoinController.getModel().getActivePerWalletModelData().setDirty(true);

            amountWDCTextField.setText(CurrencyConverter.INSTANCE.getWDCAsLocalisedString(parsedAmountWDC));
            if (notificationLabel != null) {
                notificationLabel.setText("");
            }
            return amountWDCAsString;
        } else {
            parsedAmountWDC = null;
            amountWDCTextField.setText("");
            if (notificationLabel != null) {
                String message = "";
                if (!converterResult.isFiatMoneyValid()) {
                    message = converterResult.getFiatMessage();
                    if (message == null) {
                        message = "";
                    }
                }
                if (!converterResult.isWdcMoneyValid() && converterResult.getWdcMessage() != null) {
                    if (message.length() > 0) {
                        message = message + ". ";
                    }
                    message = message + converterResult.getWdcMessage();
                }
                notificationLabel.setText(message);
            }
            return null;
        }
    }

    /**
     * Display the address, amount and label as a QR code.
     */
    public void displayQRCode(String address, String amount, String label) {
        if (qrCodeGenerator == null) {
            qrCodeGenerator = new QRCodeGenerator(this.worldcoinController);
        }
        try {
            BufferedImage image = qrCodeGenerator.generateQRcode(address, amount, label);
            ImageIcon icon;
            if (image != null) {
                icon = new ImageIcon(image);
            } else {
                icon = new ImageIcon();
            }
            if (qrCodeLabel != null) {
                qrCodeLabel.setIcon(icon);
                setDragLabelTextAndTooltip();
            }
        } catch (RuntimeException re) {
            // QR code generation failed
            log.error(re.getMessage(), re);
        }
    }

    public boolean processDroppedImage(Image image) {
        if (image == null) {
            return false;
        }

        BufferedImage bufferedImage;
        log.debug("importData - 2.1");
        if (image.getWidth(qrCodeLabel) + MINIMUM_QRCODE_PANEL_HORIZONTAL_SPACING > qrCodeLabel.getWidth()
                || image.getHeight(qrCodeLabel) + MINIMUM_QRCODE_PANEL_VERTICAL_SPACING > qrCodeLabel.getHeight()) {
            // scale image
            double qrCodeWidth = (double) qrCodeLabel.getWidth();
            double qrCodeHeight = (double) qrCodeLabel.getHeight();
            double xScale = qrCodeWidth / (double) (image.getWidth(qrCodeLabel) + MINIMUM_QRCODE_PANEL_HORIZONTAL_SPACING);
            double yScale = qrCodeHeight / (double) (image.getHeight(qrCodeLabel) + MINIMUM_QRCODE_PANEL_VERTICAL_SPACING);
            double scaleFactor = Math.min(xScale, yScale);
            bufferedImage = toBufferedImage(image, (int) (image.getWidth(qrCodeLabel) * scaleFactor),
                    (int) (image.getHeight(qrCodeLabel) * scaleFactor));
        } else {
            // no resize
            bufferedImage = toBufferedImage(image, -1, -1);
        }
        log.debug("importData - 2.2");
        ImageIcon icon = new ImageIcon(bufferedImage);

        // decode the QRCode to a String
        QRCodeEncoderDecoder qrCodeEncoderDecoder = new QRCodeEncoderDecoder(image.getWidth(qrCodeLabel),
                image.getHeight(qrCodeLabel));
        log.debug("importData - 2.3");

        String decodedString = qrCodeEncoderDecoder.decode(toBufferedImage(image, -1, -1));
        log.debug("importData - 3 - decodedResult = {}", decodedString);
        log.info("importData = decodedString = {}", decodedString);
        return processDecodedString(decodedString, icon);
    }

    public boolean processDecodedString(String decodedString, ImageIcon icon) {
        // check to see if the wallet files have changed
        WalletData perWalletModelData = this.worldcoinController.getModel().getActivePerWalletModelData();
        boolean haveFilesChanged = this.worldcoinController.getFileHandler().haveFilesChanged(perWalletModelData);

        if (haveFilesChanged) {
            // set on the perWalletModelData that files have changed and fire
            // data changed
            perWalletModelData.setFilesHaveBeenChangedByAnotherProcess(true);
            this.worldcoinController.fireFilesHaveBeenChangedByAnotherProcess(perWalletModelData);
            return false;
        } else {
            // decode the string to an WalletAddressBookData
            // TODO Consider handling the possible runtime exception at a
            // suitable level for recovery

            // Early WorldcoinWallet versions did not URL encode the label hence may
            // have illegal embedded spaces - convert to ENCODED_SPACE_CHARACTER
            // i.e be lenient
            String uriString = decodedString.replace(" ", WorldcoinController.ENCODED_SPACE_CHARACTER);
            WorldcoinURI worldcoinURI;
            try {
                worldcoinURI = new WorldcoinURI(this.worldcoinController.getModel().getNetworkParameters(), uriString);
            } catch (WorldcoinURIParseException e) {
                Message message = new Message(e.getClass().getName() +  " " + e.getMessage());
                MessageManager.INSTANCE.addMessage(message);
                return false;
            }
            log.debug("AbstractTradePanel - ping 1");
            Address address = worldcoinURI.getAddress();
            log.debug("AbstractTradePanel - ping 2");
            String addressString = address.toString();
            log.debug("AbstractTradePanel - ping 3");
            String amountString = "";
            String amountStringLocalised = "";
            if (amountWDCTextField != null) {
                CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDC(amountWDCTextField.getText());
                
                if (converterResult.isWdcMoneyValid()) {
                    parsedAmountWDC = converterResult.getWdcMoney();
                    amountString = controller.getLocaliser().worldcoinValueToStringNotLocalised(parsedAmountWDC.getAmount().toBigInteger(), false, false);
                    amountStringLocalised = CurrencyConverter.INSTANCE.getWDCAsLocalisedString(parsedAmountWDC);
                } else {
                    parsedAmountWDC = null;
                    if (notificationLabel != null) {
                        notificationLabel.setText(converterResult.getWdcMessage());
                    }
                }
            }
            if (worldcoinURI.getAmount() != null) {
                amountString = controller.getLocaliser().worldcoinValueToStringNotLocalised(worldcoinURI.getAmount(), false, false);
                parsedAmountWDC = Money.of(CurrencyConverter.INSTANCE.WORLDCOIN_CURRENCY_UNIT, new BigDecimal(worldcoinURI.getAmount()));
                amountStringLocalised = CurrencyConverter.INSTANCE.getWDCAsLocalisedString(parsedAmountWDC);
            }
            log.debug("AbstractTradePanel - ping 4");
            String decodedLabel = "";
            try {
                if (worldcoinURI.getLabel() != null) {
                    decodedLabel = java.net.URLDecoder.decode(worldcoinURI.getLabel(), "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            log.debug("AbstractTradePanel#processDecodedString addressString = " + addressString + ", amountString = " + amountString
                    + ", label = " + decodedLabel);
            log.debug("AbstractTradePanel - ping 5");

            WalletAddressBookData addressBookData = new WalletAddressBookData(decodedLabel, addressString);
            log.debug("AbstractTradePanel - ping 6");
            // see if the address is already in the address book
            // see if the current address is on the table and
            // select it
            int rowToSelectModel = addressesTableModel.findRowByAddress(addressBookData.getAddress(), false);
            if (rowToSelectModel >= 0) {
                addressesTableModel.setAddressBookDataByRow(addressBookData, rowToSelectModel, false);
                selectedAddressRowModel = rowToSelectModel;

                selectRowInTableFromModelRow(rowToSelectModel);
            } else {
                // add a new row to the table
                this.worldcoinController.getModel().getActiveWalletInfo().addSendingAddress(addressBookData);
                this.worldcoinController.getModel().getActivePerWalletModelData().setDirty(true);

                addressesTableModel.fireTableDataChanged();

                // select new row
                rowToSelectModel = addressesTableModel.findRowByAddress(addressBookData.getAddress(), false);
                if (rowToSelectModel >= 0) {
                    selectedAddressRowModel = rowToSelectModel;

                    selectRowInTableFromModelRow(rowToSelectModel);
                }
            }
            addressesTable.invalidate();
            addressesTable.validate();
            addressesTable.repaint();
            mainFrame.invalidate();
            mainFrame.validate();
            mainFrame.repaint();

            log.debug("AbstractTradePanel - ping 7");
            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_ADDRESS, addressString);
            log.debug("AbstractTradePanel - ping 8");
            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_LABEL, decodedLabel);
            log.debug("AbstractTradePanel - ping 9");

            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.SEND_AMOUNT, amountString);
            log.debug("AbstractTradePanel - ping 10");
            addressTextField.setText(addressString);
            log.debug("AbstractTradePanel - ping 11");
            amountWDCTextField.setText(amountStringLocalised);
            log.debug("AbstractTradePanel - ping 12");
            labelTextArea.setText(decodedLabel);
            log.debug("AbstractTradePanel - ping 13");
            updateFiatAmount();
            log.debug("AbstractTradePanel - ping 14");
            Message message = new Message("");
            MessageManager.INSTANCE.addMessage(message);

            if (icon != null) {
                qrCodeLabel.setIcon(icon);
                setDragLabelTextAndTooltip();
            } else {
                displayQRCode(addressString, amountString, decodedLabel);
            }
            
            checkDeleteSendingEnabled();

            return true;
        }
    }

    /**
     * select the rows that correspond to the current data
     */
    public void selectRows() {
        // stop listener firing
        addressesListener.setEnabled(false);

        String address = this.worldcoinController.getModel().getActiveWalletPreference(getAddressConstant());
        String amount = "";
        if (amountWDCTextField != null) {
            CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDC(amountWDCTextField.getText());
            
            if (converterResult.isWdcMoneyValid()) {
                parsedAmountWDC = converterResult.getWdcMoney();
                amount = controller.getLocaliser().worldcoinValueToStringNotLocalised(parsedAmountWDC.getAmount().toBigInteger(), false, false);
            }
        }
        displayQRCode(address, amount, labelTextArea.getText());

        // see if the current address is on the table and select it
        int rowToSelectModel = addressesTableModel.findRowByAddress(address, isReceiveWorldcoin());
        if (rowToSelectModel >= 0) {
            selectedAddressRowModel = rowToSelectModel;

            selectRowInTableFromModelRow(rowToSelectModel);
        }

        addressesTable.invalidate();
        addressesTable.validate();
        addressesTable.repaint();

        // enable listener
        addressesListener.setEnabled(true);
    }

    private void selectRowInTableFromModelRow(int rowToSelectModel) {
        if (rowToSelectModel < addressesTableModel.getRowCount()) {
            try {
                int rowToSelect = addressesTable.convertRowIndexToView(rowToSelectModel);
                addressesListener.setEnabled(false);
                addressesTable.getSelectionModel().setSelectionInterval(rowToSelect, rowToSelect);
                addressesListener.setEnabled(true);

                // scroll to visible
                addressesTable.scrollRectToVisible(addressesTable.getCellRect(rowToSelect, 0, false));
            } catch (ArrayIndexOutOfBoundsException e) {
                // absorb - row wrong
            }
        }
    }

    public JTextArea getLabelTextArea() {
        return labelTextArea;
    }

    private BufferedImage toBufferedImage(Image image, int width, int height) {
        log.debug("toBufferedImage - 1");
        if (image == null) {
            return null;
        }
        if (width == -1) {
            width = image.getWidth(null);
        }
        if (height == -1) {
            height = image.getHeight(null);
        }
        // draw original image to thumbnail image object and
        // scale it to the new size on-the-fly
        log.debug("toBufferedImage - 2.2, image = {} ,width = {}, height = {}", new Object[] { image, width, height });

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        log.debug("toBufferedImage - 2.3, bufferedImage = {}", bufferedImage);

        Graphics2D g2 = bufferedImage.createGraphics();

        log.debug("toBufferedImage - 3");
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, width, height, null);
        log.debug("toBufferedImage - 4");
        g2.dispose();
        return bufferedImage;
    }

    public AddressBookTableModel getAddressesTableModel() {
        return addressesTableModel;
    }

    public JTable getAddressesTable() {
        return addressesTable;
    }
    
    private String createCenteredMultilineLabelText(String labelText) {
        StringBuilder centeredText = new StringBuilder("<html><center>");
        String[] lines = labelText.split("\\|");
        if (lines != null) {

            for (int i = 0; i < lines.length ; i++) {
                if ( i > 0) {
                    centeredText.append("<br>");
                }
                centeredText.append(lines[i]);
            }
        }
        centeredText.append("</center></html>");
        
        return centeredText.toString();
    }

    // CopyQRCodeImageDataProvider methods
    @Override
    public JLabel getURIImage() {
        return qrCodeLabel;
    }
    
    @Override
    public String getAddress() {
        if (addressTextField != null) {
            String address = addressTextField.getText();
            return WhitespaceTrimmer.trim(address);

        } else {
            return null;
        }
    }
    
    @Override
    public String getLabel() {
        if (labelTextArea != null) {
            return labelTextArea.getText();
        } else {
            return null;
        }
    }
    
    @Override
    public String getAmount() {
        String amount = null;
        if (amountWDCTextField != null) {
            CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDC(amountWDCTextField.getText());
            
            if (converterResult.isWdcMoneyValid()) {
                amount = controller.getLocaliser().worldcoinValueToStringNotLocalised(converterResult.getWdcMoney().getAmount().toBigInteger(), false, false);
            }
        }
       return amount;
    }

    @Override
    public String getAmountFiat() {
       if (amountFiatTextField != null) {
           return amountFiatTextField.getText();
       } else {
           return null;
       }
    }

    public WorldcoinWalletTextField getAddressTextField() {
        return addressTextField;
    }
    
    @Override
    public void lostExchangeRate(ExchangeRate exchangeRate) {
        // TODO Auto-generated method stub
    }

    @Override
    public void foundExchangeRate(ExchangeRate exchangeRate) {
        updatedExchangeRate(exchangeRate);
    }

    @Override
    public void updatedExchangeRate(ExchangeRate exchangeRate) {
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run() {
                // Make fiat fields visible if currency is available.
                boolean fiatIsVisible = CurrencyConverter.INSTANCE.isShowingFiat() && CurrencyConverter.INSTANCE.getRate() != null;
                if (amountUnitFiatLabel != null) {
                    amountUnitFiatLabel.setVisible(fiatIsVisible);
                }
                if (amountFiatTextField != null) {
                    amountFiatTextField.setVisible(fiatIsVisible);
                }
                if (amountEqualsLabel != null) {
                    amountEqualsLabel.setVisible(fiatIsVisible);
                }
                if (amountWDCTextField != null) {
                    CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDC(amountWDCTextField.getText());
                    if (converterResult.isWdcMoneyValid()) {
                        parsedAmountWDC = converterResult.getWdcMoney();
                        updateFiatAmount();
                    }
                    // If the conversion fails this is probably an error in one the amount fields so just leave it.
                }
            }});
    }   
}
