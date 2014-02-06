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

import com.google.worldcoin.core.Address;
import com.google.worldcoin.core.AddressFormatException;
import com.google.worldcoin.core.Transaction;
import com.google.worldcoin.core.Wallet.BalanceType;
import org.wallet.controller.Controller;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.exchange.CurrencyConverterResult;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * A class to validate String addresses and amounts.
 * TODO - this should create a validation state object and have a getter
 * 
 * @author jim
 * 
 */
public class Validator {
    private static final Logger log = LoggerFactory.getLogger(Validator.class);

    private final Controller controller;
    private final WorldcoinController worldcoinController;

    public Validator(WorldcoinController worldcoinController) {
        this.worldcoinController = worldcoinController;
        this.controller = this.worldcoinController;
    }

    /**
     * Validate a String address and amount.
     * 
     * @param address
     * @param amount
     * @return
     */
    public boolean validate(String address, String amount) {
        clearValidationState();

        boolean validAddress = validateAddress(address);
        boolean validAmount = validateAmount(amount);
        return validAddress && validAmount;
    }

    private boolean validateAmount(String amount) {
        // Copy amount to wallet preferences.
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_VALUE, amount);

        Boolean amountValidatesOk = Boolean.TRUE;

        Boolean amountIsInvalid = Boolean.FALSE;
        Boolean notEnoughFunds = Boolean.FALSE;
        Boolean amountIsMissing = Boolean.FALSE;
        Boolean amountIsNegativeOrZero = Boolean.FALSE;
        Boolean amountIsTooSmall = Boolean.FALSE;

        // See if the amount is missing.
        if (amount == null || "".equals(amount) || amount.trim().length() == 0) {
            amountIsMissing = Boolean.TRUE;
            amountValidatesOk = Boolean.FALSE;
        } else {
            // See if the amount is a number.
            BigInteger amountBigInteger = null;
            try {
                CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToWDCNotLocalised(amount);
                if (converterResult.isWdcMoneyValid()) {
                    // Parses ok.
                    amountBigInteger = converterResult.getWdcMoney().getAmount().toBigInteger();
                } else {
                    amountIsInvalid = Boolean.TRUE;
                    amountValidatesOk = Boolean.FALSE;
                }
            } catch (NumberFormatException nfe) {
                amountValidatesOk = Boolean.FALSE;
                amountIsInvalid = Boolean.TRUE;
            } catch (ArithmeticException ae) {
                amountValidatesOk = Boolean.FALSE;
                amountIsInvalid = Boolean.TRUE;
            }

            // See if the amount is negative or zero.
            if (amountValidatesOk.booleanValue()) {
                if (amountBigInteger.compareTo(BigInteger.ZERO) <= 0) {
                    amountValidatesOk = Boolean.FALSE;
                    amountIsNegativeOrZero = Boolean.TRUE;
                } else {
                  if (amountBigInteger.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) {
                    amountValidatesOk = Boolean.FALSE;
                    amountIsTooSmall = Boolean.TRUE;
                  } else {
                    // The fee is worked out in detail later, but we know it will be at least the minimum reference amount.
                    BigInteger totalSpend = amountBigInteger.add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
                    BigInteger availableBalance = this.worldcoinController.getModel().getActiveWallet().getBalance(BalanceType.AVAILABLE);
                    BigInteger estimatedBalance = this.worldcoinController.getModel().getActiveWallet().getBalance(BalanceType.ESTIMATED);

                    log.debug("Amount = " + amountBigInteger.toString() + ", fee of at least " + Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.toString()
                            + ", totalSpend = " + totalSpend.toString() + ", availableBalance = " + availableBalance.toString() + ", estimatedBalance = " + estimatedBalance.toString());
                    if (totalSpend.compareTo(availableBalance) > 0) {
                      // Not enough funds.
                      amountValidatesOk = Boolean.FALSE;
                      notEnoughFunds = Boolean.TRUE;
                    }
                  }
                }
            }
        }
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_IS_MISSING, amountIsMissing.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_IS_NEGATIVE_OR_ZERO,
                amountIsNegativeOrZero.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_IS_TOO_SMALL, amountIsTooSmall.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_IS_INVALID, amountIsInvalid.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_NOT_ENOUGH_FUNDS, notEnoughFunds.toString());

        return amountValidatesOk.booleanValue();
    }

    private boolean validateAddress(String address) {
        Boolean addressIsInvalid = Boolean.TRUE;

        if (address != null && !address.isEmpty()) {
            // Copy address to wallet preferences.
            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_ADDRESS_VALUE, address);

            try {
                new Address(this.worldcoinController.getModel().getNetworkParameters(), address);
                addressIsInvalid = Boolean.FALSE;
            } catch (AddressFormatException afe) {
                // Carry on.
            } catch (java.lang.StringIndexOutOfBoundsException e) {
                // Carry on.
            }
        } else {
            this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_ADDRESS_VALUE, "");
        }
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_ADDRESS_IS_INVALID, addressIsInvalid.toString());

        return !addressIsInvalid.booleanValue();
    }

    public void clearValidationState() {
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_IS_MISSING, Boolean.FALSE.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_IS_NEGATIVE_OR_ZERO, Boolean.FALSE.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_IS_TOO_SMALL, Boolean.FALSE.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_AMOUNT_IS_INVALID, Boolean.FALSE.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_NOT_ENOUGH_FUNDS, Boolean.FALSE.toString());
        this.worldcoinController.getModel().setActiveWalletPreference(WorldcoinModel.VALIDATION_ADDRESS_IS_INVALID, Boolean.FALSE.toString());
    }
}
