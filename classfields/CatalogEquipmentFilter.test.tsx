import React from 'react';
import { shallow } from 'enzyme';

import type { Props, FilterState } from './CatalogEquipmentFilter';
import CatalogEquipmentFilter from './CatalogEquipmentFilter';

class TestFilter extends CatalogEquipmentFilter<Props, FilterState> {
    isPending=() => false;
    getCategories=() => [];
}

it('правильно вычисляет количество опций 0', () => {
    const tree = shallow(
        <CatalogEquipmentFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value={ [] }
        />,
    );
    expect(tree.find('.AbstractPopupFilter__titleDescriptionText').isEmptyRender()).toBe(true);
});

it('правильно вычисляет количество опций 1', () => {
    const tree = shallow(
        <CatalogEquipmentFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value={ [ 'aaa' ] }
        />,
    );
    expect(tree.find('.AbstractPopupFilter__titleDescriptionText').text()).toBe('1');
});

it('правильно вычисляет количество опций, когда есть несколько групп опций', () => {
    const tree = shallow(
        <CatalogEquipmentFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value={ [ 'item0', 'item1,item2,item3' ] }
        />,
    );
    expect(tree.find('.AbstractPopupFilter__titleDescriptionText').text()).toBe('4');
});

it('правильно обрабатывает отмену изменений', () => {
    const onChange = jest.fn();
    const tree = shallow(
        <TestFilter
            category="cars"
            onChange={ onChange }
            offersCount={ 100 }
            value={ [ 'item0' ] }
        />,
    );
    tree.setState({ opened: true });
    tree.setProps({ value: [ 'item0', 'item1' ] });
    tree.find('FiltersPopup').simulate('close');
    expect(onChange).toHaveBeenCalledWith([ 'item0' ], { name: 'catalog_equipment', total: true });
});
