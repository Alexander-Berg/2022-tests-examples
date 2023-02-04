const getBodyTypeGroupForSeo = require('./getBodyTypeGroupForSeo');

it('должен вернуть кузов, если он один', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    body_type_group: [ 'SEDAN' ],
                },
            },
        },
    };
    expect(getBodyTypeGroupForSeo(state)).toEqual('SEDAN');
});

it('должен вернуть hatchback, если выбрана группа hatchback', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    body_type_group: [ 'HATCHBACK', 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'LIFTBACK' ],
                },
            },
        },
    };
    expect(getBodyTypeGroupForSeo(state)).toEqual('HATCHBACK');
});

it('не должен вернуть hatchback, если выбрана не вся группа hatchback', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    body_type_group: [ 'HATCHBACK', 'HATCHBACK_3_DOORS' ],
                },
            },
        },
    };
    expect(getBodyTypeGroupForSeo(state)).toEqual('');
});

it('должен вернуть allroad, если выбрана группа allroad', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    body_type_group: [ 'ALLROAD', 'ALLROAD_3_DOORS', 'ALLROAD_5_DOORS' ],
                },
            },
        },
    };
    expect(getBodyTypeGroupForSeo(state)).toEqual('ALLROAD');
});

it('не должен вернуть allroad, если выбрана не вся группа allroad', () => {
    const state = {
        listing: {
            data: {
                search_parameters: {
                    body_type_group: [ 'ALLROAD', 'ALLROAD_3_DOORS' ],
                },
            },
        },
    };
    expect(getBodyTypeGroupForSeo(state)).toEqual('');
});
