/**
 * Copyright 2013 wallet.org
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

import com.google.worldcoin.core.CheckpointManager;
import com.google.worldcoin.core.PeerGroup;
import com.google.worldcoin.core.StoredBlock;
import com.google.worldcoin.store.BlockStoreException;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.viewsystem.swing.view.panels.SendWorldcoinPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;
import java.util.Timer;


/**
 * ReplayManager is responsible for updating Wallets that are not updated to
 * WorldcoinWallet's main BlockStore. This happens when: 1) The user imports some
 * private keys 2) They do a 'Reset blockchain and transactions' 3) An out of
 * date wallet is opened 4) Encrypted wallets are opened when the user has used
 * an older version of WorldcoinWallet that does not understand them (they then get out
 * of date).
 */
public enum ReplayManager {
  INSTANCE;

  private static final Logger log = LoggerFactory.getLogger(ReplayManager.class);
  private ReplayManagerTimerTask replayManagerTimerTask;
  private Timer replayManagerTimer;

  /**
   * The actual chain height prior to any replay
   * (as opposed to the current height whilst replaying).
   */
  private int actualLastChainHeight;

  private static final int REPLAY_MANAGER_DELAY_TIME = 0; // ms
  private static final int REPLAY_MANAGER_REPEAT_TIME = 333; // ms

  private WorldcoinController controller;

  private final Queue<ReplayTask> replayTaskQueue = new LinkedList<ReplayTask>();

  private static boolean regularDownloadIsRunning = false;


  public void initialise(WorldcoinController controller, boolean clearQueue) {
    this.controller = controller;

    if (clearQueue) {
      replayTaskQueue.clear();
    }
    replayManagerTimerTask = new ReplayManagerTimerTask(controller, replayTaskQueue);
    replayManagerTimer = new Timer();
    replayManagerTimer.scheduleAtFixedRate(replayManagerTimerTask, REPLAY_MANAGER_DELAY_TIME, REPLAY_MANAGER_REPEAT_TIME);
  }

  /**
   * Synchronise one or more wallets with the blockchain.
   */
  public void syncWallet(final ReplayTask replayTask) throws IOException,
          BlockStoreException {
    log.info("Starting replay task : " + replayTask.toString());

    // Remember the chain height.
    if (controller.getWorldcoinWalletService().getChain() != null) {
      actualLastChainHeight = controller.getWorldcoinWalletService().getChain().getBestChainHeight();
    }

    // Mark the wallets as busy and set the replay task uuid into the model
    List<WalletData> perWalletModelDataList = replayTask.getPerWalletModelDataToReplay();
    if (perWalletModelDataList != null) {
      for (WalletData perWalletModelData : perWalletModelDataList) {
        perWalletModelData.setBusy(true);
        perWalletModelData.setBusyTaskKey("worldcoinWalletDownloadListener.downloadingText");
        perWalletModelData.setBusyTaskVerbKey("worldcoinWalletDownloadListener.downloadingTextShort");
        perWalletModelData.setReplayTaskUUID(replayTask.getUuid());
      }
      controller.fireWalletBusyChange(true);
    }

    Date dateToReplayFrom = replayTask.getStartDate();

    MessageManager.INSTANCE.addMessage(new Message(controller.getLocaliser().getString(
            "resetTransactionsSubmitAction.startReplay")));

    log.debug("Starting replay of blockchain from date = '" + dateToReplayFrom);

    // Reset UI to zero peers.
    controller.getPeerEventListener().onPeerDisconnected(null, 0);

    // Restart peerGroup and download rest of blockchain.
    Message message;
    if (dateToReplayFrom != null) {
      message = new Message(controller.getLocaliser().getString(
              "resetTransactionSubmitAction.replayingBlockchain",
              new Object[]{DateFormat.getDateInstance(DateFormat.MEDIUM, controller.getLocaliser().getLocale()).format(
                      dateToReplayFrom)}), false);
    } else {
      message = new Message(controller.getLocaliser().getString(
              "resetTransactionSubmitAction.replayingBlockchain",
              new Object[]{DateFormat.getDateInstance(DateFormat.MEDIUM, controller.getLocaliser().getLocale()).format(
                      WorldcoinWalletService.genesisBlockCreationDate)}), false);
    }
    MessageManager.INSTANCE.addMessage(message);

    log.debug("About to restart PeerGroup.");
    message = new Message(controller.getLocaliser().getString("worldcoinWalletService.stoppingWorldcoinNetworkConnection"),
            false, 0);
    MessageManager.INSTANCE.addMessage(message);

    controller.getWorldcoinWalletService().getPeerGroup().stopAndWait();
    log.debug("PeerGroup is now stopped.");

    // Reset UI to zero peers.
    controller.getPeerEventListener().onPeerDisconnected(null, 0);

    // Close the blockstore and recreate a new one.
    int newChainHeightAfterTruncate = controller.getWorldcoinWalletService().createNewBlockStoreForReplay(dateToReplayFrom);
    log.debug("dateToReplayFrom = " + dateToReplayFrom + ", newChainHeightAfterTruncate = " + newChainHeightAfterTruncate);
    replayTask.setStartHeight(newChainHeightAfterTruncate);

    // Create a new PeerGroup.
    controller.getWorldcoinWalletService().createNewPeerGroup();
    log.debug("Recreated PeerGroup.");

    // Hook up the download listeners.
    addDownloadListeners(perWalletModelDataList);

    // Start up the PeerGroup.
    PeerGroup peerGroup = controller.getWorldcoinWalletService().getPeerGroup();
    peerGroup.start();
    log.debug("Restarted PeerGroup = " + peerGroup.toString());

    log.debug("About to start  blockchain download.");
    controller.getWorldcoinWalletService().getPeerGroup().downloadBlockChain();
    log.debug("Blockchain download started.");
  }

  public void addDownloadListeners(List<WalletData> perWalletModelDataList) {
    PeerGroup peerGroup = controller.getWorldcoinWalletService().getPeerGroup();
    if (peerGroup instanceof WorldcoinWalletPeerGroup) {
      if (perWalletModelDataList != null) {
        for (WalletData perWalletModelData : perWalletModelDataList) {
          if (perWalletModelData.getSingleWalletDownloadListener() != null) {
            ((WorldcoinWalletPeerGroup) peerGroup).getWorldcoinWalletDownloadListener().addSingleWalletPanelDownloadListener(
                    perWalletModelData.getSingleWalletDownloadListener());
          }
        }
      }
    }
  }

  public void removeDownloadListeners(List<WalletData> perWalletModelDataList) {
    PeerGroup peerGroup = controller.getWorldcoinWalletService().getPeerGroup();
    if (peerGroup instanceof WorldcoinWalletPeerGroup) {
      if (perWalletModelDataList != null) {
        for (WalletData perWalletModelData : perWalletModelDataList) {
          if (perWalletModelData.getSingleWalletDownloadListener() != null) {
            ((WorldcoinWalletPeerGroup) peerGroup).getWorldcoinWalletDownloadListener().removeDownloadListener(
                    perWalletModelData.getSingleWalletDownloadListener());
          }
        }
      }
    }
  }

  /**
   * Add a ReplayTask to the ReplayManager's list of tasks to do.
   *
   * @param replayTask
   */
  public boolean offerReplayTask(ReplayTask replayTask) {
    if (replayTask == null) {
      return false;
    }

    log.debug("Received ReplayTask of " + replayTask.toString());

    // Work out for this replay task where the blockchain will be truncated to.
    int startHeight = replayTask.getStartHeight();
    if (startHeight == ReplayTask.UNKNOWN_START_HEIGHT) {
      File checkpointsFile = new File(controller.getWorldcoinWalletService().getCheckpointsFilename());
      System.out.println("ReplayManager#offerReplayTask checkpointsFile = " + checkpointsFile.getAbsolutePath());
      if (checkpointsFile.exists() && replayTask.getStartDate() != null) {
        FileInputStream stream = null;
        try {
          stream = new FileInputStream(checkpointsFile);
          CheckpointManager checkpointManager = new CheckpointManager(controller.getModel().getNetworkParameters(), stream);
          StoredBlock checkpoint = checkpointManager.getCheckpointBefore(replayTask.getStartDate().getTime() / 1000);
          System.out.println("ReplayManager#offerReplayTask checkpoint = " + checkpoint);
          if (checkpoint != null) {
            startHeight = checkpoint.getHeight();
            System.out.println("ReplayManager#offerReplayTask startHeight = " + startHeight);

            // Store it in the replay task as it will be used for percents.
            replayTask.setStartHeight(startHeight);
          }
        } catch (IOException e) {
          log.error(e.getClass().getName() + " " + e.getMessage());
        } finally {
          if (stream != null) {
            try {
              stream.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }

      }
    }
    log.debug("Actual replayTask offered = " + replayTask.toString());
    synchronized (replayTaskQueue) {
      replayTaskQueue.offer(replayTask);
      String waitingText = "singleWalletPanel.waiting.text";
      String waitingVerb = "singleWalletPanel.waiting.verb";

      for (WalletData perWalletModelData : replayTask.getPerWalletModelDataToReplay()) {
        if (perWalletModelData != null) {
          perWalletModelData.setBusy(true);
          perWalletModelData.setBusyTaskVerbKey(waitingVerb);
          perWalletModelData.setBusyTaskKey(waitingText);

          // Set the height on the wallet to be the startHeight.
          // This means that if the user shuts down MultBit replay on start up
          // will be from the required startHeight.
          perWalletModelData.getWallet().setLastBlockSeenHeight(startHeight);
          perWalletModelData.getWallet().setLastBlockSeenHash(null);
          perWalletModelData.setDirty(true);
        }
      }
    }
    return true;
  }

  /**
   * Called by the downloadlistener when the synchronise completes.
   *
   * @param replayTaskUUID TODO
   */
  public void taskHasCompleted(UUID replayTaskUUID) {
    log.debug("ReplayTask with UUID " + replayTaskUUID + " has completed.");
    // Check the UUID matches the current task.
    ReplayTask currentTask = replayTaskQueue.peek();
    if (currentTask == null) {
      return;
    } else {
      // Not relevant - ignore.
      if (!currentTask.getUuid().equals(replayTaskUUID)) {
        return;
      }
    }

    // Tell the ReplayTimerTask that we are cleaning up.
    replayManagerTimerTask.currentTaskIsTidyingUp(true);

    try {
      if (currentTask != null) {
        // This task is complete. Inform the UI.
        List<WalletData> perWalletModelDataList = currentTask.getPerWalletModelDataToReplay();
        if (perWalletModelDataList != null) {
          for (WalletData perWalletModelData : perWalletModelDataList) {
            perWalletModelData.setBusyTaskVerbKey(null);
            perWalletModelData.setBusyTaskKey(null);
            perWalletModelData.setBusy(false);
            perWalletModelData.setReplayTaskUUID(null);
          }
        }
        // TODO - does not look quite right.
        controller.fireWalletBusyChange(false);
      }
    } finally {
      // No longer tidying up.
      replayManagerTimerTask.currentTaskIsTidyingUp(false);

      // Everything is completed - clear to start the next task.
      replayManagerTimerTask.currentTaskHasCompleted();
    }
  }

  public ReplayTask getCurrentReplayTask() {
    synchronized (replayTaskQueue) {
      if (replayTaskQueue.isEmpty()) {
        return null;
      } else {
        return replayTaskQueue.peek();
      }
    }
  }

  /**
   * See if there is a waiting replay task for a perWalletModelData
   *
   * @param perWalletModelData
   * @return the waiting ReplayTask or null if there is not one.
   */
  @SuppressWarnings("unchecked")
  public ReplayTask getWaitingReplayTask(WalletData perWalletModelData) {
    synchronized (replayTaskQueue) {
      if (replayTaskQueue.isEmpty()) {
        return null;
      } else {
        for (ReplayTask replayTask : (List<ReplayTask>) replayTaskQueue) {
          List<WalletData> list = replayTask.getPerWalletModelDataToReplay();
          if (list != null) {
            for (WalletData item : list) {
              if (perWalletModelData.getWalletFilename().equals(item.getWalletFilename())) {
                return replayTask;
              }
            }
          }

        }
        return null;
      }
    }
  }

  /**
   * Download the block chain.
   * This does not use a ReplayTask.
   */
  public void downloadBlockChain() {
    @SuppressWarnings("rawtypes")
    SwingWorker worker = new SwingWorker() {
      @Override
      protected Object doInBackground() throws Exception {
        if (controller.getWorldcoinWalletService().getPeerGroup() != null) {
          regularDownloadIsRunning = true;
          SendWorldcoinPanel.setEnableSendButton(false);
          log.debug("Downloading blockchain - regularDownloadIsRunning = " + regularDownloadIsRunning);

          controller.getWorldcoinWalletService().getPeerGroup().downloadBlockChain();
        } else {
          log.error("Cannot download blockchain as there is no PeerGroup");
        }
        return null; // return not used
      }
    };
    worker.execute();
  }

  /**
   * Method called back by WorldcoinPeerEventListener to indicate a block chain download has completed
   */
  public void downloadHasCompleted() {
    regularDownloadIsRunning = false;
    SendWorldcoinPanel.setEnableSendButton(true);
    log.debug("Download has completed (in ReplayManager) - regularDownloadIsRunning = " + regularDownloadIsRunning);
  }

  /**
   * The actual height of the block chain just prior to a replay being started.
   *
   * @return
   */
  public int getActualLastChainHeight() {
    return actualLastChainHeight;
  }
}