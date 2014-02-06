package org.wallet.viewsystem.dataproviders;


/**
 * DataProvider for show open URI and cancel actions
 * @author jim
 *
 */
public interface ShowUriDialogDataProvider extends WorldcoinFormDataProvider {
    
    /**
     * Get the boolean dictating whether to show the open URI dialog or not
     */
    public boolean isShowUriDialog();
}
