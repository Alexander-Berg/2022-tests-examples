const getGroupInfo = require('./getGroupInfo');

const COMPLECTATIONS = [
    {
        offer_count: 10,
        complectation_name: 'Ultra',
        complectation_id: '1',
        price_from: {
            RUR: 100000,
        },
        price_to: {
            RUR: 200000,
        },
        tech_info: {
            tech_param: {
                id: '111',
            },
        },
    },
    {
        offer_count: 10,
        complectation_name: 'Not Ultra',
        complectation_id: '3',
        price_from: {
            RUR: 300000,
        },
        price_to: {
            RUR: 500000,
        },
        tech_info: {
            tech_param: {
                id: '111',
            },
        },
    },
];

const CARD = {
    vehicle_info: {
        tech_param: {
            id: '111',
        },
        complectation: {
            name: 'Ultra',
        },
    },
};

const state = {
    card: CARD,
    cardGroupComplectations: {
        data: {
            complectations: COMPLECTATIONS,
        },
    },
};

it('должен вытащить данные группы из стейта по имени комплектации и значению tech_param_id', () => {
    expect(getGroupInfo(state)).toStrictEqual({
        priceFrom: {
            RUR: 100000,
        },
        priceTo: {
            RUR: 200000,
        },
        offersCount: 10,
    });
});
