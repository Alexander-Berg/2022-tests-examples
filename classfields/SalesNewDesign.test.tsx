/**
 * @jest-environment jsdom
 */
jest.mock('auto-core/react/lib/cookie');

import 'jest-enzyme';

import type { MockStoreEnhanced } from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import userMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import userWithoutAuthMock from 'auto-core/react/dataDomain/user/mocks/withoutAuth.mock';
import configMock from 'auto-core/react/dataDomain/config/mocks/config';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import cookie from 'auto-core/react/lib/cookie';

const setCookie = cookie.set as jest.MockedFunction<typeof cookie.set>;
const removeCookie = cookie.remove as jest.MockedFunction<typeof cookie.remove>;

import SalesNewDesign from './SalesNewDesign';
import type { OwnProps, TAppState } from './SalesNewDesign';

const defaultState: TAppState = {
    sales: {
        items: [ offerMock ],
        pagination: {
            total_offers_count: 0,
            page: 0,
            total_page_count: 1,
            page_size: 10,
        },
        searchParams: {},
        isLoading: false,
        state: {},
    },
    config: { ...configMock, data: { ...configMock.data, pageParams: {} } },
    user: userMock,
    offerStats: {},
    cookies: {},
    bunker: {},
    salesMarkModels: {
        status: 'SUCCESS',
        mark_models: [],
    },
    drafts: { data: [] },
    c2bApplications: {
        applications: [],
        pagination: {
            page_num: 0,
            page_size: 0,
            total_count: 0,
            total_page_count: 0,
        },
    },
};

const defaultProps: OwnProps = { params: { category: 'all' } };

describe('SalesNewDesign', () => {
    it('рендерит фильтры, если офферов больше 5', () => {
        const state = {
            ...defaultState,
            sales: {
                ...defaultState.sales,
                items: [ offerMock, offerMock, offerMock, offerMock, offerMock, offerMock ],
            },
        };
        const wrapper = renderComponent({ store: mockStore(state) });

        const filters = wrapper.find('SalesFiltersNewDesign');

        expect(filters).toExist();
    });

    it('не рендерит фильтры, если офферов мало', () => {
        const wrapper = renderComponent({ store: mockStore(defaultState) });

        const filters = wrapper.find('SalesFiltersNewDesign');

        expect(filters).not.toExist();
    });

    it('не показывает вкладку аукциона, если нет заявок для аукциона', () => {
        const wrapper = renderComponent({ store: mockStore(defaultState) });

        const radios = wrapper.find('Radio');

        expect(radios).toHaveLength(4);
    });

    it('показывает боковое меню, если пользователь авторизован', () => {
        const wrapper = renderComponent({ store: mockStore(defaultState) });

        const promo = wrapper.find('.SalesNewDesign__promo');

        expect(promo).toExist();
    });

    it('не показывает боковое меню, если пользователь не авторизован', () => {
        const state = { ...defaultState, user: userWithoutAuthMock };
        const wrapper = renderComponent({ store: mockStore(state) });

        const promo = wrapper.find('.SalesNewDesign__promo');

        expect(promo).not.toExist();
    });
});

describe('SalesNewDesign открытие и закрытие статы', () => {
    const initialState = {
        ...defaultState,
        sales: {
            ...defaultState.sales,
            items: [ offerMock, offerMock, offerMock, offerMock, offerMock, offerMock ],
        },
    };

    it('выставляет куку sales-stats-hidden и отправляет экшн при деактивации тоггла Раскрытия статы', () => {
        const store = mockStore(initialState);
        const wrapper = renderComponent({ store });

        wrapper.find('SalesFiltersNewDesign').simulate('expandStatsChange', false);

        expect(setCookie).toHaveBeenCalled();
        expect(store.getActions()).toEqual([
            {
                type: 'COOKIES_CHANGE',
            },
            {
                type: 'SALES_CLOSE_ALL_OFFERS_STATS',
            },
        ]);
    });

    it('удаляет куку sales-stats-hidden и отправляет экшн при активации тоггла Раскрытия статы', () => {
        const store = mockStore({
            ...initialState,
            cookies: {
                ...initialState.cookies,
                'sales-stats-hidden': 'true',
            },
        });

        const wrapper = renderComponent({ store });
        wrapper.find('SalesFiltersNewDesign').simulate('expandStatsChange', true);

        expect(removeCookie).toHaveBeenCalled();
        expect(store.getActions()).toEqual([
            {
                type: 'COOKIES_CHANGE',
            },
            {
                type: 'SALES_EXPAND_ALL_OFFERS_STATS',
            },
        ]);
    });

    it('после трех тогглов статистики прокинет проп на показ тултипа в SalesFiltersNewDesign и выставит куку если ее не было', () => {
        const store = mockStore({
            ...initialState,
            offerStats: {},
        });

        const wrapper = renderComponent({ store });

        const statsToggle = wrapper.find('ConnectSocialAccountHoc(SalesItemNewDesign)').at(0).dive().dive()
            .find({ className: 'SalesItemNewDesign__expandToggle' });

        statsToggle.simulate('click');
        statsToggle.simulate('click');
        statsToggle.simulate('click');

        wrapper.update();

        const filters = wrapper.find('SalesFiltersNewDesign');

        expect(filters).toHaveProp('shouldShowExpandTooltip', true);
        expect(setCookie).toHaveBeenCalledWith('sales-stats-expand-tooltip-counter', '1', { expires: 60 });
    });

    it('после трех тогглов статистики прокинет проп на показ тултипа в SalesFiltersNewDesign и увеличит значение куки', () => {
        const store = mockStore({
            ...initialState,
            offerStats: {},
            cookies: {
                'sales-stats-expand-tooltip-counter': '1',
            },
        });

        const wrapper = renderComponent({ store });

        const statsToggle = wrapper.find('ConnectSocialAccountHoc(SalesItemNewDesign)').at(0).dive().dive()
            .find({ className: 'SalesItemNewDesign__expandToggle' });

        statsToggle.simulate('click');
        statsToggle.simulate('click');
        statsToggle.simulate('click');

        wrapper.update();

        const filters = wrapper.find('SalesFiltersNewDesign');

        expect(filters).toHaveProp('shouldShowExpandTooltip', true);
        expect(setCookie).toHaveBeenCalledWith('sales-stats-expand-tooltip-counter', '2', { expires: 60 });
    });

    it('после трех тогглов статистики не будет прокидывать проп и менять куку, если она уже равна 3', () => {
        const store = mockStore({
            ...initialState,
            offerStats: {},
            cookies: {
                'sales-stats-expand-tooltip-counter': '3',
            },
        });

        const wrapper = renderComponent({ store });

        const statsToggle = wrapper.find('ConnectSocialAccountHoc(SalesItemNewDesign)').at(0).dive().dive()
            .find({ className: 'SalesItemNewDesign__expandToggle' });

        statsToggle.simulate('click');
        statsToggle.simulate('click');
        statsToggle.simulate('click');

        wrapper.update();

        const filters = wrapper.find('SalesFiltersNewDesign');

        expect(filters).toHaveProp('shouldShowExpandTooltip', false);
        expect(setCookie).not.toHaveBeenCalled();
    });
});

function renderComponent(params?: { props?: OwnProps; store?: MockStoreEnhanced }) {
    const { store = mockStore(defaultState), props = defaultProps } = params || {};
    const Context = createContextProvider(contextMock);

    const page = shallow(
        <Context>
            <Provider store={ store }>
                <SalesNewDesign { ...props }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    return page;
}
