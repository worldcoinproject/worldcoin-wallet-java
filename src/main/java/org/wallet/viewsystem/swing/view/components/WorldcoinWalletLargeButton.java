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
package org.wallet.viewsystem.swing.view.components;

import org.wallet.controller.Controller;
import org.wallet.viewsystem.swing.ColorAndFontConstants;

import javax.swing.*;

/**
 * button used in toolbar on WorldcoinWallet Swing UI
 * @author jim
 *
 */
public class WorldcoinWalletLargeButton extends JButton {

    private static final long serialVersionUID = 5674557290711815650L;
   
    public WorldcoinWalletLargeButton(Action action, Controller controller) {
        super(action);

        setFont(FontSizer.INSTANCE.getAdjustedDefaultFontWithDelta(ColorAndFontConstants.WORLDCOIN_WALLET_LARGE_FONT_INCREASE));

        setOpaque(false);
        setRolloverEnabled(true);
    }
}
