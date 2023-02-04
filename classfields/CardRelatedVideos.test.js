const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const carsMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const motoMock = require('auto-core/react/dataDomain/card/mocks/card.scooters.mock');
const truckMock = require('auto-core/react/dataDomain/card/mocks/card.bus.mock');

const CardRelatedVideos = require('./CardRelatedVideos');

it('должен передать GenericItemsList правильные параметры в случае cars', () => {
    const store = {
        card: carsMock,
    };

    const innerProps = shallow(
        <CardRelatedVideos store={ mockStore(store) }/>,
        { context: contextMock },
    ).dive().find('GenericItemsList').props().resourceParams;

    expect(innerProps).toEqual({
        category: 'cars',
        mark: 'FORD',
        model: 'ECOSPORT',
        super_gen: '20104320',
        page_size: 4,
    });
});

it('должен передать GenericItemsList правильные параметры в случае moto', () => {
    const store = {
        card: motoMock,
    };

    const innerProps = shallow(
        <CardRelatedVideos store={ mockStore(store) }/>,
        { context: contextMock },
    ).dive().find('GenericItemsList').props().resourceParams;

    expect(innerProps).toEqual({
        category: 'moto',
        mark: 'SUZUKI',
        model: 'BURGMAN_400',
        page_size: 4,
    });
});

it('должен передать GenericItemsList правильные параметры в случае trucks', () => {
    const store = {
        card: truckMock,
    };

    const innerProps = shallow(
        <CardRelatedVideos store={ mockStore(store) }/>,
        { context: contextMock },
    ).dive().find('GenericItemsList').props().resourceParams;

    expect(innerProps).toEqual({
        category: 'trucks',
        mark: 'DAEWOO',
        model: 'BH_120',
        page_size: 4,
    });
});
