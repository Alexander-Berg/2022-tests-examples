const getSrc = require('./getSrc');

const TESTS = [
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'desktop_index',
            },
            log: false,
        },
        result: 'https://promo.autoru_frontend.base_domain/retargeting/?pageName=desktop_index',
    },
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'mobile_index',
            },
            log: false,
        },
        result: 'https://promo.autoru_frontend.base_domain/retargeting/?pageName=mobile_index',
    },
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'desktop_index',
            },
            log: true,
        },
        result: 'https://promo.autoru_frontend.base_domain/retargeting/?pageName=desktop_index&log=true',
    },
    {
        data: {
            marketingData: {
                experiments: {
                    random_experiment: true,
                },
                pageName: 'desktop_index',
            },
            log: true,
        },
        result: 'https://promo.autoru_frontend.base_domain/retargeting/?pageName=desktop_index&log=true',
    },
    {
        data: {
            marketingData: {
                experiments: {},
                pageName: 'desktop_index',
            },
            noads: true,
        },
        result: 'https://promo.autoru_frontend.base_domain/retargeting/?pageName=desktop_index&noads=true',
    },
];

describe('Src for retargeting iframe', function() {

    TESTS.forEach(function(testCase) {
        it(JSON.stringify(testCase.data), function() {
            expect(getSrc(testCase.data)).toEqual(testCase.result);
        });

    });

});
