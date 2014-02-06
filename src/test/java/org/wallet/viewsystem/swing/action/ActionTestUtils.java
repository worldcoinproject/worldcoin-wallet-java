package org.wallet.viewsystem.swing.action;

import java.io.File;
import java.security.SecureRandom;

import org.worldcoinj.wallet.Protos;
import org.worldcoinj.wallet.Protos.ScryptParameters;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.file.FileHandler;
import org.wallet.model.worldcoin.WalletAddressBookData;
import org.wallet.model.worldcoin.WalletData;
import org.wallet.model.worldcoin.WalletInfoData;
import org.wallet.store.WorldcoinWalletVersion;

import com.google.worldcoin.core.ECKey;
import com.google.worldcoin.core.NetworkParameters;
import com.google.worldcoin.core.Wallet;
import com.google.worldcoin.crypto.KeyCrypter;
import com.google.worldcoin.crypto.KeyCrypterScrypt;
import com.google.protobuf.ByteString;

/**
 * Class containing utility methods for action tests.
 * @author jim
 *
 */
public class ActionTestUtils {
    
     private static SecureRandom secureRandom;

     public static final String LABEL_OF_ADDRESS_ADDED = "This is an address label";

     public static void createNewActiveWallet(WorldcoinController controller, String descriptor, boolean encrypt, CharSequence walletPassword) throws Exception {
         if (secureRandom == null) {
             secureRandom = new SecureRandom();
         }
         
         byte[] salt = new byte[KeyCrypterScrypt.SALT_LENGTH];
         secureRandom.nextBytes(salt);
         Protos.ScryptParameters.Builder scryptParametersBuilder = Protos.ScryptParameters.newBuilder().setSalt(ByteString.copyFrom(salt));
         ScryptParameters scryptParameters = scryptParametersBuilder.build();
         KeyCrypter keyCrypter = new KeyCrypterScrypt(scryptParameters);

         Wallet wallet;
         ECKey ecKey;
         if (encrypt) {
             wallet = new Wallet(NetworkParameters.prodNet(), keyCrypter);
             ecKey = (new ECKey()).encrypt(keyCrypter, keyCrypter.deriveKey(walletPassword));
             wallet.addKey(ecKey);
         } else {
             wallet = new Wallet(NetworkParameters.prodNet());
             ecKey = new ECKey();
             wallet.addKey(ecKey);             
         }
         
         WalletData perWalletModelData = new WalletData();
         perWalletModelData.setWallet(wallet);
  
         // Save the wallet to a temporary directory.
         File worldcoinWalletDirectory = FileHandler.createTempDirectory("CreateAndDeleteWalletsTest");
         String worldcoinWalletDirectoryPath = worldcoinWalletDirectory.getAbsolutePath();
         String walletFile = worldcoinWalletDirectoryPath + File.separator + descriptor + ".wallet";
         
         // Put the wallet in the model as the active wallet.
         WalletInfoData walletInfoData = new WalletInfoData(walletFile, wallet, WorldcoinWalletVersion.PROTOBUF_ENCRYPTED);
         walletInfoData.addReceivingAddress(new WalletAddressBookData(LABEL_OF_ADDRESS_ADDED, ecKey.toAddress(NetworkParameters.prodNet()).toString()), false);

         perWalletModelData.setWalletInfo(walletInfoData);
         perWalletModelData.setWalletFilename(walletFile);
         perWalletModelData.setWalletDescription(descriptor);
         
         // Save the wallet and load it up again, making it the active wallet.
         // This also sets the timestamp fields used in file change detection.
         FileHandler fileHandler = new FileHandler(controller);
         fileHandler.savePerWalletModelData(perWalletModelData, true);
         WalletData loadedPerWalletModelData = fileHandler.loadFromFile(new File(walletFile));
         
         controller.getModel().setActiveWalletByFilename(loadedPerWalletModelData.getWalletFilename());         
     }
}
