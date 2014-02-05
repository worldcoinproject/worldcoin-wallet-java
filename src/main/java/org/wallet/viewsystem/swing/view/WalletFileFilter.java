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

import java.io.File;

import org.wallet.controller.Controller;
import org.wallet.file.BackupManager;
import org.wallet.model.worldcoin.WorldcoinModel;

public class WalletFileFilter extends javax.swing.filechooser.FileFilter {

    private static final String MAC_APPLICATION_SUFFIX = ".app";
    private Controller controller;
    
    public WalletFileFilter(Controller controller) {
        this.controller = controller;
    }
    
    @Override
    public boolean accept(File file) {
        return (file.isDirectory() && !(file.getName().toLowerCase().endsWith(MAC_APPLICATION_SUFFIX))) 
        || (file.getName().toLowerCase().endsWith(WorldcoinModel.WALLET_FILE_EXTENSION)
        || (file.getName().toLowerCase().endsWith(WorldcoinModel.WALLET_FILE_EXTENSION + "." + BackupManager.FILE_ENCRYPTED_WALLET_SUFFIX)));
    }

    @Override
    public String getDescription() {
        String worldcoinWalletText = controller.getLocaliser().getString("worldcoinWalletFrame.title");
        return worldcoinWalletText + " (*." + WorldcoinModel.WALLET_FILE_EXTENSION + ")";
    }
}
