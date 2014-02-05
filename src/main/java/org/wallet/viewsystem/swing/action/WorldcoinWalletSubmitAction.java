package org.wallet.viewsystem.swing.action;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.viewsystem.swing.view.panels.HelpContentsPanel;

/**
 * Abstract super class to check for whether wallet files have changed and
 * whether there is an active wallet available
 * @author jim
 *
 */
public abstract class WorldcoinWalletSubmitAction extends AbstractAction {
    private static final long serialVersionUID = 3750799470657961967L;

    protected final Controller controller;
    protected final WorldcoinController worldcoinController;
    
    /**
     * Creates a new {@link ResetTransactionsSubmitAction}.
     */
    public WorldcoinWalletSubmitAction(WorldcoinController worldcoinController, String textKey, String tooltipKey, String mnemonicKey,  Icon icon) {
        super(worldcoinController.getLocaliser().getString(textKey), icon);
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        
        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString(tooltipKey)));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic(mnemonicKey));
    }
   
    /**
     * Abort due to there not being an active wallet or the wallet has been changed by another process.
     * @return abort True if called method should abort
     */
    public boolean abort() {
        // Check if there is an active wallet.
        if (this.worldcoinController.getModel().thereIsNoActiveWallet()) {
            MessageManager.INSTANCE.addMessage(new Message(controller.getLocaliser().getString("worldcoinWalletSubmitAction.thereIsNoActiveWallet")));
            return true;
        }

        // check to see if another process has changed the active wallet
        WalletData perWalletModelData = this.worldcoinController.getModel().getActivePerWalletModelData();
        boolean haveFilesChanged = this.worldcoinController.getFileHandler().haveFilesChanged(perWalletModelData);
        
        if (haveFilesChanged) {
            // set on the perWalletModelData that files have changed and fire
            // data changed
            perWalletModelData.setFilesHaveBeenChangedByAnotherProcess(true);
            this.worldcoinController.fireFilesHaveBeenChangedByAnotherProcess(perWalletModelData);
 
            return true;
        }
        
        return false;
    }
}
