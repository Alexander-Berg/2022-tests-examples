import React from 'react';
import { render, shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { TOfferCategory } from 'auto-core/types/proto/auto/api/api_offer_model';

import SalesEmptyListLk from './SalesEmptyListLk';

let store;

const config = {
    data: {
        url: '/my/cars/',
        host: 'auto.ru',
    },
};

it('должен правильно отрендерить пустой лк для незалогина', () => {
    store = mockStore({
        config: config,
        user: { data: { auth: false } },
    });
    const context = {
        link: () => 'form_url',
        metrika: { sendPageEvent: () => '' },
        store,
    };
    const wrapper = shallow(<SalesEmptyListLk category="cars"/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('в пустом лк для незалогина по умолчанию кнопка добавления ведёт на форму добавления легковых', () => {
    store = mockStore({
        config: config,
        user: { data: { auth: false } },
    });
    const contextMock = {
        link: (name: string, { category }: { category: TOfferCategory }) => 'form_url_to_' + category,
        metrika: { sendPageEvent: () => '' },
        store,
    };

    const Context = createContextProvider(contextMock);
    const tree = render(<Context><SalesEmptyListLk category="all"/></Context>);
    expect(tree.find('.SalesEmptyListLk__button_add').prop('href')).toEqual('form_url_to_cars');
});

it('должен правильно отрендерить пустой лк для залогина', () => {
    store = mockStore({
        config: config,
        user: { data: { auth: true } },
    });
    const context = {
        link: () => 'form_url',
        metrika: { sendPageEvent: () => '' },
        store,
    };
    const wrapper = shallow(<SalesEmptyListLk category="cars"/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отправить метрику на componentDidMount', () => {
    store = mockStore({
        config: config,
        user: { data: { auth: true } },
    });
    const context = {
        link: () => '',
        metrika: { sendPageEvent: jest.fn() },
        store,
    };
    shallow(<SalesEmptyListLk category="moto"/>, { context: context }).dive();
    expect(context.metrika.sendPageEvent.mock.calls).toHaveLength(1);
    expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'show', 'empty' ]);
});

it('должен правильно строить ссылку на форму добавления', () => {
    store = mockStore({
        config: config,
        user: { data: { auth: true } },
    });
    const context = {
        link: jest.fn(),
        metrika: { sendPageEvent: () => '' },
        store,
    };
    shallow(<SalesEmptyListLk category="moto"/>, { context: context }).dive();
    expect(context.link.mock.calls).toHaveLength(1);
    expect(context.link).toHaveBeenCalledWith('form', { category: 'moto', form_type: 'add', section: 'used' });
});
