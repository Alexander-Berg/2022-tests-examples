const getSearchParametersCounters = require('./getSearchParametersCounters');

it('должен вернуть объект с полями main и extended', () => {
    expect(getSearchParametersCounters({
        listing: {
            data: {
                search_parameters: {
                    creation_date_from: '2020-01-01',
                    creation_date_to: '2020-01-01',
                    only_last_seller: true,
                    last_event_types: [ 'AUTORU_OFFERS_NEW' ],
                    seller_type: 'PRIVATE',
                    is_sold: false,
                    catalog_filter: [ {} ],
                    km_age_from: 2000,
                    km_age_to: 20000,
                    geo_radius: 200,
                    rid: [ 213 ],
                },
            },
        },
    })).toEqual({
        main: 2,
        extended: 3,
    });
});
