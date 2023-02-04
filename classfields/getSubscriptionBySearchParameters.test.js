const getSubscriptionBySearchParameters = require('./getSubscriptionBySearchParameters');

it('должен найти подписку по grouping_id в поисковых параметрах', () => {
    const subscriptionData = {
        data: {
            title: 'Mercedes-Benz E-klasse',
            category: 'CARS',
            params: {
                rid: [
                    213,
                ],
                geo_radius: 200,
                catalog_filter: [
                    { mark: 'MERCEDES', model: 'E_KLASSE', generation: '123', configuration: '456', tech_param: '789', complectation: '101112' },
                ],
                section: 'new',
            },
        },
    };

    const state = {
        subscriptions: {
            data: [ subscriptionData ],
        },
        geo: {
            gids: [ 213 ],
        },
    };
    const searchParameters = {
        geo_radius: 200,
        grouping_id: 'tech_param_id=789,complectation_id=101112',
        section: 'new',
        category: 'cars',
        catalog_filter: [ {
            mark: 'MERCEDES',
            model: 'E_KLASSE',
            generation: '123',
            configuration: '456',
        } ],
    };
    expect(getSubscriptionBySearchParameters(state, searchParameters)).toEqual(subscriptionData);
});

it('должен найти подписку на дилерский листинг', () => {
    const subscriptionData = {
        data: {
            title: 'Mercedes-Benz E-klasse',
            category: 'CARS',
            params: {
                rid: [
                    213,
                ],
                catalog_filter: [
                    { mark: 'MERCEDES' },
                ],
                section: 'used',
                dealer_id: '20879793',
            },
        },
    };

    const state = {
        subscriptions: {
            data: [ subscriptionData ],
        },
        geo: {
            gids: [ 213 ],
        },
    };
    const searchParameters = {
        dealer_code: 'major_expert_moskva_mkad_47km',
        dealer_id: '20879793',
        section: 'used',
        category: 'cars',
        catalog_filter: [ {
            mark: 'MERCEDES',
        } ],
    };
    expect(getSubscriptionBySearchParameters(state, searchParameters)).toEqual(subscriptionData);
});
