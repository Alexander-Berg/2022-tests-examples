jest.mock('../ads/isRegionFromBunkerDict', () => jest.fn());
const getCode = require('./ads.getCode');
const isRegionFromBunkerDict = require('../ads/isRegionFromBunkerDict');

const getController = (params, type) => {
    return {
        getParams: () => params,
        type,
        req: {
            experimentsData: {
                has: jest.fn().mockReturnValue(true),
            },
        },
    };
};

const TESTS = [
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'cars',
            section: 'new',
        },
        type: 'listing',
        result: 'listing-cars-new',
    },
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'cars',
            section: 'new',
        },
        type: 'listing',
        result: 'listing-rids-cars-new',
        rids: true,
    },
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'cars',
            section: 'used',
        },
        type: 'listing',
        result: 'listing-cars-used',
    },
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'moto',
        },
        type: 'listing',
        result: 'listing-moto',
    },
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'moto',
        },
        type: 'listing',
        result: 'listing-rids-moto',
        rids: true,
    },
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'trucks',
        },
        type: 'listing',
        result: 'listing-commercial',
    },
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'trucks',
        },
        type: 'listing',
        result: 'listing-rids-commercial',
        rids: true,
    },
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'trucks',
            trucks_category: 'TRUCK',
        },
        type: 'listing',
        result: 'listing-truck',
    },
    {
        platform: 'desktop',
        appId: 'af-desktop',
        params: {
            category: 'trucks',
            trucks_category: 'LCV',
        },
        type: 'listing',
        result: 'listing-lcv',
    },
    {
        platform: 'desktop',
        appId: 'af-desktop-lk',
        params: {
            category: 'trucks',
        },
        type: 'sales',
        result: 'lk-offers',
    },
    // mobile
    {
        platform: 'mobile',
        appId: 'af-mobile',
        params: {
            category: 'cars',
            section: 'all',
        },
        type: 'listing',
        result: 'listing-cars',
    },
    {
        platform: 'mobile',
        appId: 'af-mobile',
        params: {
            category: 'trucks',
            section: 'all',
            trucks_category: 'TRUCK',
        },
        type: 'listing',
        result: 'listing-commercial',
    },
    {
        platform: 'mobile',
        appId: 'af-mobile',
        params: {
            category: 'moto',
            section: 'all',
            trucks_category: 'ATV',
        },
        type: 'listing',
        result: 'listing-moto',
    },
    {
        platform: 'mobile',
        appId: 'af-mobile',
        params: {
            category: 'cars',
            section: 'new',
            from: 'marketplace',
        },
        type: 'listing',
        result: 'listing-cars',
    },
    {
        platform: 'mobile',
        appId: 'af-mobile',
        params: {
            category: 'trucks',
        },
        type: 'sales',
        result: 'lk-offers',
    },
    {
        platform: 'mobile',
        appId: 'af-mobile',
        params: {
            category: 'cars',
            section: 'new',
        },
        type: 'listing',
        result: 'marketplace-index',
    },
    {
        platform: 'mobile',
        appId: 'af-mobile',
        params: {
            category: 'cars',
            section: 'new',
            catalog_filter: [ {
                mark: 'renault',
            } ],
        },
        type: 'listing',
        result: 'marketplace-mark',
    },
    {
        platform: 'mobile',
        appId: 'af-mobile',
        params: {
            category: 'cars',
            section: 'new',
            catalog_filter: [ {
                mark: 'renault',
                model: 'arkana',
                generation: '123',
                configuration: '345',
            } ],
        },
        type: 'listing',
        result: 'marketplace-model',
    },
];

TESTS.forEach((test) => {
    it(JSON.stringify(test), () => {
        isRegionFromBunkerDict.mockImplementation(() => test.rids === true);
        expect(
            getCode(
                getController(
                    test.params,
                    test.type),
                test.appId,
                test.platform,
            ),
        ).toEqual(test.result);
    },
    );
});
