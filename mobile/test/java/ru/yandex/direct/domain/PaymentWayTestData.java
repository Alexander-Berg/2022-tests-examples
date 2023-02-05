package ru.yandex.direct.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;

import ru.yandex.direct.domain.account.management.SharedAccount;
import ru.yandex.direct.domain.clients.ClientInfo;
import ru.yandex.direct.domain.enums.PaymentWay;

public class PaymentWayTestData {

    public static Object[][] getSharedAccountTestData() {
        return get(SHARED_ACCOUNT_TEST_DATA);
    }

    static Object[][] getShortCampaignInfoTestData() {
        return get(SHORT_CAMPAIGN_INFO_TEST_DATA);
    }

    private static Object[][] get(String[][] rawLogsData) {
        Gson gson = new GsonBuilder().create();

        Object[][] data = new Object[rawLogsData.length][];

        for (int i = 0; i < data.length; ++i) {
            data[i] = deserializeRawDataArray(gson, rawLogsData[i]);
        }

        return data;
    }

    private static Object[] deserializeRawDataArray(Gson gson, String[] rawData) {
        Object[] deserialized = new Object[rawData.length + 1];

        for (int i = 0; i < rawData.length; ++i) {
            deserialized[i] = gson.fromJson(rawData[i], RAW_DATA_TYPES[i]);
        }

        deserialized[deserialized.length - 1] = getTestName(deserialized);

        return deserialized;
    }

    private static String getTestName(Object[] parameters) {
        ClientInfo clientInfo = (ClientInfo) parameters[4];
        PaymentWay[] paymentWays = (PaymentWay[]) parameters[6];

        return clientInfo.login + " - " + (parameters[0] == null ? "Campaign" : "Shared account") + " - "
                + Arrays.toString(paymentWays);
    }

    private static final Class[] RAW_DATA_TYPES = new Class[] {
            SharedAccount.class,
            ShortCampaignInfo.class,
            Float[].class,
            ShortCampaignInfo[].class,
            ClientInfo.class,
            Configuration.class,
            PaymentWay[].class,
    };

    private static final String[][] SHARED_ACCOUNT_TEST_DATA = new String[][]{
            {
                    "{\"AccountID\":8378733,\"AgencyName\":\"Крутое агентство\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"testestest@ya.ru\",\"MoneyWarningValue\":20},\"Login\":\"subuser-den\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"DateCreate\":\"2014-01-24\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Анджела Жеребцова\",\"id\":0,\"Login\":\"subuser-den\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"8-123-456-78-90\",\"Role\":\"Client\",\"SendAccNews\":\"No\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":0,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":11864682,\"AgencyName\":\"Крутое агентство\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"testestest@ya.ru\",\"MoneyWarningValue\":1},\"Login\":\"subuser-dimon\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Наталя Арестов\",\"id\":0,\"Login\":\"subuser-dimon\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"11111111111\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":0,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":8379106,\"AgencyName\":\"Крутое агентство\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"testestest@ya.ru\",\"MoneyWarningValue\":20},\"Login\":\"subuser-petr\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Танюшка Бухарин\",\"id\":0,\"Login\":\"subuser-petr\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567890\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":3,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":8484044,\"Amount\":227287.39,\"AmountAvailableForTransfer\":226287.39,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[226287.39]",
                    "[]",
                    "{\"CampaignEmails\":[\"k.khlystov@yandex.ru\",\"mdtester@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2013-09-24\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester@yandex.ru\",\"FIO\":\"Georgiy Черевко\",\"id\":0,\"Login\":\"mdtester\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"000\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":7,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "{\"AccountID\":7981142,\"Amount\":123456.78,\"AmountAvailableForTransfer\":123456.78,\"Currency\":\"USD\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester2@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester2\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[123456.78]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester2@yandex.ru\"],\"ClientCurrency\":\"USD\",\"DateCreate\":\"2013-10-16\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester2@yandex.ru\",\"FIO\":\"Leon Столярчук\",\"id\":0,\"Login\":\"mdtester2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":24390000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":10000000000},\"Phone\":\"1234567\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":0.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"OVERDRAFT\"]",
            }, {
                    "{\"AccountID\":8484044,\"Amount\":227287.39,\"AmountAvailableForTransfer\":226287.39,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[226287.39]",
                    "[]",
                    "{\"CampaignEmails\":[],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2014-08-01\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"wertyu@safdg.ru\",\"FIO\":\"Artyom Халтурин\",\"id\":0,\"Login\":\"mdtester-pred\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"123456789\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":7,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "{\"AccountID\":24204623,\"AgencyName\":\"Агентство для автотестов мобильных\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userb8@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-subuser-auto1\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Vadim Чечель\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":24204623,\"AgencyName\":\"Агентство для автотестов мобильных\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userb8@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-subuser-auto1\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[]",
                    "[]",
                    "{\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Vadim Чечель\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":24204623,\"AgencyName\":\"Агентство для автотестов мобильных\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userb8@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-subuser-auto1\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb8@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Vadim Чечель\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":24172419,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester-auto@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-auto\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-12\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto@yandex.ru\",\"FIO\":\"Сауле Zhukova\",\"id\":0,\"Login\":\"mdtester-auto\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":37000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":9410000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":12,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\",\"OVERDRAFT\"]",
            }, {
                    "{\"AccountID\":24172419,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester-auto@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-auto\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-12\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto@yandex.ru\",\"FIO\":\"Сауле Zhukova\",\"id\":0,\"Login\":\"mdtester-auto\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":37000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":9410000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":12,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\",\"OVERDRAFT\"]",
            }, {
                    "{\"AccountID\":12032425,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userB5@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"userB5\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"userB5@yandex.ru\"],\"DateCreate\":\"2013-01-11\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userB5@yandex.ru\",\"FIO\":\"Георгий Горин\",\"id\":0,\"Login\":\"userB5\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":1111000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":1111000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":3,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\",\"OVERDRAFT\"]",
            }, {
                    "{\"AccountID\":12032425,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userB5@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"userB5\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userB5@yandex.ru\"],\"DateCreate\":\"2013-01-11\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userB5@yandex.ru\",\"FIO\":\"Георгий Горин\",\"id\":0,\"Login\":\"userB5\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":1111000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":1111000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":3,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\",\"OVERDRAFT\"]",
            }, {
                    "{\"AccountID\":24295580,\"Amount\":123456.78,\"AmountAvailableForTransfer\":123441.78,\"Currency\":\"EUR\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userb21o@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"userb21o\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[123441.78]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb21o@yandex.ru\"],\"ClientCurrency\":\"EUR\",\"DateCreate\":\"2017-01-18\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb21o@yandex.ru\",\"FIO\":\"Влад Ангелов\",\"id\":0,\"Login\":\"userb21o\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":2848000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":1111000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":0.0}",
                    "{\"campaignsCount\":1,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"OVERDRAFT\"]",
            }, {
                    "{\"AccountID\":24295580,\"Amount\":123456.78,\"AmountAvailableForTransfer\":123441.78,\"Currency\":\"EUR\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userb21o@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"userb21o\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb21o@yandex.ru\"],\"ClientCurrency\":\"EUR\",\"DateCreate\":\"2017-01-18\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb21o@yandex.ru\",\"FIO\":\"Влад Ангелов\",\"id\":0,\"Login\":\"userb21o\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":2848000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":1111000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":0.0}",
                    "{\"campaignsCount\":1,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"OVERDRAFT\"]",
            }, {
                    "{\"AccountID\":8378733,\"AgencyName\":\"Крутое агентство\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"testestest@ya.ru\",\"MoneyWarningValue\":20},\"Login\":\"subuser-den\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"DateCreate\":\"2014-01-24\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Денис Новый\",\"id\":0,\"Login\":\"subuser-den\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"8-123-456-78-90\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":0,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":11864682,\"AgencyName\":\"Крутое агентство\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"testestest@ya.ru\",\"MoneyWarningValue\":1},\"Login\":\"subuser-dimon\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Димов Димон\",\"id\":0,\"Login\":\"subuser-dimon\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"11111111111\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":0,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":8379106,\"AgencyName\":\"Крутое агентство\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"testestest@ya.ru\",\"MoneyWarningValue\":20},\"Login\":\"subuser-petr\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Петров Пётр\",\"id\":0,\"Login\":\"subuser-petr\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567890\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":8484044,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"k.khlystov@yandex.ru\",\"mdtester@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2013-09-24\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester@yandex.ru\",\"FIO\":\"Тестировщик Мобильный\",\"id\":0,\"Login\":\"mdtester\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"000\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":6,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "{\"AccountID\":7981142,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"USD\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester2@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester2\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester2@yandex.ru\"],\"ClientCurrency\":\"USD\",\"DateCreate\":\"2013-10-16\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester2@yandex.ru\",\"FIO\":\"Телевизоров Просмотр\",\"id\":0,\"Login\":\"mdtester2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":0.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "{\"AccountID\":8484044,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2014-08-01\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"wertyu@safdg.ru\",\"FIO\":\"Василий Тёркин\",\"id\":0,\"Login\":\"mdtester-pred\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"123456789\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":6,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "{\"AccountID\":24172419,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester-auto@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-auto\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-12\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto@yandex.ru\",\"FIO\":\"Авто Мобильный Тестировщик\",\"id\":0,\"Login\":\"mdtester-auto\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "{\"AccountID\":24204623,\"AgencyName\":\"Агентство для автотестов мобильных\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userb8@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-subuser-auto1\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Субклиент Автосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":24204623,\"AgencyName\":\"Агентство для автотестов мобильных\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userb8@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-subuser-auto1\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[]",
                    "[]",
                    "{\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Субклиент Автосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":24204623,\"AgencyName\":\"Агентство для автотестов мобильных\",\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userb8@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-subuser-auto1\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb8@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Субклиент Автосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[\"TERMINAL\"]",
            }, {
                    "{\"AccountID\":24172419,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester-auto@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-auto\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-12\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto@yandex.ru\",\"FIO\":\"Авто Мобильный Тестировщик\",\"id\":0,\"Login\":\"mdtester-auto\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "{\"AccountID\":24172419,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Currency\":\"RUB\",\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"mdtester-auto@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"mdtester-auto\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-12\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto@yandex.ru\",\"FIO\":\"Авто Мобильный Тестировщик\",\"id\":0,\"Login\":\"mdtester-auto\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "{\"AccountID\":12032425,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userB5@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"userB5\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[0.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"userB5@yandex.ru\"],\"DateCreate\":\"2013-01-11\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userB5@yandex.ru\",\"FIO\":\"Сипин Петр\",\"id\":0,\"Login\":\"userB5\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":4,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "{\"AccountID\":12032425,\"Amount\":0.0,\"AmountAvailableForTransfer\":0.0,\"Discount\":0.0,\"EmailNotification\":{\"Email\":\"userB5@yandex.ru\",\"MoneyWarningValue\":20},\"Login\":\"userB5\",\"SmsNotification\":{\"MoneyInSms\":\"No\",\"MoneyOutSms\":\"No\",\"SmsTimeFrom\":\"09:00\",\"SmsTimeTo\":\"21:00\"}}",
                    "null",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userB5@yandex.ru\"],\"DateCreate\":\"2013-01-11\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userB5@yandex.ru\",\"FIO\":\"Сипин Петр\",\"id\":0,\"Login\":\"userB5\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":4,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            },
    };

    private static final String[][] SHORT_CAMPAIGN_INFO_TEST_DATA = new String[][]{
            {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":7010992,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Выжигание\",\"Rest\":{\"Amount\":2571000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-06-20\",\"Status\":\"Нет активных объявлений. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2571000000},\"SumAvailableForTransfer\":{\"Amount\":2556000000}}",
                    "[2556.0,5000.0]",
                    "[{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":8307447,\"Clicks\":0,\"ContextStrategyName\":\"MaximumCoverage\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Экстра-кампания\",\"Rest\":{\"Amount\":5000000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2014-01-23\",\"Status\":\"Кампания остановлена. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":5000000000},\"SumAvailableForTransfer\":{\"Amount\":5000000000}}]",
                    "{\"CampaignEmails\":[\"testestest@ya.ru\"],\"ClientRights\":[{\"RightName\":\"AllowEditCampaigns\",\"Value\":\"Yes\"},{\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"RightName\":\"AllowTransferMoney\",\"Value\":\"Yes\"}],\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Вардан Вотякова\",\"id\":0,\"Login\":\"subuser-leha\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"123-45-67\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":true,\"isSharedAccountEnabled\":false}",
                    "[\"TRANSFER\", \"TERMINAL\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":12495223,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Тест оповещений\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2015-04-17\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Танюшка Бухарин\",\"id\":0,\"Login\":\"subuser-petr\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567890\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":3,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204603,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Конструкторы\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Vadim Чечель\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":80966593,\"Clicks\":0,\"ContextStrategyName\":\"MaximumCoverage\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"(копия) На модерацию\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2016-10-24\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"k.khlystov@yandex.ru\",\"mdtester@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2013-09-24\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester@yandex.ru\",\"FIO\":\"Georgiy Черевко\",\"id\":0,\"Login\":\"mdtester\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"000\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":7,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"USD\",\"CampaignID\":7981397,\"Clicks\":0,\"ContextStrategyName\":\"MaximumCoverage\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"DRAFT\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-11-27\",\"Status\":\"Кампания остановлена\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"New\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester2@yandex.ru\"],\"ClientCurrency\":\"USD\",\"DateCreate\":\"2013-10-16\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester2@yandex.ru\",\"FIO\":\"Leon Столярчук\",\"id\":0,\"Login\":\"mdtester2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":24390000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":10000000000},\"Phone\":\"1234567\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":0.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":80966593,\"Clicks\":0,\"ContextStrategyName\":\"MaximumCoverage\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"(копия) На модерацию\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2016-10-24\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2014-08-01\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"wertyu@safdg.ru\",\"FIO\":\"Artyom Халтурин\",\"id\":0,\"Login\":\"mdtester-pred\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"123456789\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":7,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":12495223,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Тест оповещений\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2015-04-17\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"11111@ya.ru\",\"testestest@ya.ru\"],\"ClientRights\":[{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"Yes\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Танюшка Бухарин\",\"id\":0,\"Login\":\"subuser-petr\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567890\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":3,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":7010968,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Большая кампания автобусы\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-06-20\",\"Status\":\"Ожидает модерации\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Pending\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"testestest@ya.ru\"],\"ClientRights\":[{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Наталя Арестов\",\"id\":0,\"Login\":\"subuser-dimon\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"11111111111\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":0,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":7257832,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Большая кампания пароходы\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-08-06\",\"Status\":\"Отклонено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"No\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"testestest@ya.ru\"],\"ClientRights\":[{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Наталя Арестов\",\"id\":0,\"Login\":\"subuser-dimon\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"11111111111\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":0,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":7257820,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Большая кампания самолеты\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-08-06\",\"Status\":\"Отклонено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"No\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"testestest@ya.ru\"],\"ClientRights\":[{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Наталя Арестов\",\"id\":0,\"Login\":\"subuser-dimon\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"11111111111\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":0,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":7010992,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Выжигание\",\"Rest\":{\"Amount\":2571000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-06-20\",\"Status\":\"Нет активных объявлений. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2571000000},\"SumAvailableForTransfer\":{\"Amount\":2556000000}}",
                    "[2556.0,5000.0]",
                    "[{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":7010992,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Выжигание\",\"Rest\":{\"Amount\":2571000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-06-20\",\"Status\":\"Нет активных объявлений. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2571000000},\"SumAvailableForTransfer\":{\"Amount\":2556000000}},{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":8307447,\"Clicks\":0,\"ContextStrategyName\":\"MaximumCoverage\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Экстра-кампания\",\"Rest\":{\"Amount\":5000000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2014-01-23\",\"Status\":\"Кампания остановлена. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":5000000000},\"SumAvailableForTransfer\":{\"Amount\":5000000000}}]",
                    "{\"CampaignEmails\":[\"testestest@ya.ru\"],\"ClientRights\":[{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"Yes\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"Yes\"}],\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Вардан Вотякова\",\"id\":0,\"Login\":\"subuser-leha\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"123-45-67\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24207415,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Статусы объявлений 1\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-12\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto@yandex.ru\",\"FIO\":\"Сауле Zhukova\",\"id\":0,\"Login\":\"mdtester-auto\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":37000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":941000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":12,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24201226,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"РК активная\",\"Rest\":{\"Amount\":2542000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2542000000},\"SumAvailableForTransfer\":{\"Amount\":1542000000}}",
                    "[1542.3729]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto2@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto2@yandex.ru\",\"FIO\":\"Попова Гапеева\",\"id\":0,\"Login\":\"mdtester-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":37000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":9410000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":3,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\",\"OVERDRAFT\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5412196,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"!!!Тестирование модерации!!!\",\"Rest\":{\"Amount\":10000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-10-05\",\"Status\":\"Кампания остановлена\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"WeeklyBudget\",\"Sum\":{\"Amount\":10000000},\"SumAvailableForTransfer\":{\"Amount\":10000000}}",
                    "[35.7,10.0]",
                    "[{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5379260,\"Clicks\":75,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Дорога домой\",\"Rest\":{\"Amount\":35000000},\"Shows\":27652,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-09-27\",\"Status\":\"Кампания остановлена. Идет активизация. Кампания закончилась 23.03.2013\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":55000000},\"SumAvailableForTransfer\":{\"Amount\":35000000}}]",
                    "{\"CampaignEmails\":[\"ananna3-322@yandex.ru\"],\"DateCreate\":\"2012-09-27\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"ananna3-322@yandex.ru\",\"FIO\":\"Gulya Пипкин\",\"id\":0,\"Login\":\"ananna3-322\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"TRANSFER\",\"CARD\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5379260,\"Clicks\":75,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Дорога домой\",\"Rest\":{\"Amount\":35000000},\"Shows\":27652,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-09-27\",\"Status\":\"Кампания остановлена. Идет активизация. Кампания закончилась 23.03.2013\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":55000000},\"SumAvailableForTransfer\":{\"Amount\":35000000}}",
                    "[35.7,10.0]",
                    "[{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5412196,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"!!!Тестирование модерации!!!\",\"Rest\":{\"Amount\":10000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-10-05\",\"Status\":\"Кампания остановлена\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"WeeklyBudget\",\"Sum\":{\"Amount\":10000000},\"SumAvailableForTransfer\":{\"Amount\":10000000}}]",
                    "{\"CampaignEmails\":[\"ananna3-322@yandex.ru\"],\"DateCreate\":\"2012-09-27\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"ananna3-322@yandex.ru\",\"FIO\":\"Gulya Пипкин\",\"id\":0,\"Login\":\"ananna3-322\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"TRANSFER\",\"CARD\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204603,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Конструкторы\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb8@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Vadim Чечель\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204703,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 1 без ОС\",\"Rest\":{\"Amount\":2824000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2824000000},\"SumAvailableForTransfer\":{\"Amount\":1824000000}}",
                    "[1824.5763,1824.5763]",
                    "[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":25372841,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 2 без ОС\",\"Rest\":{\"Amount\":2824000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-02-16\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2824000000},\"SumAvailableForTransfer\":{\"Amount\":1824000000}}]",
                    "{\"CampaignEmails\":[\"userb9@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"Yes\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb9@yandex.ru\",\"FIO\":\"Сауле Zhukova\",\"id\":0,\"Login\":\"mdtester-subuser-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"666-66-66\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\", \"CARD\", \"TRANSFER\", \"TERMINAL\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":25372841,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 2 без ОС\",\"Rest\":{\"Amount\":2824000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-02-16\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2824000000},\"SumAvailableForTransfer\":{\"Amount\":1824000000}}",
                    "[1824.5763,1824.5763]",
                    "[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204703,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 1 без ОС\",\"Rest\":{\"Amount\":2824000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2824000000},\"SumAvailableForTransfer\":{\"Amount\":1824000000}}]",
                    "{\"CampaignEmails\":[\"userb9@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"Yes\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb9@yandex.ru\",\"FIO\":\"Сауле Zhukova\",\"id\":0,\"Login\":\"mdtester-subuser-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"666-66-66\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\", \"CARD\", \"TRANSFER\", \"TERMINAL\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204703,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 1 без ОС\",\"Rest\":{\"Amount\":2824000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2824000000},\"SumAvailableForTransfer\":{\"Amount\":1824000000}}",
                    "[1824.5763,1824.5763]",
                    "[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":25372841,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 2 без ОС\",\"Rest\":{\"Amount\":2824000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-02-16\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2824000000},\"SumAvailableForTransfer\":{\"Amount\":1824000000}}]",
                    "{\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb9@yandex.ru\",\"FIO\":\"Сауле Zhukova\",\"id\":0,\"Login\":\"mdtester-subuser-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"666-66-66\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":true,\"isSharedAccountEnabled\":false}",
                    "[\"TERMINAL\",\"TRANSFER\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204703,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 1 без ОС\",\"Rest\":{\"Amount\":2824000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2824000000},\"SumAvailableForTransfer\":{\"Amount\":1824000000}}",
                    "[1824.5763,1824.5763]",
                    "[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":25372841,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 2 без ОС\",\"Rest\":{\"Amount\":2824000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-02-16\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":2824000000},\"SumAvailableForTransfer\":{\"Amount\":1824000000}}]",
                    "{\"CampaignEmails\":[\"userb9@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"RightName\":\"AllowTransferMoney\",\"Value\":\"Yes\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb9@yandex.ru\",\"FIO\":\"Сауле Zhukova\",\"id\":0,\"Login\":\"mdtester-subuser-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"666-66-66\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":true,\"isSharedAccountEnabled\":false}",
                    "[\"TERMINAL\",\"TRANSFER\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":25349535,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Новая\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-02-15\",\"Status\":\"Черновик\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"New\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto3@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-02-15\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto3@yandex.ru\",\"FIO\":\"Эрнст Feldman\",\"id\":0,\"Login\":\"mdtester-auto3\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":0,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5659341,\"Clicks\":0,\"ContextStrategyName\":\"WeeklyBudget\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Строительство домов\",\"Rest\":{\"Amount\":3333000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-11-28\",\"Status\":\"Кампания остановлена. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"ShowsDisabled\",\"Sum\":{\"Amount\":3333000000},\"SumAvailableForTransfer\":{\"Amount\":3333000000}}",
                    "[3333.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"userB2@yandex.ru\"],\"DateCreate\":\"2012-11-28\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userB2@yandex.ru\",\"FIO\":\"Ромка Лазутина\",\"id\":0,\"Login\":\"userB2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":1111000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":1111000000},\"Phone\":\"1234567\",\"Role\":\"Client\",\"SendAccNews\":\"No\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\",\"OVERDRAFT\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":21002811,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Копия Game of Thrones - С.-Петербург - сеть\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2016-08-18\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[3333.0]",
                    "[{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5659341,\"Clicks\":0,\"ContextStrategyName\":\"WeeklyBudget\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Строительство домов\",\"Rest\":{\"Amount\":3333000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-11-28\",\"Status\":\"Кампания остановлена. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"ShowsDisabled\",\"Sum\":{\"Amount\":3333000000},\"SumAvailableForTransfer\":{\"Amount\":3333000000}}]",
                    "{\"CampaignEmails\":[\"userB2@yandex.ru\"],\"DateCreate\":\"2012-11-28\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userB2@yandex.ru\",\"FIO\":\"Ромка Лазутина\",\"id\":0,\"Login\":\"userB2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":1111000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":1111000000},\"Phone\":\"1234567\",\"Role\":\"Client\",\"SendAccNews\":\"No\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"TRANSFER\",\"CARD\",\"OVERDRAFT\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"EUR\",\"CampaignID\":7426828,\"Clicks\":0,\"ContextStrategyName\":\"MaximumCoverage\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Мыло\",\"Rest\":{\"Amount\":3333000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-09-05\",\"Status\":\"Идут показы. Идет активизация\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"ShowsDisabled\",\"Sum\":{\"Amount\":3333000000},\"SumAvailableForTransfer\":{\"Amount\":382000000}}",
                    "[382.2119]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb21@yandex.ru\"],\"ClientCurrency\":\"EUR\",\"DateCreate\":\"2013-09-05\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb21@yandex.ru\",\"FIO\":\"Ратмир Турчин\",\"id\":0,\"Login\":\"userb21\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":25641000000},\"OverdraftSumAvailableInCurrency\":{\"Amount\":10000000000},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":0.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"OVERDRAFT\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":12495223,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Тест оповещений\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2015-04-17\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Петров Пётр\",\"id\":0,\"Login\":\"subuser-petr\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567890\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204603,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Конструкторы\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Субклиент Автосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204703,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 1 без ОС\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb9@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb9@yandex.ru\",\"FIO\":\"Субклиент Безвтосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"666-66-66\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":false}",
                    "[\"TERMINAL\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":7561045,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Амперметры\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-09-25\",\"Status\":\"Ждёт оплаты\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"k.khlystov@yandex.ru\",\"mdtester@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2013-09-24\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester@yandex.ru\",\"FIO\":\"Тестировщик Мобильный\",\"id\":0,\"Login\":\"mdtester\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"000\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":6,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":7561045,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Амперметры\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-09-25\",\"Status\":\"Ждёт оплаты\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2014-08-01\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"wertyu@safdg.ru\",\"FIO\":\"Василий Тёркин\",\"id\":0,\"Login\":\"mdtester-pred\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"123456789\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":6,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":12495223,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Тест оповещений\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2015-04-17\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"11111@ya.ru\",\"testestest@ya.ru\"],\"ClientRights\":[{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"Yes\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Петров Пётр\",\"id\":0,\"Login\":\"subuser-petr\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567890\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Крутое агентство\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":7257834,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Большая кампания повозки\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2013-08-06\",\"Status\":\"Отклонено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"No\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"testestest@ya.ru\"],\"ClientRights\":[{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Крутое агентство\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2012-12-26\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"testestest@ya.ru\",\"FIO\":\"Димов Димон\",\"id\":0,\"Login\":\"subuser-dimon\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"11111111111\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":0,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24207415,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Статусы объявлений 1\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-12\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto@yandex.ru\",\"FIO\":\"Авто Мобильный Тестировщик\",\"id\":0,\"Login\":\"mdtester-auto\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24201226,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"РК активная\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto2@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto2@yandex.ru\",\"FIO\":\"Авто Мобильный Тестировщик\",\"id\":0,\"Login\":\"mdtester-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":3,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5412196,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"!!!Тестирование модерации!!!\",\"Rest\":{\"Amount\":10000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-10-05\",\"Status\":\"Кампания остановлена\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"WeeklyBudget\",\"Sum\":{\"Amount\":10000000},\"SumAvailableForTransfer\":{\"Amount\":10000000}}",
                    "[10.0]",
                    "[]",
                    "{\"CampaignEmails\":[\"ananna3-322@yandex.ru\"],\"DateCreate\":\"2012-09-27\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"ananna3-322@yandex.ru\",\"FIO\":\"Бирюкова Анна\",\"id\":0,\"Login\":\"ananna3-322\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":3,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5379260,\"Clicks\":75,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Дорога домой\",\"Rest\":{\"Amount\":0},\"Shows\":27652,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-09-27\",\"Status\":\"Средства на счете закончились. Кампания закончилась 23.03.2013\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":20000000},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[10.0]",
                    "[{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5412196,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"!!!Тестирование модерации!!!\",\"Rest\":{\"Amount\":10000000},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-10-05\",\"Status\":\"Кампания остановлена\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"WeeklyBudget\",\"Sum\":{\"Amount\":10000000},\"SumAvailableForTransfer\":{\"Amount\":10000000}}]",
                    "{\"CampaignEmails\":[\"ananna3-322@yandex.ru\"],\"DateCreate\":\"2012-09-27\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"ananna3-322@yandex.ru\",\"FIO\":\"Бирюкова Анна\",\"id\":0,\"Login\":\"ananna3-322\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":3,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"TRANSFER\",\"CARD\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204603,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Конструкторы\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb8@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb8@yandex.ru\",\"FIO\":\"Субклиент Автосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto1\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"555-55-55\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204703,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 1 без ОС\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb9@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb9@yandex.ru\",\"FIO\":\"Субклиент Безвтосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"666-66-66\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\", \"CARD\", \"TERMINAL\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":24204703,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 1 без ОС\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-01-13\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb9@yandex.ru\",\"FIO\":\"Субклиент Безвтосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"666-66-66\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":true,\"isSharedAccountEnabled\":false}",
                    "[\"TERMINAL\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":25349535,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Новая\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-02-15\",\"Status\":\"Черновик\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"New\",\"StatusShow\":\"Yes\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"mdtester-auto3@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"DateCreate\":\"2017-02-15\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"mdtester-auto3@yandex.ru\",\"FIO\":\"auto3 mdtester\",\"id\":0,\"Login\":\"mdtester-auto3\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"Yes\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":0,\"isAgency\":false,\"isSharedAccountEnabled\":true}",
                    "[]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":5659341,\"Clicks\":0,\"ContextStrategyName\":\"WeeklyBudget\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":0,\"Name\":\"Строительство домов\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2012-11-28\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"ShowsDisabled\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userB2@yandex.ru\"],\"DateCreate\":\"2012-11-28\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userB2@yandex.ru\",\"FIO\":\"Николаев Коля userB2\",\"id\":0,\"Login\":\"userB2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567\",\"Role\":\"Client\",\"SendAccNews\":\"No\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "null",
                    "{\"BonusDiscount\":0.0,\"CampaignCurrency\":\"YND_FIXED\",\"CampaignID\":21002811,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Копия Game of Thrones - С.-Петербург - сеть\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2016-08-18\",\"Status\":\"Допущено модератором\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"Yes\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userB2@yandex.ru\"],\"DateCreate\":\"2012-11-28\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userB2@yandex.ru\",\"FIO\":\"Николаев Коля userB2\",\"id\":0,\"Login\":\"userB2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"1234567\",\"Role\":\"Client\",\"SendAccNews\":\"No\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\"}",
                    "{\"campaignsCount\":2,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[\"YANDEX_MONEY\",\"TERMINAL\",\"CARD\"]",
            }, {
                    "null",
                    "{\"AgencyName\":\"Агентство для автотестов мобильных\",\"BonusDiscount\":0.0,\"CampaignCurrency\":\"RUB\",\"CampaignID\":25372841,\"Clicks\":0,\"ContextStrategyName\":\"Default\",\"DayBudgetEnabled\":\"Yes\",\"IsActive\":\"No\",\"limitPercent\":100,\"Name\":\"Кампания 2 без ОС\",\"Rest\":{\"Amount\":0},\"Shows\":0,\"SourceCampaignID\":\"null\",\"StartDate\":\"2017-02-16\",\"Status\":\"Черновик\",\"StatusActivating\":\"Pending\",\"StatusArchive\":\"No\",\"StatusModerate\":\"New\",\"StatusShow\":\"No\",\"StrategyName\":\"HighestPosition\",\"Sum\":{\"Amount\":0},\"SumAvailableForTransfer\":{\"Amount\":0}}",
                    "[]",
                    "[]",
                    "{\"CampaignEmails\":[\"userb9@yandex.ru\"],\"ClientCurrency\":\"RUB\",\"ClientRights\":[{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowEditCampaigns\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowImportXLS\",\"Value\":\"No\"},{\"AgencyName\":\"Агентство для автотестов мобильных\",\"RightName\":\"AllowTransferMoney\",\"Value\":\"No\"}],\"DateCreate\":\"2017-01-13\",\"Discount\":0.0,\"DisplayStoreRating\":\"Yes\",\"Email\":\"userb9@yandex.ru\",\"FIO\":\"Субклиент Безвтосчётович\",\"id\":0,\"Login\":\"mdtester-subuser-auto2\",\"NonResident\":\"No\",\"OverdraftSumAvailable\":{\"Amount\":0},\"OverdraftSumAvailableInCurrency\":{\"Amount\":0},\"Phone\":\"666-66-66\",\"Role\":\"Client\",\"SendAccNews\":\"Yes\",\"SendNews\":\"Yes\",\"SendWarn\":\"Yes\",\"SharedAccountEnabled\":\"No\",\"StatusArch\":\"No\",\"VATRate\":18.0}",
                    "{\"campaignsCount\":1,\"isAgency\":false,\"isSharedAccountEnabled\":false}",
                    "[]",
            }
    };
}
