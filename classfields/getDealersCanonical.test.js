const getDealersCanonical = require('./getDealersCanonical');

it('возвращает правильный url для dealers-listing-net', () => {
    const pageParams = {
        dealer_net_semantic_url: 'major',
    };

    const state = {
        router: {
            current: {
                name: 'dealers-listing-net',
            },
        },
    };

    expect(getDealersCanonical(state, pageParams)).toEqual('/dealer-net/major/');
});

it('возвращает правильный url для dealers-listing', () => {
    const pageParams = {
        params: {
            category: 'cars',
            dealer_org_type: '1',
        },
    };

    const state = {
        router: {
            current: {
                name: 'dealers-listing',
            },
        },
    };

    expect(getDealersCanonical(state, pageParams)).toEqual('/dilery/cars/new/');
});
