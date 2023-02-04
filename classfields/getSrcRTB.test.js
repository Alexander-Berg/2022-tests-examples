const getSrcRTB = require('./getSrcRTB');

jest.mock('auto-core/react/lib/cookie', () => {
    return {
        get: jest.fn(() => 'SUID'),
    };
});

jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(() => 'LID_KEY'),
}));

jest.mock('nanoid', () => ({
    customAlphabet: () => () => 'L123K',
}));

const TESTS = [
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'desktop_index',
                currentUrl: 'CURRENT_URL',
                referer: 'REFERER_URL',
            },
            isConversion: false,
            isBEM: false,
        },
        result: 'https://creativecdn.com/tags?type=iframe&id=pr_c2rHi7OQgV0u3CcfQ00B_home&id=pr_c2rHi7OQgV0u3CcfQ00B_uid_SUID' +
            '&id=pr_c2rHi7OQgV0u3CcfQ00B_lid_LID_KEY&su=CURRENT_URL&sr=REFERER_URL',
    },
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'mobile_index',
                currentUrl: 'CURRENT_URL',
                referer: 'REFERER_URL',
            },
            isConversion: false,
            isBEM: false,
        },
        result: 'https://creativecdn.com/tags?type=iframe&id=pr_c2rHi7OQgV0u3CcfQ00B_home&id=pr_c2rHi7OQgV0u3CcfQ00B_uid_SUID' +
            '&id=pr_c2rHi7OQgV0u3CcfQ00B_lid_LID_KEY&su=CURRENT_URL&sr=REFERER_URL',

    },
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'listing_cars',
                section: 'used',
                currentUrl: 'CURRENT_URL',
                referer: 'REFERER_URL',
            },
            isConversion: false,
            isBEM: false,
        },
        result: 'https://creativecdn.com/tags?type=iframe&id=pr_c2rHi7OQgV0u3CcfQ00B_listing&id=pr_c2rHi7OQgV0u3CcfQ00B_uid_SUID' +
            '&id=pr_c2rHi7OQgV0u3CcfQ00B_lid_LID_KEY&su=CURRENT_URL&sr=REFERER_URL',
    },
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'card_cars',
                product_id: 'PRODUCT_ID',
                currentUrl: 'CURRENT_URL',
                referer: 'REFERER_URL',
            },
            isConversion: false,
            isBEM: false,
        },
        result: 'https://creativecdn.com/tags?type=iframe&id=pr_c2rHi7OQgV0u3CcfQ00B_offer_PRODUCT_ID' +
            '&id=pr_c2rHi7OQgV0u3CcfQ00B_uid_SUID&id=pr_c2rHi7OQgV0u3CcfQ00B_lid_LID_KEY&su=CURRENT_URL&sr=REFERER_URL',
    },
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'desktop_index',
                sellerType: 'dealerbu',
                product_price: 'PRODUCT_PRICE',
                product_id: 'PRODUCT_ID',
                suid: 'SUID',
                currentUrl: 'CURRENT_URL',
                referer: 'REFERER_URL',
            },
            isConversion: true,
            isBEM: true,
        },
        result: 'https://creativecdn.com/tags?type=iframe&id=pr_c2rHi7OQgV0u3CcfQ00B_orderstatus2_PRODUCT_PRICE_dealerbu-DATENOWL123K_PRODUCT_ID' +
            '&id=pr_c2rHi7OQgV0u3CcfQ00B_uid_SUID&cd=default&su=CURRENT_URL&sr=REFERER_URL&ts=DATENOW',
    },
    {
        data: {
            marketingData: {
                experiments: {
                    TEST_EXP: true,
                },
                favorites: [],
                geoIdsParents: {},
                hashEmail: 'cf3ed6ba24022ad5b5c86748fb781f842e8566740777251d886013e8a5c9afc0',
                pageName: 'desktop_listing_cars',
                pageParams: {
                    category: 'cars',
                    section: 'all',
                    catalog_filter: [],
                },
                products: [ '1092340742-9b0084c9',
                    '1092791578-5a6dd130',
                    '1096920528-dbf3c34e',
                    '1090542954-79d9ef99',
                ],
                section: 'all',
                currentUrl: 'CURRENT_URL',
                referer: 'REFERER_URL',
            },
        },
        result: 'https://creativecdn.com/tags?type=iframe&id=pr_c2rHi7OQgV0u3CcfQ00B_listing_1092340742-9b0084c9,' +
            '1092791578-5a6dd130,1096920528-dbf3c34e,1090542954-79d9ef99&id=pr_c2rHi7OQgV0u3CcfQ00B_uid_SUID' +
            '&id=pr_c2rHi7OQgV0u3CcfQ00B_lid_LID_KEY&su=CURRENT_URL&sr=REFERER_URL',
    },
];

describe('Src for retargeting iframe', function() {

    TESTS.forEach(function(testCase) {
        it(JSON.stringify(testCase.data), function() {
            const timestmpRegEx = /&ts=\d+/;
            const orderID = /_DATENOW[A-Za-z\d]+/;
            expect(getSrcRTB(testCase.data.marketingData, { isConversion: testCase.data.isConversion, isBEM: testCase.data.isBEM })
                .replace(timestmpRegEx, '')
                .replace(orderID, '_DATENOW'))
                .toEqual(testCase.result);
        });

    });

});
