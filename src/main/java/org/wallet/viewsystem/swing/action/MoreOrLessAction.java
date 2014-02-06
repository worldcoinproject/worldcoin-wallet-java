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
package org.wallet.viewsystem.swing.action;

import org.wallet.controller.Controller;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.viewsystem.swing.view.panels.AbstractTradePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This {@link Action} represents the action to toggle the more or less button
 * on the send/ receive worldcoin panels.
 */
public class MoreOrLessAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(MoreOrLessAction.class);


    private static final long serialVersionUID = 114352235465057705L;

    private Controller controller;
    private AbstractTradePanel abstractTradePanel;

    /**
     * Creates a new {@link MoreOrLessAction}.
     */
    public MoreOrLessAction(Controller controller, AbstractTradePanel abstractTradePanel) {
        super("");
        this.controller = controller;
        this.abstractTradePanel = abstractTradePanel;
    }

    /**
     * Perform the action.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        boolean showSidePanel = !abstractTradePanel.isShowSidePanel();
        abstractTradePanel.setShowSidePanel(showSidePanel);
        
        //log.debug("showSidePanel = " + showSidePanel);

        // Put it in the user preferences - will then get loaded when view form loads.
        controller.getModel().setUserPreference(WorldcoinModel.SHOW_SIDE_PANEL, (Boolean.valueOf(showSidePanel)).toString());
        
        // Display the side panel (or not).
        abstractTradePanel.displaySidePanel();
    }
}