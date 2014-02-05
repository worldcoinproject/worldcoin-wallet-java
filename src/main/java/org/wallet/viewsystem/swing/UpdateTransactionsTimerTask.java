package org.wallet.viewsystem.swing;

import java.util.TimerTask;
import javax.swing.SwingUtilities;

import org.wallet.controller.Controller;
import org.wallet.viewsystem.DisplayHint;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.swing.view.panels.ShowTransactionsPanel;

public class UpdateTransactionsTimerTask extends TimerTask {
    private Controller controller;
    private ShowTransactionsPanel transactionsPanel;
    private WorldcoinWalletFrame mainFrame;

    private boolean updateTransactions = false;
    private boolean isCurrentlyUpdating = false;


    public UpdateTransactionsTimerTask(Controller controller, final ShowTransactionsPanel transactionsPanel,
            WorldcoinWalletFrame mainFrame) {
        this.controller = controller;
        this.transactionsPanel = transactionsPanel;
        this.mainFrame = mainFrame;
    }

    @Override
    public void run() {
        // If still updating from the last time, skip this firing.
        // Timer thread.
        if (!isCurrentlyUpdating) {
            // If viewing transactions, refresh the screen so that transaction
            // confidence icons can update.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    boolean updateThisTime = false;
                    if (updateTransactions) {
                        updateTransactions = false;
                        updateThisTime = true;
                    }

                    if (updateThisTime) {
                        mainFrame.updateHeader();
                        if (controller.getCurrentView() == View.TRANSACTIONS_VIEW) {
                            // Swing thread.
                            isCurrentlyUpdating = true;
                            try {
                                transactionsPanel.displayView(DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED);
                            } finally {
                                // Swing thread.
                                isCurrentlyUpdating = false;
                            }
                        }
                    }
                }
            });
        }
    }

    public boolean isUpdateTransactions() {
        // Clone before return.
        return updateTransactions ? true : false;
    }

    public void setUpdateTransactions(boolean updateTransactions) {
        this.updateTransactions = updateTransactions;
    }
}
