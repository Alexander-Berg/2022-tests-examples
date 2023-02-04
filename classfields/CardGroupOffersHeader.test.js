const React = require('react');
const { shallow } = require('enzyme');

const CardGroupOffersHeader = require('./CardGroupOffersHeader');
const CardGroupOffersFilters = require('../CardGroupOffersFilters');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const stateMock = require('auto-core/react/dataDomain/cardGroup/mocks/state.mock');

const getCardGroupFilterItems = require('auto-core/react/dataDomain/cardGroup/selectors/getCardGroupFilterItems').default;
const getCardGroupFilterValues = require('auto-core/react/dataDomain/cardGroup/selectors/getCardGroupFilterValues');

const baseProps = {
    groupInfo: {
        mark: { code: 'mercedes', name: 'Mercedes-Benz', cyrName: 'Мерседес-Бенц' },
        model: { code: 'e_klasse', name: 'E-Класс', cyrName: 'Е-класс' },
        generation: { id: '20743500', name: 'V (W213, S213, C238)' },
        configuration: { id: '20743538', bodyTypeGroup: 'SEDAN', humanBodyType: 'Седан', humanBodyTypeFull: 'Седан' },
        minPrice: 2464280,
        maxPrice: 5698400,
        vendorColors: [
            { body_color_id: 20767136, hex_codes: [ '2C3047' ], color_type: 'METALLIC' },
        ],
        mainPhoto: {
            sizes: {
                orig: '//avatars.mds.yandex.net/get-verba/1030388/2a000001609072024cf91b018a617dee8397/orig',
                wizardv3mr: '//avatars.mds.yandex.net/get-verba/1030388/2a000001609072024cf91b018a617dee8397/wizardv3mr',
                cattouch: '//avatars.mds.yandex.net/get-verba/1030388/2a000001609072024cf91b018a617dee8397/cattouch',
            },
        },
        photoUrl: '//avatars.mds.yandex.net/get-verba/1540742/2a0000016adaebb8410c9c04a756368dfd56/cattouch',
        offersCount: 289,
    },
    filterItems: getCardGroupFilterItems(stateMock),
    filterValues: getCardGroupFilterValues(stateMock),
    isPending: false,
    onChangeFilter: jest.fn,
    onFilterSubmit: jest.fn,
    priceRange: {},
    totalOffersCount: 99,
    filteredOffersCount: 99,
};

it('должен отрендерить горизонтальный блок фильтров над списком офферов, если есть офферы', () => {
    const tree = shallow(
        <CardGroupOffersHeader
            { ...baseProps }
        />,
        { context: contextMock },
    );
    expect(
        tree.find(CardGroupOffersFilters),
    ).toHaveLength(1);
});

it('не должен отрендерить горизонтальный блок фильтров над списком офферов, если нет офферов', () => {
    const tree = shallow(
        <CardGroupOffersHeader
            { ...baseProps }
            totalOffersCount={ 0 }
        />,
        { context: contextMock },
    );
    expect(
        tree.find(CardGroupOffersFilters),
    ).toHaveLength(0);
});
