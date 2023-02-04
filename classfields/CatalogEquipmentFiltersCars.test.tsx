import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';

jest.mock('auto-core/react/dataDomain/equipmentFilters/actions/fetch', () => {
    return jest.fn(() => () => {});
});

import fetchEquipmentFilters from 'auto-core/react/dataDomain/equipmentFilters/actions/fetch';

import CatalogEquipmentFilter from './CatalogEquipmentFiltersCars';

const store = {
    equipmentFilters: {
        data: {},
    },
};

it('не должен запросить фильтры при маунте', () => {
    shallow(
        <CatalogEquipmentFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value={ [] }
            searchParameters={{ category: 'cars' }}
        />,
        { context: { store: mockStore(store) } },
    ).dive();
    expect(fetchEquipmentFilters).not.toHaveBeenCalled();
});

it('должен запросить фильтры при открытии поп-апа', () => {
    const tree = shallow(
        <CatalogEquipmentFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value={ [] }
            searchParameters={{ category: 'cars' }}
        />,
        { context: { store: mockStore(store) } },
    ).dive();
    tree.setState({ opened: true });
    expect(fetchEquipmentFilters).toHaveBeenCalledWith({ category: 'cars' });
});

it('не должен перезапросить фильтры при повторном открытии поп-апа', () => {
    const tree = shallow(
        <CatalogEquipmentFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value={ [] }
            searchParameters={{ category: 'cars' }}
        />,
        { context: { store: mockStore(store) } },
    ).dive();
    tree.setState({ opened: true });
    // запросили 1 раз
    expect(fetchEquipmentFilters).toHaveBeenCalledTimes(1);
    tree.setState({ opened: false });
    tree.setState({ opened: true });
    // все еще 1 запрос
    expect(fetchEquipmentFilters).toHaveBeenCalledTimes(1);
});

it('должен перезапросить фильтры при повторном открытии поп-апа после смены параметров', () => {
    const tree = shallow(
        <CatalogEquipmentFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value={ [] }
            searchParameters={{ category: 'cars' }}
        />,
        { context: { store: mockStore(store) } },
    );
    tree.dive().setState({ opened: true });
    // запросили 1 раз
    expect(fetchEquipmentFilters).toHaveBeenCalledTimes(1);
    tree.dive().setState({ opened: false });
    tree.setProps({ searchParameters: { category: 'cars', section: 'new' } });
    tree.dive().setState({ opened: true });
    // поменяли серч-параметры -- перезапросили фильтры
    expect(fetchEquipmentFilters).toHaveBeenCalledTimes(2);
});
