jest.mock('../../lib/request');
jest.mock('../../../lib/metrika', () => {
    return {
        send_page_event: jest.fn(),
    };
});

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import configMock from '../../../mocks/state/config.mock';
import request from '../../lib/request';

import type { OwnProps } from './SafeDealWidget';
import SafeDealWidget from './SafeDealWidget';

const request_mock = request as jest.MockedFunction<typeof request>;
const request_promise_success = Promise.resolve();
request_mock.mockReturnValue(request_promise_success);

const mockStore = configureStore([ thunk ]);

const OFFER_ID = '1112345';
const CATEGORY = 'cars';
const DEAL_ID = '0987654321';
const CHAT_ID = 'ID1245';
const PRICE = '5555555';

const default_props = {
    time: '23:23',
    on_close: jest.fn(),
    on_request: jest.fn(),
    on_cancel_request: jest.fn(),
    is_seller: false,
    is_mobile: false,
};

it('должен отрисовать модал при клике на кнопку "Отправить запрос"', () => {
    const tree = renderComponent(default_props).dive().dive();

    tree.find('.SafeDealWidget__button').simulate('click');

    expect(tree.state('is_modal_visible')).toBe(true);
});

it('должен отправить запрос на сделку от покупателя', () => {
    const expectedRequestItem = {
        params: {
            category: CATEGORY.toUpperCase(),
            offer_id: OFFER_ID,
            price: PRICE,
        },
        path: 'safe_deal/create_safe_deal',
    };
    const tree = renderComponent(default_props).dive().dive();

    tree.find('.SafeDealWidget__button').simulate('click');
    tree.find('SafeDealCreateModal')
        .dive()
        .find('.SafeDealCreateModal__button')
        .simulate('click');

    expect(request).toHaveBeenCalledWith(expectedRequestItem);
});

it('должен отправить запрос на сделку от продавца', () => {
    const expectedRequestItem = {
        params: {
            category: CATEGORY.toUpperCase(),
            offer_id: OFFER_ID,
            room_id: CHAT_ID,
            price: PRICE,
        },
        path: 'safe_deal/create_safe_deal',
    };
    const tree = renderComponent({ ...default_props, is_seller: true }).dive().dive();

    tree.find('.SafeDealWidget__button').simulate('click');

    expect(request).toHaveBeenCalledWith(expectedRequestItem);
});

it('должен отрисовать ссылку на условия сервиса', () => {
    const EXPECTED_LINK = 'https://yandex.ru/legal/autoru_escrow/';
    const tree = renderComponent(default_props).dive().dive();

    const link = tree.find('.SafeDealWidget__termsOfUse_link');

    expect(link.prop('href')).toBe(EXPECTED_LINK);
});

it('должен отменять запрос на сделку от покупателя', () => {
    const expectedRequestItem = {
        params: {
            by_party_type: 'by_buyer',
            deal_id: DEAL_ID,
        },
        path: 'safe_deal/cancel_safe_deal',
    };
    const tree = renderComponent(default_props).dive().dive();
    tree.setState({
        has_error: false,
        is_deal_created: true,
        is_request_pending: false,
        deal_id: DEAL_ID,
    });

    tree.find('.SafeDealWidget__button_cancel').simulate('click');

    expect(request).toHaveBeenCalledWith(expectedRequestItem);
});

it('должен отменять запрос на сделку от продавца', () => {
    const expectedRequestItem = {
        params: {
            by_party_type: 'by_seller',
            deal_id: DEAL_ID,
        },
        path: 'safe_deal/cancel_safe_deal',
    };
    const tree = renderComponent({ ...default_props, is_seller: true }).dive().dive();
    tree.setState({
        has_error: false,
        is_deal_created: true,
        is_request_pending: false,
        deal_id: DEAL_ID,
    });

    tree.find('.SafeDealWidget__button_cancel').simulate('click');

    expect(request).toHaveBeenCalledWith(expectedRequestItem);
});

it('должен отрисовать ссылку на страницу списка сделок', () => {
    const EXPECTED_LINK = 'https://auto.ru/my/deals/';
    const tree = renderComponent(default_props).dive().dive();
    tree.setState({
        has_error: false,
        is_deal_created: true,
        is_request_pending: false,
        deal_id: DEAL_ID,
    });

    const link = tree.find('.SafeDealWidget__button');

    expect(link.prop('url')).toBe(EXPECTED_LINK);
});

function renderComponent(props: OwnProps) {
    const store = mockStore({
        config: configMock.value(),
        chat_id: CHAT_ID,
        chat_list: [ { id: CHAT_ID, source: { id: OFFER_ID, category: CATEGORY }, subject: { price: { price: PRICE } } } ],
    });

    return shallow(
        <Provider store={ store }>
            <SafeDealWidget { ...props }/>
        </Provider>,
    );
}
