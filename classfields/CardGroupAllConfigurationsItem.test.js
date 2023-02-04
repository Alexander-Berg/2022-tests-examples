const React = require('react');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const CardGroupAllConfigurationsItem = require('./CardGroupAllConfigurationsItem');

const compareMock = require('autoru-frontend/mockData/state/compare.mock');

const store = mockStore({
    compare: compareMock,
});

const COMPLECTATION = {
    complectation_id: '123456',
    tech_info: {
        tech_param: {
            id: '123',
            displacement: 1968,
            power: 192,
            engine_type: 'GASOLINE',
            transmission: 'ROBOT',
            gear_type: 'FORWARD_CONTROL',
        },
        configuration: {
            body_type: 'ALLROAD_5_DOORS',
        },
    },
    offer_count: 31,
    price_from: {
        RUR: 1000500,
    },
};

const SELECTED_COMPLECTATION = {
    complectation_id: '78910',
    tech_info: {
        tech_param: {
            id: '123',
            displacement: 1968,
            power: 192,
            engine_type: 'DIESEL',
            transmission: 'VARIATOR',
            gear_type: 'FORWARD_CONTROL',
        },
        configuration: {
            body_type: 'ALLROAD_3_DOORS',
        },
    },
    offer_count: 31,
    price_from: {
        RUR: 1000500,
    },
};

it('не должен содержать выделенных характеристик, если все характеристики совпадают', async() => {
    const tree = shallow(
        <CardGroupAllConfigurationsItem
            complectation={ COMPLECTATION }
            selectedComplectation={ COMPLECTATION }
            url=""
            store={ store }
        />,
    );
    expect(tree.find('.CardGroupAllConfigurationsItem__valueSelected')).toHaveLength(0);
});

it('должен выделить не совпадающие харакееристики', async() => {
    const tree = shallow(
        <CardGroupAllConfigurationsItem
            complectation={ COMPLECTATION }
            selectedComplectation={ SELECTED_COMPLECTATION }
            url=""
            store={ store }
        />,
    );
    expect(tree.find('.CardGroupAllConfigurationsItem__value_transmission.CardGroupAllConfigurationsItem__valueSelected')).toHaveLength(1);
    expect(tree.find('.CardGroupAllConfigurationsItem__value_engineInfo.CardGroupAllConfigurationsItem__valueSelected')).toHaveLength(1);
});
