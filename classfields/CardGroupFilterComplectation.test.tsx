import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import complectationFilterData from 'auto-core/react/dataDomain/cardGroup/mocks/complectationFilterData.mock';
import type { TCardGroupFiltersValues } from 'auto-core/react/dataDomain/cardGroup/types';
import { nbsp } from 'auto-core/react/lib/html-entities';

import CardGroupFilterComplectation from './CardGroupFilterComplectation';

let filterValues: TCardGroupFiltersValues;

beforeEach(() => {
    filterValues = {
        catalog_equipment: [],
        color: [],
        gear_type: [],
        tech_param_id: [ '2' ],
        transmission: [ '1' ],
        search_tag: [],
    };
});

it('должен открыть модал с фильтром по клику на тег', () => {
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [] }
            offersCount={ 10 }
            onChangeOptions={ () => {} }
            onChangeComplectation={ () => {} }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
        />,
    );
    expect(wrapper.find('Modal')).not.toExist();
    wrapper.find('Tag').simulate('click');
    expect(wrapper.find('Modal')).toExist();
});

it('должен вызвать onChangeOptions при смене опций', () => {
    const onChangeOptions = jest.fn();
    const onChangeComplectation = jest.fn();
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [] }
            offersCount={ 10 }
            onChangeOptions={ onChangeOptions }
            onChangeComplectation={ onChangeComplectation }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
        />,
    );
    wrapper.find('Tag').simulate('click');
    wrapper.find('CardGroupFilterComplectationModalContent').dive()
        .find('CardGroupFilterComplectationAdditionalOptions').simulate('change', [ 'some_option' ]);
    expect(onChangeOptions).toHaveBeenCalledWith([ 'some_option' ]);
    expect(onChangeComplectation).not.toHaveBeenCalled();
});

it('должен вызвать onChangeComplectation при смене комплектации', () => {
    const onChangeOptions = jest.fn();
    const onChangeComplectation = jest.fn();
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [] }
            offersCount={ 10 }
            onChangeOptions={ onChangeOptions }
            onChangeComplectation={ onChangeComplectation }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
        />,
    );
    wrapper.find('Tag').simulate('click');
    wrapper.find('CardGroupFilterComplectationModalContent').dive()
        .find('CardGroupFilterComplectationItem').at(2).simulate('click', 'some_complectation');
    expect(onChangeComplectation).toHaveBeenCalledWith('some_complectation');
    expect(onChangeOptions).not.toHaveBeenCalled();
});

it('должен сбросить изменения при закрытии модала', () => {
    const onChangeOptions = jest.fn();
    const onChangeComplectation = jest.fn();
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [] }
            offersCount={ 10 }
            onChangeOptions={ onChangeOptions }
            onChangeComplectation={ onChangeComplectation }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
        />,
    );
    wrapper.find('Tag').simulate('click');
    wrapper.find('CardGroupFilterComplectationModalContent').dive()
        .find('CardGroupFilterComplectationAdditionalOptions').simulate('change', [ 'some_option' ]);
    wrapper.find('CardGroupFilterComplectationModalContent').dive()
        .find('CardGroupFilterComplectationItem').at(2).simulate('click', 'some_complectation');
    wrapper.find('Modal').simulate('requestHide');
    expect(onChangeOptions.mock.calls[1]).toEqual([ [] ]);
});

it('должен вычислить текст в теге, когда нет значения', () => {
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [] }
            selectedOptions={ [] }
            offersCount={ 10 }
            onChangeOptions={ () => {} }
            onChangeComplectation={ () => {} }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
        />,
    );
    expect(wrapper.find('Tag').children().text()).toBe('Комплектация');
});

it('должен вычислить текст в теге, когда выбрана комплектация', () => {
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [] }
            offersCount={ 10 }
            onChangeOptions={ () => {} }
            onChangeComplectation={ () => {} }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
        />,
    );
    expect(wrapper.find('Tag').children().at(0).text()).toBe('Комплектация 1');
});

it('должен вычислить текст в теге, когда выбраны только базовые опции', () => {
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [] }
            selectedOptions={ [ 'option1', 'option2,option3' ] }
            offersCount={ 10 }
            onChangeOptions={ () => {} }
            onChangeComplectation={ () => {} }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
        />,
    );
    expect(wrapper.find('Tag').children().at(0).text()).toBe('Комплектация');
});

it('должен вычислить текст в теге, когда передаем каунтер доп. опций', () => {
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [] }
            selectedOptions={ [ 'option1', 'option2,option3' ] }
            offersCount={ 10 }
            onChangeOptions={ () => {} }
            onChangeComplectation={ () => {} }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
            selectedAdditionalOptionsCount={ 3 }
        />,
    );
    expect(wrapper.find('Tag').children().at(0).text()).toBe(`3${ nbsp }опции`);
});

it('должен вычислить текст в теге, когда выбрана комплектация и передается каунтер доп. опций', () => {
    const wrapper = shallow(
        <CardGroupFilterComplectation
            items={ complectationFilterData.COMPLECTATION_ITEMS }
            additionalOptions={ complectationFilterData.ADDITIONAL_OPTIONS }
            additionalOptionsGrouped={ complectationFilterData.ADDITIONAL_OPTIONS_GROUPED }
            availableOptionsGrouped={ complectationFilterData.AVAILABLE_OPTIONS_GROUPED }
            selectedComplectationsNames={ [ 'Комплектация 1' ] }
            selectedOptions={ [ 'option1', 'option2' ] }
            offersCount={ 10 }
            onChangeOptions={ () => {} }
            onChangeComplectation={ () => {} }
            onSubmit={ () => {} }
            onReset={ () => {} }
            filterValues={ filterValues }
            selectedAdditionalOptionsCount={ 2 }
        />,
    );
    expect(wrapper.find('Tag').children().at(0).text()).toBe('Комплектация 1 + 2');
});
