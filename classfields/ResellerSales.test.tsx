jest.mock('auto-core/react/lib/cookie');

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import cookie from 'auto-core/react/lib/cookie';

import ResellerSales from './ResellerSales';

const setCookie = cookie.set as jest.MockedFunction<typeof cookie.set>;
const removeCookie = cookie.get as jest.MockedFunction<typeof cookie.get>;

setCookie.mockImplementation(() => {});
removeCookie.mockImplementation(() => ({}));

const initialState = {
    sales: {
        items: [],
        pagination: { page: 1, total_page_count: 2 },
        state: {},
        isLoading: false,
    },
    config: { data: { pageParams: {} } },
    salesMarkModels: { mark_models: [] },
    cookies: { 'has-user-seen-new-reseller-cabinet': 'true' },
    user: { data: {} },
};
const store = mockStore(initialState);

it('выставляет куку reseller-sales-stats-hidden и отправляет экшн при деактивации тоггла Раскрытия статы', () => {
    const tree = shallow(
        <Provider store={ store }>
            <ResellerSales/>
        </Provider>,
        { context: { ...contextMock } },
    ).dive().dive();

    tree.find('SalesFiltersNewDesign').simulate('expandStatsChange', false);

    expect(setCookie).toHaveBeenCalled();
    expect(store.getActions()).toEqual([
        {
            payload: {},
            type: 'COOKIES_CHANGE',
        },
        {
            type: 'SALES_CLOSE_ALL_OFFERS_STATS',
        },
    ]);
});

it('удаляет куку reseller-sales-stats-hidden и отправляет экшн при активации тоггла Раскрытия статы', () => {
    const store = mockStore({
        ...initialState,
        cookies: {
            ...initialState.cookies,
            'reseller-sales-stats-hidden': 'true',
        },
    });

    const tree = shallow(
        <Provider store={ store }>
            <ResellerSales/>
        </Provider>,
        { context: { ...contextMock } },
    ).dive().dive();

    tree.find('SalesFiltersNewDesign').simulate('expandStatsChange', true);

    expect(removeCookie).toHaveBeenCalled();
    expect(store.getActions()).toEqual([
        {
            payload: {},
            type: 'COOKIES_CHANGE',
        },
        {
            type: 'SALES_EXPAND_ALL_OFFERS_STATS',
        },
    ]);
});

it('выставляет куку has-user-seen-new-reseller-cabinet если ее не было', () => {
    const store = mockStore({
        ...initialState,
        cookies: {},
    });

    shallow(
        <Provider store={ store }>
            <ResellerSales/>
        </Provider>,
        { context: { ...contextMock } },
    ).dive().dive();

    expect(setCookie).toHaveBeenCalled();
    expect(store.getActions()).toEqual([
        {
            payload: {},
            type: 'COOKIES_CHANGE',
        },
    ]);
});

it('после трех тогглов статистики прокинет проп на показ тултипа в SalesFiltersNewDesign и выставит куку если ее не было', () => {
    const store = mockStore({
        ...initialState,
        sales: {
            ...initialState.sales,
            items: [ offerMock ],
        },
        offerStats: {},
        cookies: {
            'has-user-seen-new-reseller-cabinet': 'true',
        },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <ResellerSales/>
        </Provider>,
        { context: { ...contextMock } },
    ).dive().dive();

    const statsToggle = wrapper.find('ResellerSalesItem').dive().find('ResellerSalesItemMainInfo').dive().find('.ResellerSalesItemMainInfo__statsToggle');

    statsToggle.simulate('click');
    statsToggle.simulate('click');
    statsToggle.simulate('click');

    wrapper.update();

    const filters = wrapper.find('SalesFiltersNewDesign');

    expect(filters).toHaveProp('shouldShowExpandTooltip', true);
    expect(setCookie).toHaveBeenCalledWith('reseller-sales-stats-expand-tooltip-counter', '1', { expires: 60 });
});

it('после трех тогглов статистики прокинет проп на показ тултипа в SalesFiltersNewDesign и увеличит значение куки', () => {
    const store = mockStore({
        ...initialState,
        sales: {
            ...initialState.sales,
            items: [ offerMock ],
        },
        offerStats: {},
        cookies: {
            'has-user-seen-new-reseller-cabinet': 'true',
            'reseller-sales-stats-expand-tooltip-counter': '1',
        },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <ResellerSales/>
        </Provider>,
        { context: { ...contextMock } },
    ).dive().dive();

    const statsToggle = wrapper.find('ResellerSalesItem').dive().find('ResellerSalesItemMainInfo').dive().find('.ResellerSalesItemMainInfo__statsToggle');

    statsToggle.simulate('click');
    statsToggle.simulate('click');
    statsToggle.simulate('click');

    wrapper.update();

    const filters = wrapper.find('SalesFiltersNewDesign');

    expect(filters).toHaveProp('shouldShowExpandTooltip', true);
    expect(setCookie).toHaveBeenCalledWith('reseller-sales-stats-expand-tooltip-counter', '2', { expires: 60 });
});

it('после трех тогглов статистики не будет прокидывать проп и менять куку, если она уже равна 3', () => {
    const store = mockStore({
        ...initialState,
        sales: {
            ...initialState.sales,
            items: [ offerMock ],
        },
        offerStats: {},
        cookies: {
            'has-user-seen-new-reseller-cabinet': 'true',
            'reseller-sales-stats-expand-tooltip-counter': '3',
        },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <ResellerSales/>
        </Provider>,
        { context: { ...contextMock } },
    ).dive().dive();

    const statsToggle = wrapper.find('ResellerSalesItem').dive().find('ResellerSalesItemMainInfo').dive().find('.ResellerSalesItemMainInfo__statsToggle');

    statsToggle.simulate('click');
    statsToggle.simulate('click');
    statsToggle.simulate('click');

    wrapper.update();

    const filters = wrapper.find('SalesFiltersNewDesign');

    expect(filters).toHaveProp('shouldShowExpandTooltip', false);
    expect(setCookie).not.toHaveBeenCalled();
});
