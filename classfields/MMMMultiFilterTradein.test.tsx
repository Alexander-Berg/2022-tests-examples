import { noop } from 'lodash';
import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';

import type { StateBreadcrumbsPublicApi } from 'auto-core/react/dataDomain/breadcrumbsPublicApi/types';

import type { Props, State } from './MMMMultiFilterTradein';
import MMMMultiFilter from './MMMMultiFilterTradein';

let wrapper: ShallowWrapper<Props, State>;
let store: {
    breadcrumbsPublicApi: StateBreadcrumbsPublicApi;
};

it('не должно быть кнопки "еще", если нет марки-модели', () => {
    wrapper = shallow(
        <MMMMultiFilter
            maxMmmCount={ 4 }
            mmmInfo={ [ {} ] }
            onChange={ noop }
            onSubmit={ noop }
            withGenerations
        />, { context: { store: mockStore(store) } },
    ).dive();
    expect(wrapper.find('.MMMMultiFilterTradein__add').isEmptyRender()).toBe(true);
});

it('должна быть кнопка "еще", если есть марка и модель', () => {
    wrapper = shallow(
        <MMMMultiFilter
            maxMmmCount={ 4 }
            mmmInfo={ [
                { mark: 'audi', models: [ { id: 'a1', generations: [], nameplates: [] } ] },
            ] }
            onChange={ noop }
            onSubmit={ noop }
            withGenerations
        />, { context: { store: mockStore(store) } },
    ).dive();
    expect(wrapper.find('.MMMMultiFilterTradein__add').isEmptyRender()).toBe(false);
});

it('не должно быть кнопки "еще", если марок моделей максимальное количество', () => {
    wrapper = shallow(
        <MMMMultiFilter
            maxMmmCount={ 2 }
            mmmInfo={ [
                { mark: 'audi', models: [ { id: 'a1', generations: [], nameplates: [] } ] },
                { mark: 'audi', models: [ { id: 'a2', generations: [], nameplates: [] } ] } ] }
            onChange={ noop }
            onSubmit={ noop }
            withGenerations
        />, { context: { store: mockStore(store) } },
    ).dive();
    expect(wrapper.find('.MMMMultiFilterTradein__add').isEmptyRender()).toBe(true);
});
