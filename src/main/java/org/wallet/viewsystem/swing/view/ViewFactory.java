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
package org.wallet.viewsystem.swing.view;

import java.util.EnumMap;
import java.util.Map;

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.controller.exchange.ExchangeController;
import org.wallet.viewsystem.View;
import org.wallet.viewsystem.Viewable;
import org.wallet.viewsystem.swing.WorldcoinWalletFrame;
import org.wallet.viewsystem.swing.view.dialogs.ShowOpenUriDialog;
import org.wallet.viewsystem.swing.view.panels.AddPasswordPanel;
import org.wallet.viewsystem.swing.view.panels.ChangePasswordPanel;
import org.wallet.viewsystem.swing.view.panels.ChartsPanel;
import org.wallet.viewsystem.swing.view.panels.ExportPrivateKeysPanel;
import org.wallet.viewsystem.swing.view.panels.HelpAboutPanel;
import org.wallet.viewsystem.swing.view.panels.HelpContentsPanel;
import org.wallet.viewsystem.swing.view.panels.ImportPrivateKeysPanel;
import org.wallet.viewsystem.swing.view.panels.MessagesPanel;
import org.wallet.viewsystem.swing.view.panels.ReceiveWorldcoinPanel;
import org.wallet.viewsystem.swing.view.panels.RemovePasswordPanel;
import org.wallet.viewsystem.swing.view.panels.ResetTransactionsPanel;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinPanel;
import org.wallet.viewsystem.swing.view.panels.ShowPreferencesPanel;
import org.wallet.viewsystem.swing.view.panels.ShowTransactionsPanel;
import org.wallet.viewsystem.swing.view.panels.SignMessagePanel;
import org.wallet.viewsystem.swing.view.panels.VerifyMessagePanel;
import org.wallet.viewsystem.swing.view.panels.WelcomePanel;

/**
 * a factory class that lazy loads views
 * 
 * @author jim
 * 
 */
public class ViewFactory {
    private Map<View, Viewable> viewMap;

    private final Controller controller;
    private final WorldcoinController worldcoinController;
    private final ExchangeController exchangeController;
    
    private final WorldcoinWalletFrame mainFrame;

    public ViewFactory(WorldcoinController worldcoinController, ExchangeController exchangeController, WorldcoinWalletFrame mainFrame) {
        this.worldcoinController = worldcoinController;
        this.exchangeController = exchangeController;
        this.controller = this.worldcoinController;
        this.mainFrame = mainFrame;
        initialise();
    }
    
    public final void initialise() {
        viewMap = new EnumMap<View, Viewable>(View.class);        
    }

    public Viewable getView(View viewNumber) {
        Viewable viewToReturn = viewMap.get(viewNumber);

        if (viewToReturn == null) {
            viewToReturn = createView(viewNumber);
        }

        return viewToReturn;
    }

    public void addView(View viewNumber, Viewable view) {
        viewMap.put(viewNumber, view);
    }
    
    private Viewable createView(View viewNumber) {
        Viewable viewToReturn = null;

        switch (viewNumber) {

        case SAME_VIEW: {
            assert false;
            break;
        }

        case WELCOME_VIEW: {
            viewToReturn = new WelcomePanel(controller, mainFrame);
            break;
        }

        case TRANSACTIONS_VIEW: {
            viewToReturn = new ShowTransactionsPanel(this.worldcoinController, mainFrame);
            break;
        }

        case HELP_ABOUT_VIEW: {
            viewToReturn = new HelpAboutPanel(controller, mainFrame);
            break;
        }

        case HELP_CONTENTS_VIEW: {
            viewToReturn = new HelpContentsPanel(controller, mainFrame);
            break;
        }
               
        case RECEIVE_WORLDCOIN_VIEW: {
            viewToReturn = new ReceiveWorldcoinPanel(this.worldcoinController, mainFrame);
            break;
        }
        
        case SEND_WORLDCOIN_VIEW: {
            viewToReturn = new SendWorldcoinPanel(this.worldcoinController, mainFrame);
            break;
        }
        
        case PREFERENCES_VIEW: {
            viewToReturn = new ShowPreferencesPanel(this.worldcoinController, this.exchangeController, mainFrame);
            break;
        }

        case RESET_TRANSACTIONS_VIEW: {
            viewToReturn = new ResetTransactionsPanel(this.worldcoinController, mainFrame);
            break;
        }

        case SHOW_OPEN_URI_DIALOG_VIEW: {
            viewToReturn = new ShowOpenUriDialog(this.worldcoinController, mainFrame);
            break;
        }

        case SIGN_MESSAGE_VIEW: {
            viewToReturn = new SignMessagePanel(this.worldcoinController, mainFrame);
            break;
        }

        case VERIFY_MESSAGE_VIEW: {
            viewToReturn = new VerifyMessagePanel(this.worldcoinController, mainFrame);
            break;
        }

        case SHOW_IMPORT_PRIVATE_KEYS_VIEW: {
            viewToReturn = new ImportPrivateKeysPanel(this.worldcoinController, mainFrame);
            break;
        }

        case SHOW_EXPORT_PRIVATE_KEYS_VIEW: {
            viewToReturn = new ExportPrivateKeysPanel(this.worldcoinController, mainFrame);
            break;
        }

        case MESSAGES_VIEW: {
            viewToReturn = new MessagesPanel(controller, mainFrame);
            break;
        }
        
        case ADD_PASSWORD_VIEW: {
            viewToReturn = new AddPasswordPanel(this.worldcoinController, mainFrame);
            break;
        }
        
        case CHANGE_PASSWORD_VIEW: {
            viewToReturn = new ChangePasswordPanel(this.worldcoinController, mainFrame);
            break;
        }
        
        case REMOVE_PASSWORD_VIEW: {
            viewToReturn = new RemovePasswordPanel(this.worldcoinController, mainFrame);
            break;
        }

        case CHARTS_VIEW: {
            viewToReturn = new ChartsPanel(this.worldcoinController, mainFrame);
            break;
        }

        default: {
        }
        }

        if (viewToReturn != null) {
            viewMap.put(viewNumber, viewToReturn);
        }
        return viewToReturn;
    }
}
