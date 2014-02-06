/*
 * The MIT License
 *
 * Copyright 2013 Cameron Garnham.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.wallet;

import java.util.Locale;
import org.wallet.controller.worldcoin.WorldcoinController;
import org.wallet.controller.core.CoreController;
import org.wallet.controller.exchange.ExchangeController;
import org.wallet.exchange.CurrencyConverter;
import org.wallet.model.core.CoreModel;
import org.wallet.model.worldcoin.WorldcoinModel;
import org.wallet.model.exchange.ExchangeModel;

/**
 *
 * @author Cameron Garnham
 */
public class CreateControllers {

    public static Controllers createControllers() {
        return CreateControllers.createControllers(null, null, null, null);
    }

    public static Controllers createControllers(Localiser localiser) {
        return CreateControllers.createControllers(localiser, null, null, null);
    }
    
    public static Controllers createControllers(Localiser localiser, ApplicationDataDirectoryLocator applicationDataDirectoryLocator) {
        return CreateControllers.createControllers(localiser, applicationDataDirectoryLocator, null, null);
    }
    
    public static Controllers createControllers(ApplicationDataDirectoryLocator applicationDataDirectoryLocator) {
        return CreateControllers.createControllers(null, applicationDataDirectoryLocator, null, null);
    }

    public static Controllers createControllers(
            Localiser localiser,
            ApplicationDataDirectoryLocator applicationDataDirectoryLocator,
            String first,
            String second
            )
    {

        final CoreController coreController = new CoreController(applicationDataDirectoryLocator);
        final WorldcoinController worldcoinController = new WorldcoinController(coreController);
        final ExchangeController exchangeController = new ExchangeController(coreController);
        
        final CoreModel coreModel = new CoreModel();
        final WorldcoinModel worldcoinWalletModel = new WorldcoinModel(coreModel);
        final ExchangeModel exchangeModel = new ExchangeModel(coreModel);
        
        coreController.setLocaliser((null != localiser) ? localiser : new Localiser(Locale.ENGLISH));
        
        coreController.setModel(coreModel);
        worldcoinController.setModel(worldcoinWalletModel);
        exchangeController.setModel(exchangeModel);
        
        CurrencyConverter.INSTANCE.initialise(coreController);

        WorldcoinWallet.setCoreController(coreController);
        WorldcoinWallet.setWorldcoinController(worldcoinController);
        WorldcoinWallet.setExchangeController(exchangeController);
        
        return new Controllers(coreController, worldcoinController, exchangeController);
    }

    public static class Controllers {

        public final CoreController coreController;
        public final WorldcoinController worldcoinController;
        public final ExchangeController exchangeController;

        public Controllers(final CoreController coreController, final WorldcoinController worldcoinController, final ExchangeController exchangeController) {
            this.coreController = coreController;
            this.worldcoinController = worldcoinController;
            this.exchangeController = exchangeController;
        }
    }
}
