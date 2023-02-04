import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { CompareEntity } from '@vertis/schema-registry/ts-types-snake/auto/api/compare_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import ComparisonCell from './ComparisonCell';

const props = {
    groupId: 'Объявление',
    itemId: '1092839022-c45a84ba',
    urlToFreeReport: 'https://frontend.alivander.dev.vertis.yandex.net/cars/used/sale/toyota/avensis/1092839022-c45a84ba/?action=showVinReport',
};

it('должен отрендерить заглушку, если вместо photo_url пришла пустая строка', () => {
    const entity = {
        id: 'saloon-front',
        name: 'Салон спереди',
        photo_url: '',
    } as CompareEntity;
    const wrapper = shallow(
        <ComparisonCell entity={ entity } { ...props }/>,
        { context: { ...contextMock, store: mockStore({}) } },
    );

    expect(wrapper.find('.ComparisonCell__image_unknown')).toExist();
});

it('вместо прочерка с бэка в текстовом поле должен подставить иконку', () => {
    const entity = {
        id: 'id',
        name: 'name',
        string_value: '—',
    } as CompareEntity;
    const wrapper = shallow(
        <ComparisonCell entity={ entity } { ...props }/>,
        { context: { ...contextMock, store: mockStore({}) } },
    );

    expect(wrapper.find('.ComparisonCell__icon_minus')).toExist();
});

it('для подозрительных данных должен нарисовать иконку-warning', () => {
    const entity = {
        id: 'id',
        name: 'name',
        string_value: 'value',
        attention: true,
    } as CompareEntity;
    const wrapper = shallow(
        <ComparisonCell entity={ entity } { ...props }/>,
        { context: { ...contextMock, store: mockStore({}) } },
    );

    expect(wrapper.find('.ComparisonCell__icon_warning')).toExist();
});
