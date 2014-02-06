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
package org.wallet.viewsystem.swing;

import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.file.WalletSaveException;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.store.WalletVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * TimerTask to regularly check the 'health' of WorldcoinWallet.
 * It detects whether wallet files have been changed by some external process and to save dirty files.
 * It checks the ping times of the peers to see if they are up.
 *
 * @see java.util.Timer
 * @see java.util.TimerTask
 */
public class HealthCheckTimerTask extends TimerTask {

  public static final int INITIAL_DELAY = 30000; // milliseconds
  public static final int DEFAULT_REPEAT_RATE = 90000; // milliseconds

  private static Logger log = LoggerFactory.getLogger(HealthCheckTimerTask.class);

  private final Controller controller;
  private final WorldcoinController worldcoinController;

  private boolean isRunning = false;

  //private Map<String, Long> previousPeerToLastPingTime;

  //private int counter;

  /**
   * Constructs the object, sets the string to be output in function run()
   *
   * @param worldcoinController Controller giving access to Worldcoin object model
   */
  public HealthCheckTimerTask(WorldcoinController worldcoinController) {
    this.worldcoinController = worldcoinController;
    this.controller = worldcoinController;

    //previousPeerToLastPingTime = new HashMap<String, Long>();

    //counter = 0;
  }

  /**
   * When the timer executes, this code is run.
   */
  @Override
  public void run() {
    isRunning = true;
    try {
      log.debug("Start of HealthCheckTimerTask");

      //if (counter == 0) {
        log.debug("Checking if wallets are dirty . . .");
        List<WalletData> perWalletModelDataList = worldcoinController.getModel().getPerWalletModelDataList();

        if (perWalletModelDataList != null) {
          Iterator<WalletData> iterator = perWalletModelDataList.iterator();
          while (iterator.hasNext()) {
            WalletData loopModelData = iterator.next();
            if (worldcoinController.getFileHandler() != null) {
              // See if the files have been changed by another
              // process (non WorldcoinWallet).
              boolean haveFilesChanged = worldcoinController.getFileHandler().haveFilesChanged(loopModelData);
              if (haveFilesChanged) {
                boolean previousFilesHaveBeenChanged = loopModelData.isFilesHaveBeenChangedByAnotherProcess();
                loopModelData.setFilesHaveBeenChangedByAnotherProcess(true);
                if (!previousFilesHaveBeenChanged) {
                  // only fire once, when change happens
                  worldcoinController.fireFilesHaveBeenChangedByAnotherProcess(loopModelData);
                  log.debug("Marking wallet " + loopModelData.getWalletFilename()
                          + " as having been changed by another process.");
                }
              }

              // See if they are dirty - write out if so.
              if (loopModelData.isDirty()) {
                log.debug("Saving dirty wallet '" + loopModelData.getWalletFilename() + "'...");
                try {
                  worldcoinController.getFileHandler().savePerWalletModelData(loopModelData, false);
                  log.debug("... done.");
                } catch (WalletSaveException e) {
                  String message = controller.getLocaliser().getString(
                          "createNewWalletAction.walletCouldNotBeCreated",
                          new Object[]{loopModelData.getWalletFilename(), e.getMessage()});
                  log.error(message);
                  MessageManager.INSTANCE.addMessage(new Message(message));
                } catch (WalletVersionException e) {
                  String message = controller.getLocaliser().getString(
                          "createNewWalletAction.walletCouldNotBeCreated",
                          new Object[]{loopModelData.getWalletFilename(), e.getMessage()});
                  log.error(message);
                  MessageManager.INSTANCE.addMessage(new Message(message));
                }
              }
            }
          }
        }
      //}
      //log.debug("Checking if peers are alive . . .");
//      if (worldcoinController.getWorldcoinWalletService() != null && worldcoinController.getWorldcoinWalletService().getPeerGroup() != null) {
//        PeerGroup peerGroup = worldcoinController.getWorldcoinWalletService().getPeerGroup();
//        Peer downloadPeer = peerGroup.getDownloadPeer();
//        if (downloadPeer == null) {
//          //log.debug("There is no downloadPeer");
//        } else {
//          //log.debug("The downloadPeer is '" + downloadPeer.toString());
//        }
//
//        // Work out if all the last ping times are exactly the same as the last times the health check ran.
//        // The ping time says the same if the network is lost so if all the ping times are the same as the previous time
//        // the health check ran then this is a good indicator that the network has been lost.
//        List<Peer> connectedPeers = peerGroup.getConnectedPeers();
//        if (connectedPeers != null) {
//          boolean allPingTimesWereIdenticalToPreviousHealthCheck = true;
//          for (Peer connectedPeer : connectedPeers) {
//            long thePingTimeThisHealthCheck = connectedPeer.getLastPingTime();
//            Long thePreviousPingTime = previousPeerToLastPingTime.get(connectedPeer.toString());
//            if (thePingTimeThisHealthCheck != (thePreviousPingTime == null ? 0 : thePreviousPingTime.longValue())) {
//              allPingTimesWereIdenticalToPreviousHealthCheck = false;
//              break;
//            }
//          }
//          if (allPingTimesWereIdenticalToPreviousHealthCheck) {
//            log.debug("SUSPECTED LOSS OF NETWORK - all ping times were identical to the last time the health check was run.");
//          }
//          // Clear out previous ping times and then refill.
//          previousPeerToLastPingTime.clear();
//
//          for (Peer connectedPeer : connectedPeers) {
//            if (downloadPeer == null) {
//              //log.debug("There is a null connectedPeer");
//            } else {
//              //log.debug("There is a connectedPeer : '" + connectedPeer.toString() + "', with a last ping time of " + connectedPeer.getLastPingTime());
//              previousPeerToLastPingTime.put(connectedPeer.toString(), connectedPeer.getLastPingTime());
//            }
//          }
//        }
//      } else {
//        //log.debug("Cannot perform peer check due to missing peerGroup");
//      }

      log.debug("End of HealthCheckTimerTask");
    } catch (java.util.ConcurrentModificationException cme) {
      log.error("The list of open wallets was changed whilst files were being written.");
    } finally {
      //int SLOW_DOWN_RATE = 4;
      //counter = (counter + 1) % SLOW_DOWN_RATE;
      isRunning = false;
    }
  }

  public boolean isRunning() {
    return isRunning;
  }
}