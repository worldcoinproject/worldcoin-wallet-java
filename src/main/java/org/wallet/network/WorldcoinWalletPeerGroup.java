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
package org.wallet.network;

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;

import com.google.worldcoin.core.BlockChain;
import com.google.worldcoin.core.NetworkParameters;
import com.google.worldcoin.core.PeerGroup;


public class WorldcoinWalletPeerGroup extends PeerGroup {
    private final Controller controller;
    private final WorldcoinController worldcoinController;
    WorldcoinWalletDownloadListener worldcoinWalletDownloadListener = null;

    public static final int MAXIMUM_NUMBER_OF_PEERS = 6;

        
    public WorldcoinWalletPeerGroup(WorldcoinController worldcoinController, NetworkParameters params, BlockChain chain) {
        super(params, chain);
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
        worldcoinWalletDownloadListener = new WorldcoinWalletDownloadListener(this.worldcoinController);

        setMaxConnections(MAXIMUM_NUMBER_OF_PEERS);
    }
    
    /**
     * Download the blockchain from peers.
     * 
     * <p>This method wait until the download is complete.  "Complete" is defined as downloading
     * from at least one peer all the blocks that are in that peer's inventory.
     */
    @Override
    public void downloadBlockChain() {
        startBlockChainDownload(worldcoinWalletDownloadListener);
    }

    public WorldcoinWalletDownloadListener getWorldcoinWalletDownloadListener() {
        return worldcoinWalletDownloadListener;
    }
}
