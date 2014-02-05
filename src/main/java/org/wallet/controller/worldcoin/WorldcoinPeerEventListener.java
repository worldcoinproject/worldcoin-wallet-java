package org.wallet.controller.worldcoin;

import com.google.worldcoin.core.*;
import org.wallet.controller.Controller;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.core.StatusEnum;
import org.wallet.network.ReplayManager;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinConfirmPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WorldcoinPeerEventListener implements PeerEventListener {

  private Logger log = LoggerFactory.getLogger(WorldcoinPeerEventListener.class);

  private final Controller controller;
  private final WorldcoinController worldcoinController;

  public WorldcoinPeerEventListener(WorldcoinController worldcoinController) {
    this.worldcoinController = worldcoinController;
    this.controller = this.worldcoinController;
  }

  @Override
  public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
    this.worldcoinController.fireBlockDownloaded();

    if (blocksLeft == 0) {
      ReplayManager.INSTANCE.downloadHasCompleted();
    }
  }

  @Override
  public void onChainDownloadStarted(Peer peer, int blocksLeft) {
    if (blocksLeft == 0) {
      ReplayManager.INSTANCE.downloadHasCompleted();
    }
    this.worldcoinController.fireBlockDownloaded();
  }

  @Override
  public void onPeerConnected(Peer peer, int peerCount) {
    if (peer != null) {
      log.debug("Connected to peer:" + peer.getPeerVersionMessage());
    }
    if (peerCount >= 1) {
      controller.setOnlineStatus(StatusEnum.ONLINE);
    }
    if (controller.getModel() != null) {
      this.worldcoinController.getModel().setNumberOfConnectedPeers(peerCount);
    }
    SendWorldcoinConfirmPanel.updatePanel();
  }

  @Override
  public void onPeerDisconnected(Peer peer, int peerCount) {
    if (peer != null) {
      log.debug("Disconnected from peer, address : " + peer.getAddress() + ", peerCount = " + peerCount);
    }
    if (peerCount == 0) {
      controller.setOnlineStatus(StatusEnum.CONNECTING);
    }
    if (controller.getModel() != null) {
      this.worldcoinController.getModel().setNumberOfConnectedPeers(peerCount);
    }
    SendWorldcoinConfirmPanel.updatePanel();
  }

  @Override
  public Message onPreMessageReceived(Peer peer, Message message) {
    return message;
  }

  @Override
  public void onTransaction(Peer peer, Transaction transaction) {
    // Loop through all the wallets, seeing if the transaction is relevant and adding them as pending if so.
    if (transaction != null) {
      try {
        java.util.List<WalletData> perWalletModelDataList = worldcoinController.getModel().getPerWalletModelDataList();

        if (perWalletModelDataList != null) {
          for (WalletData perWalletModelData : perWalletModelDataList) {
            Wallet loopWallet = perWalletModelData.getWallet();
            if (loopWallet != null) {
              if (loopWallet.isTransactionRelevant(transaction)) {
                if (!(transaction.isTimeLocked() && transaction.getConfidence().getSource() != TransactionConfidence.Source.SELF)) {
                  if (loopWallet.getTransaction(transaction.getHash()) == null) {
                    log.debug("WorldcoinWallet adding a new pending transaction for the wallet '"
                            + perWalletModelData.getWalletDescription() + "'\n" + transaction.toString());
                    // The perWalletModelData is marked as dirty.
                    if (perWalletModelData.getWalletInfo() != null) {
                      synchronized (perWalletModelData.getWalletInfo()) {
                        perWalletModelData.setDirty(true);
                      }
                    } else {
                      perWalletModelData.setDirty(true);
                    }
                    loopWallet.receivePending(transaction, null);
                  }
                }
              }
            }
          }
        }
      } catch (ScriptException e) {
        log.error(e.getMessage(), e);
      } catch (VerificationException e) {
        log.error(e.getMessage(), e);
      }
    }
  }


  @Override
  public List<Message> getData(Peer peer, GetDataMessage m) {
    return null;
  }
}
