/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import compareMock from 'autoru-frontend/mockData/state/compare.mock';

import type { TStateCompare } from 'auto-core/react/dataDomain/compare/TStateCompare';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import type { TStateComparison } from 'www-desktop-compare/react/dataDomain/common/TStateComparison';

import Page, { CONTENT_TYPE } from './Page';

interface State {
    compare: TStateCompare;
    comparison: TStateComparison;
    config: StateConfig;
}

let store: ThunkMockStore<State>;
beforeEach(() => {
    store = mockStore({
        compare: compareMock,
        comparison: {
            status: '',
            offers: [],
            models: [],
        },
        config: configStateMock.value(),
    });
});

describe('страница сравнения офферов', () => {
    const params = { content: CONTENT_TYPE.OFFERS };

    it('должен правильно отрендерить заголовок отрендерить', () => {
        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('.Page__title').text()).toEqual('Сравнение объявлений');
    });

    it('если ещё не полученны данные, рисует лоадер', () => {
        store = mockStore({
            compare: compareMock,
            comparison: {
                status: 'PENDING',
                offers: [],
                models: [],
            },
            config: configStateMock.value(),
        });
        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('Loader')).toExist();
    });

    it('если нет офферов, рисует заглушку', () => {
        store = mockStore({
            compare: compareMock,
            comparison: {
                status: 'SUCCESS',
                offers: [],
                models: [],
            },
            config: configStateMock.value(),
        });
        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('EmptyComparison')).toExist();
    });

    it('переход на страницу сравнения моделей', () => {
        const originalWindowLocation = global.location;
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        delete global.location;
        global.location = { ...originalWindowLocation, href: '' };

        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('RadioGroup').props().value).toEqual(CONTENT_TYPE.OFFERS);
        wrapper.find('RadioGroup').simulate('change', CONTENT_TYPE.MODELS);
        expect(global.location.href).toEqual('link/compare/?content=models');

        global.location = originalWindowLocation;
    });
});

describe('страница сравнения моделей', () => {
    const params = { content: CONTENT_TYPE.MODELS };

    it('должен правильно отрендерить заголовок отрендерить', () => {
        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('.Page__title').text()).toEqual('Сравнение моделей');
    });

    it('должен подключить шторку для добавления моделей', () => {
        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('Connect(FormToAddModel)')).toExist();
    });

    it('если ещё не полученны данные, рисует лоадер', () => {
        store = mockStore({
            compare: compareMock,
            comparison: {
                status: 'PENDING',
                offers: [],
                models: [],
            },
            config: configStateMock.value(),
        });
        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('Loader')).toExist();
    });

    it('если нет моделей, рисует заглушку', () => {
        store = mockStore({
            compare: compareMock,
            comparison: {
                status: 'SUCCESS',
                offers: [],
                models: [],
            },
            config: configStateMock.value(),
        });
        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('EmptyComparison')).toExist();
    });

    it('переход на страницу сравнения офферов', () => {
        const originalWindowLocation = global.location;
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        delete global.location;
        global.location = { ...originalWindowLocation, href: '' };

        const wrapper = shallow(
            <Page params={ params }/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(wrapper.find('RadioGroup').props().value).toEqual(CONTENT_TYPE.MODELS);
        wrapper.find('RadioGroup').simulate('change', CONTENT_TYPE.OFFERS);
        expect(global.location.href).toEqual('link/compare/?content=offers');

        global.location = originalWindowLocation;
    });
});
