package org.wallet.viewsystem.swing.view.panels;

import org.wallet.controller.worldcoin.WorldcoinController;
import org.worldcoinj.wallet.Protos.Wallet.EncryptionType;
import org.wallet.controller.Controller;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.swing.action.CancelBackToParentAction;
import org.wallet.viewsystem.swing.action.CreateNewReceivingAddressSubmitAction;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletButton;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletLabel;
import org.wallet.viewsystem.swing.view.components.WorldcoinWalletTitledPanel;
import org.wallet.viewsystem.swing.view.dialogs.CreateNewReceivingAddressDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * JPanel for creating new receiving addresses.
 * (The JPanel is separated out of the JDialog to enable running tests headless on a server).
 * 
 * @author jim
 *
 */
public class CreateNewReceivingAddressPanel extends JPanel {
    private static final long serialVersionUID = -1604698555807842464L;
    
    private final Controller controller;
    private final WorldcoinController worldcoinController;
 
    private ReceiveWorldcoinPanel receiveWorldcoinPanel;
    private CreateNewReceivingAddressDialog createNewReceivingAddressDialog;

    private WorldcoinWalletLabel messageText;
    
    private CreateNewReceivingAddressSubmitAction createNewReceivingAddressSubmitAction;
    private WorldcoinWalletButton createNewReceivingAddressSubmitButton;
    private WorldcoinWalletButton cancelButton;
    
    private JPasswordField walletPasswordField;
    private WorldcoinWalletLabel walletPasswordPromptLabel;
    
    private JComboBox numberOfAddresses;
    
    private static final int STENT_WIDTH = 10;
   
    public CreateNewReceivingAddressPanel(WorldcoinController worldcoinController, ReceiveWorldcoinPanel receiveWorldcoinPanel,
            CreateNewReceivingAddressDialog createNewReceivingAddressDialog) {
        super();

        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        
        this.receiveWorldcoinPanel = receiveWorldcoinPanel;
        this.createNewReceivingAddressDialog = createNewReceivingAddressDialog;

        setOpaque(false);
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(STENT_WIDTH), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 7;
        constraints.gridy = 1;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(STENT_WIDTH), constraints);

        WorldcoinWalletLabel explainLabel = new WorldcoinWalletLabel("");
        explainLabel.setText(controller.getLocaliser().getString("createNewReceivingAddressDialog.message"));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.weightx = 0.8;
        constraints.weighty = 1.0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(explainLabel, constraints);

        numberOfAddresses = new JComboBox();
        numberOfAddresses.addItem(1);
        numberOfAddresses.addItem(5);
        numberOfAddresses.addItem(20);
        numberOfAddresses.addItem(100);
        //numberOfAddresses.addItem(new Integer(500));

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 5;
        constraints.gridy = 1;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(numberOfAddresses, constraints);

        // Add wallet password field.
        walletPasswordPromptLabel = new WorldcoinWalletLabel(controller.getLocaliser().getString(
                "showExportPrivateKeysPanel.walletPasswordPrompt"));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(walletPasswordPromptLabel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 4;
        constraints.gridy = 7;
        constraints.weightx = 0.05;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(WorldcoinWalletTitledPanel.createStent(WorldcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS), constraints);

        JPanel filler4 = new JPanel();
        filler4.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 7;
        constraints.weightx = 0.3;
        constraints.weighty = 0.01;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler4, constraints);

        walletPasswordField = new JPasswordField(24);
        walletPasswordField.setMinimumSize(new Dimension(200, 20));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 5;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridheight = 1;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(walletPasswordField, constraints);

        JPanel filler5 = new JPanel();
        filler5.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.01;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler5, constraints);

        if (this.worldcoinController.getModel().getActiveWallet() != null) {
            if (this.worldcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
                // Need wallet password.
                walletPasswordField.setEnabled(true);
                walletPasswordPromptLabel.setEnabled(true);
            } else {
                // No wallet password required.
                walletPasswordField.setEnabled(false);
                walletPasswordPromptLabel.setEnabled(false);
            }
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 10;
        constraints.weightx = 0.8;
        constraints.weighty = 0.1;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(buttonPanel, constraints);

        CancelBackToParentAction cancelAction = new CancelBackToParentAction(controller,
                ImageLoader.createImageIcon(ImageLoader.CROSS_ICON_FILE), createNewReceivingAddressDialog);
        cancelButton = new WorldcoinWalletButton(cancelAction, controller);
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Clear password.
                if (walletPasswordField != null) {
                    walletPasswordField.setText("");
                }
            }});
 
        if (createNewReceivingAddressDialog != null) {
            createNewReceivingAddressDialog.addWindowListener(new WindowAdapter(){
                @Override
                public void windowClosed(WindowEvent arg0) {
                    if (walletPasswordField != null) {
                        walletPasswordField.setText("");
                    }
                }
            });
        }
        buttonPanel.add(cancelButton);

        createNewReceivingAddressSubmitAction = new CreateNewReceivingAddressSubmitAction(this.worldcoinController,
                createNewReceivingAddressDialog, this, walletPasswordField);
        createNewReceivingAddressSubmitButton = new WorldcoinWalletButton(createNewReceivingAddressSubmitAction, controller);
        buttonPanel.add(createNewReceivingAddressSubmitButton);

        messageText = new WorldcoinWalletLabel("");
        messageText.setText(" ");
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 11;
        constraints.weightx = 0.8;
        constraints.weighty = 0.15;
        constraints.gridwidth = 4;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(messageText, constraints);

        JLabel filler3 = new JLabel();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 7;
        constraints.gridy = 11;
        constraints.weightx = 0.05;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler3, constraints);

        JLabel filler6 = new JLabel();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 7;
        constraints.gridy = 12;
        constraints.weightx = 0.05;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(filler6, constraints);
    }
 
    public void setMessageText(String message) {
        messageText.setText(message);
    }

    public ReceiveWorldcoinPanel getReceiveWorldcoinPanel() {
        return receiveWorldcoinPanel;
    }

    public int getNumberOfAddressesToCreate() {
        return ((Integer) numberOfAddresses.getSelectedItem()).intValue();
    }

    public JComboBox getNumberOfAddresses() {
        return numberOfAddresses;
    }

    // Accessors used in testing.
    public CreateNewReceivingAddressSubmitAction getCreateNewReceivingAddressSubmitAction() {
        return createNewReceivingAddressSubmitAction;
    }

    public String getMessageText() {
        return messageText.getText();
    }

    public void setWalletPassword(String walletPassword) {
        walletPasswordField.setText(walletPassword);
    }

    public WorldcoinWalletButton getCancelButton() {
        return cancelButton;
    }

    public boolean isWalletPasswordFieldEnabled() {
        return walletPasswordField.isEnabled();
    }
}
