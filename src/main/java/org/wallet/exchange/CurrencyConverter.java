package org.wallet.exchange;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.wallet.controller.Controller;
import org.wallet.model.exchange.ExchangeData;
import org.wallet.model.exchange.ExchangeModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.joda.money.CurrencyUnit;
import org.joda.money.IllegalCurrencyException;
import org.joda.money.Money;
import org.joda.money.format.MoneyAmountStyle;
import org.joda.money.format.MoneyFormatter;
import org.joda.money.format.MoneyFormatterBuilder;



public enum CurrencyConverter {
    INSTANCE;
   
    private static final Logger log = LoggerFactory.getLogger(CurrencyConverter.class);

    private Controller controller;
    
    private Collection<CurrencyConverterListener> listeners;
    
    public static final BigInteger NUMBER_OF_SATOSHI_IN_ONE_WORLDCOIN = BigInteger.valueOf(100000000); // 8 zeros
    public static final int NUMBER_OF_DECIMAL_POINTS_IN_A_WORLDCOIN = 8;
    
    // Extra digits used in calculation.
    public static final int ADDITIONAL_CALCULATION_DIGITS = 16;
   
    // This is the Worldcoin currency unit, denominated in satoshi with 0 decimal places.
    public CurrencyUnit WORLDCOIN_CURRENCY_UNIT;
    
    /**
     * The currency unit for the currency being converted.
     */
    private CurrencyUnit currencyUnit;  

    /**
     * MoneyFormatter without currency code
     */
    MoneyFormatter moneyFormatter;
    
    /**
     * MoneyFormatter with currency code
     */
    MoneyFormatter moneyFormatterWithCurrencyCode;
    
    /**
     * The exchange rate i.e the value of 1 WDC in the currency.
     */
    private BigDecimal rate;
    
    /**
     * The rate rate in terms of satoshi i.e. value of 1 satoshi in the currency
     */
    private BigDecimal rateDividedByNumberOfSatoshiInOneWorldcoin;
      
    private String groupingSeparator;
    
    /**
     * Map of currency code to currency info.
     */
    private Map<String, CurrencyInfo> currencyCodeToInfoMap;
    
    /**
     * Map of currency code to currency description (from OpenExchangeRates)
     */
    private Map<String, String> currencyCodeToDescriptionMap;

    public void initialise(Controller controller) {
        // Initialise conversion currency.
        String currencyCode = controller.getModel().getUserPreference(ExchangeModel.TICKER_FIRST_ROW_CURRENCY);
        String exchange = controller.getModel().getUserPreference(ExchangeModel.TICKER_FIRST_ROW_EXCHANGE);
        String newCurrencyCode = currencyCode;
        if (ExchangeData.WORLDCOIN_CHARTS_EXCHANGE_NAME.equals(exchange)) {
            // Use only the last three characters - the currency code.
            if (currencyCode.length() >= 3) {
                newCurrencyCode = currencyCode.substring(currencyCode.length() - 3);
            }
        }
        initialise(controller, newCurrencyCode);
    }
    
    public void initialise(Controller controller, String currencyCode) {
       this.controller = controller;
       
       try {
           WORLDCOIN_CURRENCY_UNIT = CurrencyUnit.of("BTC");
           
           if (currencyCode != null && !"".equals(currencyCode)) {
               currencyUnit = CurrencyUnit.of(currencyCode);
           } else {
               currencyUnit = CurrencyUnit.of("USD");
           }
       } catch (IllegalCurrencyException ice) {
           ice.printStackTrace();
           // Default to USD.
           currencyUnit = CurrencyUnit.of("USD");
       }
       
       
        // Exchange rate is unknown.
        rate = null;
        rateDividedByNumberOfSatoshiInOneWorldcoin = null;
        
        // Setup listeners
        listeners = new ArrayList<CurrencyConverterListener>();
        
        // Initialise currency info map.
        currencyCodeToInfoMap = new HashMap<String, CurrencyInfo>();
        currencyCodeToInfoMap.put("USD", new CurrencyInfo("USD", "$", true));
        currencyCodeToInfoMap.put("AUD", new CurrencyInfo("AUD", "AU$", true));
        currencyCodeToInfoMap.put("CAD", new CurrencyInfo("CAD", "CA$", true));
        currencyCodeToInfoMap.put("NZD", new CurrencyInfo("NZD", "NZ$", true));
        currencyCodeToInfoMap.put("SGD", new CurrencyInfo("SGD", "SG$", true));
        currencyCodeToInfoMap.put("HKD", new CurrencyInfo("HKD", "HK$", true));

        currencyCodeToInfoMap.put("GBP", new CurrencyInfo("GBP", "\u00A3", true));
        currencyCodeToInfoMap.put("EUR", new CurrencyInfo("EUR", "\u20AC", true));
        currencyCodeToInfoMap.put("CHF", new CurrencyInfo("CHF", " CHF", false));
        currencyCodeToInfoMap.put("JPY", new CurrencyInfo("JPY", "\u00A5", true));
        currencyCodeToInfoMap.put("CNY", new CurrencyInfo("CNY", "\u5143", false));
        currencyCodeToInfoMap.put("RUB", new CurrencyInfo("RUB", "\u0440\u0443\u0431", false));
        currencyCodeToInfoMap.put("SEK", new CurrencyInfo("SEK", "\u006B\u0072", true));
        currencyCodeToInfoMap.put("DKK", new CurrencyInfo("DKK", "\u006B\u0072.", true));
        currencyCodeToInfoMap.put("THB", new CurrencyInfo("THB", "\u0E3F", true));
        currencyCodeToInfoMap.put("PLN", new CurrencyInfo("PLN", "\u007A\u0142", false));
        updateFormatters();
        
        // Initialise currency description map.
        currencyCodeToDescriptionMap = new HashMap<String, String>();
        currencyCodeToDescriptionMap.put("AED", "United Arab Emirates Dirham");
        currencyCodeToDescriptionMap.put("AFN", "Afghan Afghani");
        currencyCodeToDescriptionMap.put("ALL", "Albanian Lek");
        currencyCodeToDescriptionMap.put("AMD", "Armenian Dram");
        currencyCodeToDescriptionMap.put("ANG", "Netherlands Antillean Guilder");
        currencyCodeToDescriptionMap.put("AOA", "Angolan Kwanza");
        currencyCodeToDescriptionMap.put("ARS", "Argentine Peso");
        currencyCodeToDescriptionMap.put("AUD", "Australian Dollar");
        currencyCodeToDescriptionMap.put("AWG", "Aruban Florin");
        currencyCodeToDescriptionMap.put("AZN", "Azerbaijani Manat");
        currencyCodeToDescriptionMap.put("BAM", "Bosnia-Herzegovina Convertible Mark");
        currencyCodeToDescriptionMap.put("BBD", "Barbadian Dollar");
        currencyCodeToDescriptionMap.put("BDT", "Bangladeshi Taka");
        currencyCodeToDescriptionMap.put("BGN", "Bulgarian Lev");
        currencyCodeToDescriptionMap.put("BHD", "Bahraini Dinar");
        currencyCodeToDescriptionMap.put("BIF", "Burundian Franc");
        currencyCodeToDescriptionMap.put("BMD", "Bermudan Dollar");
        currencyCodeToDescriptionMap.put("BND", "Brunei Dollar");
        currencyCodeToDescriptionMap.put("BOB", "Bolivian Boliviano");
        currencyCodeToDescriptionMap.put("BRL", "Brazilian Real");
        currencyCodeToDescriptionMap.put("BSD", "Bahamian Dollar");
        currencyCodeToDescriptionMap.put("WDC", "Worldcoin");
        currencyCodeToDescriptionMap.put("BTN", "Bhutanese Ngultrum");
        currencyCodeToDescriptionMap.put("BWP", "Botswanan Pula");
        currencyCodeToDescriptionMap.put("BYR", "Belarusian Ruble");
        currencyCodeToDescriptionMap.put("BZD", "Belize Dollar");
        currencyCodeToDescriptionMap.put("CAD", "Canadian Dollar");
        currencyCodeToDescriptionMap.put("CDF", "Congolese Franc");
        currencyCodeToDescriptionMap.put("CHF", "Swiss Franc");
        currencyCodeToDescriptionMap.put("CLP", "Chilean Peso");
        currencyCodeToDescriptionMap.put("CNY", "Chinese Yuan");
        currencyCodeToDescriptionMap.put("COP", "Colombian Peso");
        currencyCodeToDescriptionMap.put("CRC", "Costa Rican Col\u00F3n");
        currencyCodeToDescriptionMap.put("CUP", "Cuban Peso");
        currencyCodeToDescriptionMap.put("CVE", "Cape Verdean Escudo");
        currencyCodeToDescriptionMap.put("CZK", "Czech Republic Koruna");
        currencyCodeToDescriptionMap.put("DJF", "Djiboutian Franc");
        currencyCodeToDescriptionMap.put("DKK", "Danish Krone");
        currencyCodeToDescriptionMap.put("DOP", "Dominican Peso");
        currencyCodeToDescriptionMap.put("DZD", "Algerian Dinar");
        currencyCodeToDescriptionMap.put("EGP", "Egyptian Pound");
        currencyCodeToDescriptionMap.put("ETB", "Ethiopian Birr");
        currencyCodeToDescriptionMap.put("EUR", "Euro");
        currencyCodeToDescriptionMap.put("FJD", "Fijian Dollar");
        currencyCodeToDescriptionMap.put("FKP", "Falkland Islands Pound");
        currencyCodeToDescriptionMap.put("GBP", "British Pound Sterling");
        currencyCodeToDescriptionMap.put("GEL", "Georgian Lari");
        currencyCodeToDescriptionMap.put("GHS", "Ghanaian Cedi");
        currencyCodeToDescriptionMap.put("GIP", "Gibraltar Pound");
        currencyCodeToDescriptionMap.put("GMD", "Gambian Dalasi");
        currencyCodeToDescriptionMap.put("GNF", "Guinean Franc");
        currencyCodeToDescriptionMap.put("GTQ", "Guatemalan Quetzal");
        currencyCodeToDescriptionMap.put("GYD", "Guyanaese Dollar");
        currencyCodeToDescriptionMap.put("HKD", "Hong Kong Dollar");
        currencyCodeToDescriptionMap.put("HNL", "Honduran Lempira");
        currencyCodeToDescriptionMap.put("HRK", "Croatian Kuna");
        currencyCodeToDescriptionMap.put("HTG", "Haitian Gourde");
        currencyCodeToDescriptionMap.put("HUF", "Hungarian Forint");
        currencyCodeToDescriptionMap.put("IDR", "Indonesian Rupiah");
        currencyCodeToDescriptionMap.put("ILS", "Israeli New Sheqel");
        currencyCodeToDescriptionMap.put("INR", "Indian Rupee");
        currencyCodeToDescriptionMap.put("IQD", "Iraqi Dinar");
        currencyCodeToDescriptionMap.put("IRR", "Iranian Rial");
        currencyCodeToDescriptionMap.put("ISK", "Icelandic Kr\u00F3na");
        currencyCodeToDescriptionMap.put("JMD", "Jamaican Dollar");
        currencyCodeToDescriptionMap.put("JOD", "Jordanian Dinar");
        currencyCodeToDescriptionMap.put("JPY", "Japanese Yen");
        currencyCodeToDescriptionMap.put("KES", "Kenyan Shilling");
        currencyCodeToDescriptionMap.put("KGS", "Kyrgystani Som");
        currencyCodeToDescriptionMap.put("KHR", "Cambodian Riel");
        currencyCodeToDescriptionMap.put("KMF", "Comorian Franc");
        currencyCodeToDescriptionMap.put("KPW", "North Korean Won");
        currencyCodeToDescriptionMap.put("KRW", "South Korean Won");
        currencyCodeToDescriptionMap.put("KWD", "Kuwaiti Dinar");
        currencyCodeToDescriptionMap.put("KYD", "Cayman Islands Dollar");
        currencyCodeToDescriptionMap.put("KZT", "Kazakhstani Tenge");
        currencyCodeToDescriptionMap.put("LAK", "Laotian Kip");
        currencyCodeToDescriptionMap.put("LBP", "Lebanese Pound");
        currencyCodeToDescriptionMap.put("LKR", "Sri Lankan Rupee");
        currencyCodeToDescriptionMap.put("LRD", "Liberian Dollar");
        currencyCodeToDescriptionMap.put("LSL", "Lesotho Loti");
        currencyCodeToDescriptionMap.put("LTL", "Lithuanian Litas");
        currencyCodeToDescriptionMap.put("LVL", "Latvian Lats");
        currencyCodeToDescriptionMap.put("LYD", "Libyan Dinar");
        currencyCodeToDescriptionMap.put("MAD", "Moroccan Dirham");
        currencyCodeToDescriptionMap.put("MDL", "Moldovan Leu");
        currencyCodeToDescriptionMap.put("MGA", "Malagasy Ariary");
        currencyCodeToDescriptionMap.put("MKD", "Macedonian Denar");
        currencyCodeToDescriptionMap.put("MMK", "Myanma Kyat");
        currencyCodeToDescriptionMap.put("MNT", "Mongolian Tugrik");
        currencyCodeToDescriptionMap.put("MOP", "Macanese Pataca");
        currencyCodeToDescriptionMap.put("MRO", "Mauritanian Ouguiya");
        currencyCodeToDescriptionMap.put("MUR", "Mauritian Rupee");
        currencyCodeToDescriptionMap.put("MVR", "Maldivian Rufiyaa");
        currencyCodeToDescriptionMap.put("MWK", "Malawian Kwacha");
        currencyCodeToDescriptionMap.put("MXN", "Mexican Peso");
        currencyCodeToDescriptionMap.put("MYR", "Malaysian Ringgit");
        currencyCodeToDescriptionMap.put("MZN", "Mozambican Metical");
        currencyCodeToDescriptionMap.put("NAD", "Namibian Dollar");
        currencyCodeToDescriptionMap.put("NGN", "Nigerian Naira");
        currencyCodeToDescriptionMap.put("NIO", "Nicaraguan C\u00F3rdoba");
        currencyCodeToDescriptionMap.put("NOK", "Norwegian Krone");
        currencyCodeToDescriptionMap.put("NPR", "Nepalese Rupee");
        currencyCodeToDescriptionMap.put("NZD", "New Zealand Dollar");
        currencyCodeToDescriptionMap.put("OMR", "Omani Rial");
        currencyCodeToDescriptionMap.put("PAB", "Panamanian Balboa");
        currencyCodeToDescriptionMap.put("PEN", "Peruvian Nuevo Sol");
        currencyCodeToDescriptionMap.put("PGK", "Papua New Guinean Kina");
        currencyCodeToDescriptionMap.put("PHP", "Philippine Peso");
        currencyCodeToDescriptionMap.put("PKR", "Pakistani Rupee");
        currencyCodeToDescriptionMap.put("PLN", "Polish Zloty");
        currencyCodeToDescriptionMap.put("PYG", "Paraguayan Guarani");
        currencyCodeToDescriptionMap.put("QAR", "Qatari Rial");
        currencyCodeToDescriptionMap.put("RON", "Romanian Leu");
        currencyCodeToDescriptionMap.put("RSD", "Serbian Dinar");
        currencyCodeToDescriptionMap.put("RUB", "Russian Ruble");
        currencyCodeToDescriptionMap.put("RWF", "Rwandan Franc");
        currencyCodeToDescriptionMap.put("SAR", "Saudi Riyal");
        currencyCodeToDescriptionMap.put("SBD", "Solomon Islands Dollar");
        currencyCodeToDescriptionMap.put("SCR", "Seychellois Rupee");
        currencyCodeToDescriptionMap.put("SDG", "Sudanese Pound");
        currencyCodeToDescriptionMap.put("SEK", "Swedish Krona");
        currencyCodeToDescriptionMap.put("SGD", "Singapore Dollar");
        currencyCodeToDescriptionMap.put("SHP", "Saint Helena Pound");
        currencyCodeToDescriptionMap.put("SLL", "Sierra Leonean Leone");
        currencyCodeToDescriptionMap.put("SOS", "Somali Shilling");
        currencyCodeToDescriptionMap.put("SRD", "Surinamese Dollar");
        currencyCodeToDescriptionMap.put("STD", "S\u0101o Tom\u00E9 and Principe Dobra");
        currencyCodeToDescriptionMap.put("SYP", "Syrian Pound");
        currencyCodeToDescriptionMap.put("SZL", "Swazi Lilangeni");
        currencyCodeToDescriptionMap.put("THB", "Thai Baht");
        currencyCodeToDescriptionMap.put("TJS", "Tajikistani Somoni");
        currencyCodeToDescriptionMap.put("TMT", "Turkmenistani Manat");
        currencyCodeToDescriptionMap.put("TND", "Tunisian Dinar");
        currencyCodeToDescriptionMap.put("TOP", "Tongan Pa'anga");
        currencyCodeToDescriptionMap.put("TRY", "Turkish Lira");
        currencyCodeToDescriptionMap.put("TTD", "Trinidad and Tobago Dollar");
        currencyCodeToDescriptionMap.put("TWD", "New Taiwan Dollar");
        currencyCodeToDescriptionMap.put("TZS", "Tanzanian Shilling");
        currencyCodeToDescriptionMap.put("UAH", "Ukrainian Hryvnia");
        currencyCodeToDescriptionMap.put("UGX", "Ugandan Shilling");
        currencyCodeToDescriptionMap.put("USD", "United States Dollar");
        currencyCodeToDescriptionMap.put("UYU", "Uruguayan Peso");
        currencyCodeToDescriptionMap.put("UZS", "Uzbekistan Som");
        currencyCodeToDescriptionMap.put("VEF", "Venezuelan Bol\u00EDvar");
        currencyCodeToDescriptionMap.put("VND", "Vietnamese Dong");
        currencyCodeToDescriptionMap.put("VUV", "Vanuatu Vatu");
        currencyCodeToDescriptionMap.put("WST", "Samoan Tala");
        currencyCodeToDescriptionMap.put("XAF", "CFA Franc BEAC");
        currencyCodeToDescriptionMap.put("XCD", "East Caribbean Dollar");
        currencyCodeToDescriptionMap.put("XDR", "Special Drawing Rights");
        currencyCodeToDescriptionMap.put("XOF", "CFA Franc BCEAO");
        currencyCodeToDescriptionMap.put("XPF", "CFP Franc");
        currencyCodeToDescriptionMap.put("YER", "Yemeni Rial");
        currencyCodeToDescriptionMap.put("ZAR", "South African Rand");
        currencyCodeToDescriptionMap.put("ZMK", "Zambian Kwacha");
        currencyCodeToDescriptionMap.put("ZWL", "Zimbabwean Dollar");
    }
    
    public void updateFormatters() {
        moneyFormatter = getMoneyFormatter(false);
        moneyFormatterWithCurrencyCode = getMoneyFormatter(true);
        
        DecimalFormat fiatFormatter = (DecimalFormat) DecimalFormat.getInstance(controller.getLocaliser().getLocale());
        groupingSeparator = String.valueOf(fiatFormatter.getDecimalFormatSymbols().getGroupingSeparator());     
    }

    /**
     * Convert a number of satoshis to fiat
     * @param worldcoinAmount in satoshis
     * @return equivalent fiat amount
     */
    public Money convertFromWDCToFiat(BigInteger worldcoinAmountInSatoshi) {
        if (rate == null) {
            return null;
        } else {
            Money worldcoin = Money.of(WORLDCOIN_CURRENCY_UNIT, new BigDecimal(worldcoinAmountInSatoshi));
            
            Money fiatAmount = null;
            if (rateDividedByNumberOfSatoshiInOneWorldcoin != null) {
                fiatAmount = worldcoin.convertedTo(currencyUnit, rateDividedByNumberOfSatoshiInOneWorldcoin, RoundingMode.HALF_EVEN);
            }
            
            return fiatAmount;
        }
    }
    
    public CurrencyConverterResult convertFromFiatToWDC(String fiat) {
        if (rate == null || rate.equals(BigDecimal.ZERO)) {
            return new CurrencyConverterResult();
        } else {  
            
            if (fiat == null || fiat.trim().equals("")) {
                return new CurrencyConverterResult();   
            }
            
            Money wdcAmount = null;
            
            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(controller.getLocaliser().getLocale());
            formatter.setParseBigDecimal(true);
            
            // Convert spaces to non breakable space.
            fiat = fiat.replaceAll(" ", "\u00A0");

            try {
                BigDecimal parsedFiat = (BigDecimal)formatter.parse(fiat);
                Money fiatMoney = Money.of(currencyUnit, parsedFiat);
                wdcAmount = fiatMoney.convertedTo(WORLDCOIN_CURRENCY_UNIT, new BigDecimal(NUMBER_OF_SATOSHI_IN_ONE_WORLDCOIN).divide(rate, WORLDCOIN_CURRENCY_UNIT.getDecimalPlaces() + ADDITIONAL_CALCULATION_DIGITS, RoundingMode.HALF_EVEN), RoundingMode.HALF_EVEN);
                
                CurrencyConverterResult result = new CurrencyConverterResult();
                result.setWdcMoneyValid(true);
                result.setWdcMoney(wdcAmount);
                result.setFiatMoneyValid(true);
                result.setFiatMoney(fiatMoney);
                return result;    
            } catch (ParseException pe) {
                log.debug("convertFromFiatToWDC: " + pe.getClass().getName() + " "  + pe.getMessage());
                CurrencyConverterResult result = new CurrencyConverterResult();
                result.setWdcMoneyValid(false);
                result.setFiatMoneyValid(false);
                result.setFiatMessage(controller.getLocaliser().getString("currencyConverter.couldNotUnderstandAmount",
                        new Object[]{fiat}));
                return result;
            } catch (ArithmeticException ae) {
                log.debug("convertFromFiatToWDC: " + ae.getClass().getName() + " "  + ae.getMessage());
                String currencyString = currencyUnit.getCurrencyCode();
                if (currencyCodeToInfoMap.get(currencyString) != null) {
                    currencyString = currencyCodeToInfoMap.get(currencyString).getCurrencySymbol();
                }
                CurrencyConverterResult result = new CurrencyConverterResult();
                result.setWdcMoneyValid(false);
                result.setFiatMoneyValid(false);
                result.setFiatMessage(controller.getLocaliser().getString("currencyConverter.fiatCanOnlyHaveSetDecimalPlaces",
                        new Object[]{currencyString, currencyUnit.getDecimalPlaces()}));
                return result;
            }
        }
    }
    
    private MoneyFormatter getMoneyFormatter(boolean addCurrencySymbol) {
        MoneyFormatter moneyFormatter;
        
        // Suffix currency codes.
        String currencyCode = currencyUnit.getCurrencyCode();
        CurrencyInfo currencyInfo = currencyCodeToInfoMap.get(currencyCode);
        if (currencyInfo == null) {
            // Create a default currency info with the raw currency code as a suffix, including a separator space
            currencyInfo = new CurrencyInfo(currencyCode, currencyCode, false);
            currencyInfo.setHasSeparatingSpace(true);
        }

        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(controller.getLocaliser().getLocale());
        char decimalSeparator = formatter.getDecimalFormatSymbols().getDecimalSeparator();
        char groupingSeparator = formatter.getDecimalFormatSymbols().getGroupingSeparator();
        MoneyAmountStyle moneyAmountStyle;
        if ('.' == decimalSeparator) {
            if (',' == groupingSeparator) {
                moneyAmountStyle = MoneyAmountStyle.ASCII_DECIMAL_POINT_GROUP3_COMMA;
            } else if (' ' == groupingSeparator || '\u00A0' == groupingSeparator) {
                moneyAmountStyle = MoneyAmountStyle.ASCII_DECIMAL_POINT_GROUP3_SPACE;
            } else {
                moneyAmountStyle = MoneyAmountStyle.ASCII_DECIMAL_POINT_NO_GROUPING;
            }
        } else {
            if (',' == decimalSeparator) {
                if ('.' == groupingSeparator) {
                    moneyAmountStyle = MoneyAmountStyle.ASCII_DECIMAL_COMMA_GROUP3_DOT;
                } else if (' ' == groupingSeparator || '\u00A0' == groupingSeparator) {
                    moneyAmountStyle = MoneyAmountStyle.ASCII_DECIMAL_COMMA_GROUP3_SPACE;
                } else {
                    moneyAmountStyle = MoneyAmountStyle.ASCII_DECIMAL_COMMA_NO_GROUPING;
                }
            } else {
                // Do not really know - keep it simple.
                moneyAmountStyle = MoneyAmountStyle.ASCII_DECIMAL_POINT_NO_GROUPING;                
            }
        }
        
        String separator;
        if (currencyInfo.hasSeparatingSpace) {
            separator = " ";
        } else {
            separator = "";
        }
        if (currencyInfo.isPrefix()) {
            // Prefix currency code.
            if (addCurrencySymbol) {
                moneyFormatter = new MoneyFormatterBuilder().appendLiteral(currencyInfo.getCurrencySymbol()).appendLiteral(separator).appendAmount(moneyAmountStyle).toFormatter(controller.getLocaliser().getLocale());
            } else {
                moneyFormatter = new MoneyFormatterBuilder().appendAmount(moneyAmountStyle).toFormatter(controller.getLocaliser().getLocale());
            }
        } else {
             // Postfix currency code.
            if (addCurrencySymbol) {
                moneyFormatter = new MoneyFormatterBuilder().appendAmount(moneyAmountStyle).appendLiteral(separator).appendLiteral(currencyInfo.getCurrencySymbol()).toFormatter(controller.getLocaliser().getLocale());
            } else {
                moneyFormatter = new MoneyFormatterBuilder().appendAmount(moneyAmountStyle).toFormatter(controller.getLocaliser().getLocale());
            }
        }
        return moneyFormatter;
    }
    
    public String getFiatAsLocalisedString(Money money) {
        return getFiatAsLocalisedString(money, true, false);
    }
    
    public String getFiatAsLocalisedString(Money money, boolean addCurrencySymbol, boolean addParenthesis) {
        if (money == null) {
            return "";
        }
        
        MoneyFormatter moneyFormatterToUse;
        if (addCurrencySymbol) {
            if (moneyFormatterWithCurrencyCode == null) {
                moneyFormatterWithCurrencyCode = getMoneyFormatter(true);
            }
            moneyFormatterToUse = moneyFormatterWithCurrencyCode;
        } else {
            if (moneyFormatter == null) {
                moneyFormatter = getMoneyFormatter(false);
            }
            moneyFormatterToUse = moneyFormatter;
            
        }
   
        if (groupingSeparator == null) {
            DecimalFormat fiatFormatter = (DecimalFormat) DecimalFormat.getInstance(controller.getLocaliser().getLocale());
            groupingSeparator = String.valueOf(fiatFormatter.getDecimalFormatSymbols().getGroupingSeparator());
        }

        String toReturn =  moneyFormatterToUse.print(money);
        
        // Get rid of negative sign followed by thousand separator
        if (".".equals(groupingSeparator)) {
             // Escape regex.
            groupingSeparator = "\\.";
        }
        toReturn = toReturn.replaceAll("-" + groupingSeparator, "-");
        
        if (addParenthesis) {
            toReturn = "  (" + toReturn + ")";
        }
        return toReturn;
    }
    
    public String getWDCAsLocalisedString(Money wdcMoney) {
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(controller.getLocaliser().getLocale());
        formatter.setMaximumFractionDigits(NUMBER_OF_DECIMAL_POINTS_IN_A_WORLDCOIN);
        String wdcString = formatter.format(wdcMoney.getAmount().divide(new BigDecimal(NUMBER_OF_SATOSHI_IN_ONE_WORLDCOIN)));
        return wdcString;
    }
    
    public CurrencyConverterResult parseToFiat(String fiat) {
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(controller.getLocaliser().getLocale());
        formatter.setParseBigDecimal(true);

        // Convert spaces to non breakable space.
        fiat = fiat.replaceAll(" ", "\u00A0");

        try {
            BigDecimal parsedFiat = (BigDecimal) formatter.parse(fiat);
            Money fiatMoney = Money.of(currencyUnit, parsedFiat);
            CurrencyConverterResult result = new CurrencyConverterResult();
            result.setFiatMoneyValid(true);
            result.setFiatMoney(fiatMoney);
            return result;    
        } catch (ParseException pe) {
            log.debug("convertToMoney: " + pe.getClass().getName() + " " + pe.getMessage());
            CurrencyConverterResult result = new CurrencyConverterResult();
            result.setFiatMoneyValid(false);
            result.setFiatMessage(controller.getLocaliser().getString("currencyConverter.couldNotUnderstandAmount",
                    new Object[]{fiat}));
            return result;    
        } catch (ArithmeticException ae) {
            log.debug("convertToMoney: " + ae.getClass().getName() + " " + ae.getMessage());
            String currencyString = currencyUnit.getCurrencyCode();
            if (currencyCodeToInfoMap.get(currencyString) != null) {
                currencyString = currencyCodeToInfoMap.get(currencyString).getCurrencySymbol();
            }
            CurrencyConverterResult result = new CurrencyConverterResult();
            result.setFiatMoneyValid(false);
            result.setFiatMessage(controller.getLocaliser().getString("currencyConverter.fiatCanOnlyHaveSetDecimalPlaces",
                    new Object[]{currencyString, currencyUnit.getDecimalPlaces()}));
            return result;    
        }
    }
    
    /**
     * Parse a localised string and returns a Money denominated in Satoshi
     * @param wdcString
     * @return
     */
    public CurrencyConverterResult parseToWDC(String wdcString) {
        return parseToWDC(wdcString, controller.getLocaliser().getLocale());
    }
    
    /**
     * Parse a non localised string and returns a Money denominated in Satoshi
     * @param wdcString
     * @return
     */
    public CurrencyConverterResult parseToWDCNotLocalised(String wdcString) {
        return parseToWDC(wdcString, Locale.ENGLISH);
    }

    private CurrencyConverterResult parseToWDC(String wdcString, Locale locale) {
        if (wdcString == null || wdcString.equals("")) {
            return new CurrencyConverterResult();
        }
        
        // Convert spaces to non breakable space.
        wdcString = wdcString.replaceAll(" ", "\u00A0");
        
        Money wdcAmount = null;
        
        DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(locale);
        formatter.setParseBigDecimal(true);
        try {
            BigDecimal parsedWDC = ((BigDecimal)formatter.parse(wdcString)).movePointRight(NUMBER_OF_DECIMAL_POINTS_IN_A_WORLDCOIN);
            //log.debug("For locale " + controller.getLocaliser().getLocale().toString() +  ", '" + wdcString + "' parses to " + parsedWDC.toPlainString());
            wdcAmount = Money.of(WORLDCOIN_CURRENCY_UNIT, parsedWDC);
            CurrencyConverterResult result = new CurrencyConverterResult();
            result.setWdcMoneyValid(true);
            result.setWdcMoney(wdcAmount);
            return result; 
        } catch (ParseException pe) {
            log.debug("parseToWDC: " + pe.getClass().getName() + " " + pe.getMessage());
            CurrencyConverterResult result = new CurrencyConverterResult();
            result.setWdcMoneyValid(false);
            result.setWdcMessage(controller.getLocaliser().getString("currencyConverter.couldNotUnderstandAmount",
                    new Object[]{wdcString}));
            return result;
        } catch (ArithmeticException ae) {
            log.debug("parseToWDC: " + ae.getClass().getName() + " " + ae.getMessage());
            CurrencyConverterResult result = new CurrencyConverterResult();
            result.setWdcMoneyValid(false);
            result.setWdcMessage(controller.getLocaliser().getString("currencyConverter.wdcCanOnlyHaveEightDecimalPlaces"));
            return result;
        }
    }
    
    /**
     * Convert an unlocalised WDC amount e.g. 0.1234 to a localised WDC value with fiat
     * e.g. 0,1234 ($10,23)
     * @param wdcAsString
     * @return pretty string with format <wdc localised> (<fiat localised>)
     */
    public String prettyPrint(String wdcAsString) {
        String prettyPrint = "";
        CurrencyConverterResult converterResult = parseToWDCNotLocalised(wdcAsString);

        if (converterResult.isWdcMoneyValid()) {
            prettyPrint = getWDCAsLocalisedString(converterResult.getWdcMoney());
        } else {
            // WDC did not parse - just use the original text
            prettyPrint = wdcAsString;
        }
        prettyPrint = prettyPrint + " " + controller.getLocaliser().getString("sendWorldcoinPanel.amountUnitLabel");
        if (wdcAsString != null && !"".equals(wdcAsString)) {
            if (getRate() != null && isShowingFiat()) {
                if (converterResult.isWdcMoneyValid()) {
                    Money fiat = convertFromWDCToFiat(converterResult.getWdcMoney().getAmount()
                            .toBigInteger());
                    prettyPrint = prettyPrint + getFiatAsLocalisedString(fiat, true, true);
                }
            }
        }
        return prettyPrint;
    }

    public boolean isShowingFiat() {
        return !Boolean.FALSE.toString().equals(controller.getModel().getUserPreference(ExchangeModel.SHOW_WORLDCOIN_CONVERTED_TO_FIAT));
    }
    
    public CurrencyUnit getCurrencyUnit() {
        return currencyUnit;
    }

    public void setCurrencyUnit(CurrencyUnit currencyUnit) {
        // If this is a new currency, blank the rate.
        // Thus you should set the currency unit first.
        if (this.currencyUnit != null && !this.currencyUnit.equals(currencyUnit)) {
            rate = null;
            rateDividedByNumberOfSatoshiInOneWorldcoin = null;
        }
        this.currencyUnit = currencyUnit;
        
        // Reinitialise currency formatters.
        moneyFormatter = getMoneyFormatter(false);
        moneyFormatterWithCurrencyCode = getMoneyFormatter(true);
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        boolean fireFoundInsteadOfUpdated = (rate== null);
        this.rate = rate;
        rateDividedByNumberOfSatoshiInOneWorldcoin = rate.divide(new BigDecimal(CurrencyConverter.NUMBER_OF_SATOSHI_IN_ONE_WORLDCOIN));
        
        if (fireFoundInsteadOfUpdated) {
            notifyFoundExchangeRate();
        } else {
            notifyUpdatedExchangeRate();
        }
    }
    
    public void addCurrencyConverterListener(CurrencyConverterListener listener) {
        if (listeners == null) {
            throw new IllegalStateException("You need to initialise the CurrencyConverter first");
        }
        listeners.add(listener);
    }
    
    public void removeCurrencyConverterListener(CurrencyConverterListener listener) {
        if (listeners == null) {
            throw new IllegalStateException("You need to initialise the CurrencyConverter first");
        }
        listeners.remove(listener);
    }
    
    private void notifyFoundExchangeRate() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (listeners != null) {
                    for (CurrencyConverterListener listener : listeners) {
                        listener.foundExchangeRate(new ExchangeRate(currencyUnit, rate, new Date()));
                    }
                }
            }
        });
    }
    
    private void notifyUpdatedExchangeRate() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (listeners != null) {
                    for (CurrencyConverterListener listener : listeners) {
                        listener.updatedExchangeRate(new ExchangeRate(currencyUnit, rate, new Date()));
                    }
                }
            }
        });
    }

    public Map<String, CurrencyInfo> getCurrencyCodeToInfoMap() {
        return currencyCodeToInfoMap;
    }
    
    public Map<String, String> getCurrencyCodeToDescriptionMap() {
        return currencyCodeToDescriptionMap;
    }
}
