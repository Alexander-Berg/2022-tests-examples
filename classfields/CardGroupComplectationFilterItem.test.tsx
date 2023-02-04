import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import complectationFilterData from 'auto-core/react/dataDomain/cardGroup/mocks/complectationFilterData.mock';
import { nbsp } from 'auto-core/react/lib/html-entities';

import CardGroupComplectationFilterItem from './CardGroupComplectationFilterItem';

const items = {
    engineFilterItems: [],
    transmissionFilterItems: [],
    gearTypeFilterItems: [],
    colorFilterItems: [],
    availableOptionsUniqueGroups: {},
    availableOptionsGroupedItems: [],
    additionalOptionsFilterItems: [],
    additionalOptionsGroupedItems: [],
    cardGroupSearchTags: [],
    complectationFilterItems: complectationFilterData.COMPLECTATION_ITEMS,
};

const initialValues = {
    catalog_equipment: [],
    color: [],
    gear_type: [],
    tech_param_id: [],
    transmission: [],
    search_tag: [],
};

it('должен вычислить подпись, когда нет значения', () => {
    const wrapper = shallow(
        <CardGroupComplectationFilterItem
            items={ items }
            values={ initialValues }
            onChangeComplectation={ _.noop }
            onChangeOptions={ _.noop }
            selectedComplectationsNames={ [] }
        />,
    );

    const description = wrapper.find('.CardGroupComplectationFilterItem__selected');

    expect(description.children()).not.toExist();
});

it('должен вычислить подпись, когда выбрана комплектация', () => {
    const wrapper = shallow(
        <CardGroupComplectationFilterItem
            items={ items }
            values={{
                ...initialValues,
                complectation_name: 'Комплектация первая',
            }}
            onChangeComplectation={ _.noop }
            onChangeOptions={ _.noop }
            selectedComplectationsNames={ [ 'Комплектация первая' ] }
        />,
    );

    const description = wrapper.find('.CardGroupComplectationFilterItem__selected');

    expect(description.text()).toBe('1 комплектация');
});

it('должен вычислить подпись, когда передаем каунтер доп. опций', () => {
    const wrapper = shallow(
        <CardGroupComplectationFilterItem
            items={ items }
            values={ initialValues }
            onChangeComplectation={ _.noop }
            onChangeOptions={ _.noop }
            selectedAdditionalOptionsCount={ 3 }
            selectedComplectationsNames={ [] }
        />,
    );

    const description = wrapper.find('.CardGroupComplectationFilterItem__selected');

    expect(description.text()).toBe(`3${ nbsp }опции`);
});

it('должен вычислить подпись, когда выбрана комплектация и передаем каунтер доп. опций', () => {
    const wrapper = shallow(
        <CardGroupComplectationFilterItem
            items={ items }
            values={{
                ...initialValues,
                complectation_name: 'Комплектация первая',
            }}
            onChangeComplectation={ _.noop }
            onChangeOptions={ _.noop }
            selectedAdditionalOptionsCount={ 3 }
            selectedComplectationsNames={ [ 'Комплектация первая' ] }
        />,
    );

    const description = wrapper.find('.CardGroupComplectationFilterItem__selected');

    expect(description.text()).toBe('1 комплектация + 3');
});
