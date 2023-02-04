import { shallow } from 'enzyme';
import React from 'react';
const _ = require('lodash');

const listingState = require('autoru-frontend/mockData/state/listing');
const geo = require('auto-core/react/dataDomain/geo/mocks/geo.mock');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const subscriptions = require('auto-core/react/dataDomain/subscriptions/mocks/subscriptions.mock').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ListingSubscription = require('./ListingSubscription');

const { nbsp } = require('auto-core/react/lib/html-entities');

it('должен правильно посчитать текст для поиска без параметров', () => {
    const listing = _.cloneDeep(listingState);
    //убираем параметры
    listing.data.search_parameters = {};
    listing.data.paramsDescription = {
        paramsInfo: [],
        moreCount: 0,
    };
    const store = mockStore({ listing, geo, user: { data: {} } });
    const wrapper = shallow(
        <ListingSubscription/>,
        { context: { ...contextMock, store } },
    );

    expect(wrapper.dive().find('.ListingSubscription__text').text()).toBe('Все марки автомобилей');
});

it('должен правильно посчитать текст для поиска с малым количеством параметров (без moreCount)', () => {
    const listing = _.cloneDeep(listingState);
    //убираем ммм, потому что это неважно, а мокать хлебные крошки под ммм лень
    listing.data.search_parameters = {};
    const store = mockStore({ listing, geo, user: { data: {} } });
    const wrapper = shallow(
        <ListingSubscription/>,
        { context: { ...contextMock, store } },
    );

    expect(wrapper.dive().find('.ListingSubscription__text').text()).toBe('Все марки автомобилей, цена до 50 000 ₽');
});

it('должен правильно посчитать текст для поиска с большим количеством параметров (с moreCount)', () => {
    const listing = _.cloneDeep(listingState);
    //убираем ммм, потому что это неважно, а мокать хлебные крошки под ммм лень
    listing.data.search_parameters = {};
    listing.data.paramsDescription.moreCount = 5;
    const store = mockStore({ listing, geo, user: { data: {} } });
    const wrapper = shallow(
        <ListingSubscription/>,
        { context: { ...contextMock, store } },
    );

    expect(wrapper.dive().find('.ListingSubscription__text').text()).toBe(`Все марки автомобилей, цена до 50 000 ₽, +${ nbsp }5${ nbsp }параметров`);
});

it('правильный текст кнопки, если нет подписки', () => {
    const store = mockStore({ listing: listingState, geo, user: { data: {} } });
    const wrapper = shallow(
        <ListingSubscription/>,
        { context: { ...contextMock, store } },
    );
    expect(wrapper.dive().find('ButtonWithLoader').children().text()).toBe('Сохранить поиск');
});

it('правильный текст кнопки, если есть подписка', () => {
    const listing = _.cloneDeep(listingState);
    listing.data.search_parameters = subscriptions.data[0].data.params;
    const store = mockStore({ listing, geo, subscriptions, user: { data: {} } });
    const wrapper = shallow(
        <ListingSubscription/>,
        { context: { ...contextMock, store } },
    );
    expect(wrapper.dive().find('ButtonWithLoader').children().text()).toBe('Сохранён');
});
