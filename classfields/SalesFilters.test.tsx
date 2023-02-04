jest.mock('auto-core/react/dataDomain/sales/actions/filterOffers');
jest.mock('react-router-redux', () => ({
    replace: jest.fn(() => () => {}),
}));

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import reactRouterRedux from 'react-router-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import filterOffers from 'auto-core/react/dataDomain/sales/actions/filterOffers';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import salesMarkModelsMock from 'auto-core/react/dataDomain/salesMarkModels/mocks/markModels.mock';
import salesMock from 'auto-core/react/dataDomain/sales/mocks';

import SalesFilters from './SalesFilters';
import type { TAppState } from './SalesFilters';

let initialState: TAppState;

const filterOfferPromise = Promise.resolve();

const filterOffersMock = filterOffers as jest.MockedFunction<typeof filterOffers>;
filterOffersMock.mockImplementation(() => () => filterOfferPromise);

beforeEach(() => {
    initialState = {
        config: configStateMock.withPageParams({ category: 'cars' }).value(),
        salesMarkModels: salesMarkModelsMock,
        sales: salesMock.value(),
    };
});

describe('начальное состояние', () => {
    it('будет дефолтным, если нет параметров поиска в урле', () => {
        const page = shallowRenderComponent({ initialState });

        expect(page.find('SalesFilterMark').prop('value')).toEqual([]);
        expect(page.find('SalesFilterModel').prop('value')).toEqual([]);
        expect(page.find('PriceFromToFilter').prop('from')).toBeUndefined();
        expect(page.find('PriceFromToFilter').prop('to')).toBeUndefined();
    });

    it('предвыберет марку и модель, если в урле они есть', () => {
        initialState.sales = salesMock.withSearchParams({ mark_model: [ 'audi', 'bmw#x3' ] }).value();
        const page = shallowRenderComponent({ initialState });

        expect(page.find('SalesFilterMark').prop('value')).toEqual([ 'AUDI', 'BMW' ]);
        expect(page.find('SalesFilterModel').prop('value')).toEqual([ 'BMW#X3' ]);
        expect(page.find('PriceFromToFilter').prop('from')).toBeUndefined();
        expect(page.find('PriceFromToFilter').prop('to')).toBeUndefined();
    });

    it('оставит пустыми марку и модель, если в урле они отсуствуют', () => {
        initialState.sales = salesMock.withSearchParams({ price_from: 700000, price_to: 1000000 }).value();
        const page = shallowRenderComponent({ initialState });

        expect(page.find('SalesFilterMark').prop('value')).toEqual([]);
        expect(page.find('SalesFilterModel').prop('value')).toEqual([]);
        expect(page.find('PriceFromToFilter').prop('from')).toBe(700000);
        expect(page.find('PriceFromToFilter').prop('to')).toBe(1000000);
    });
});

describe('смена фильтра', () => {
    it('меняет марку', () => {
        const page = shallowRenderComponent({ initialState });
        const markFilter = page.find('SalesFilterMark');
        markFilter.simulate('change', [ 'AUDI' ], { name: 'marks' });

        const updatedMarkFilter = page.find('SalesFilterMark');
        expect(updatedMarkFilter.prop('value')).toEqual([ 'AUDI' ]);
    });

    it('меняет модель', () => {
        const page = shallowRenderComponent({ initialState });
        const modelFilter = page.find('SalesFilterModel');
        modelFilter.simulate('change', [ 'AUDI#A3' ], { name: 'models' });

        const updatedModelFilter = page.find('SalesFilterModel');
        expect(updatedModelFilter.prop('value')).toEqual([ 'AUDI#A3' ]);
    });

    it('меняет цены', () => {
        const page = shallowRenderComponent({ initialState });
        const priceFilter = page.find('PriceFromToFilter');

        priceFilter.simulate('change', 7, { name: 'price_from' });
        priceFilter.simulate('change', '', { name: 'price_to' });

        const updatedPriceFilter = page.find('PriceFromToFilter');

        expect(updatedPriceFilter.prop('from')).toBe(7);
        expect(updatedPriceFilter.prop('to')).toBeUndefined();
    });

    it('при сбросе марки, сбрасывает все выбранные модели', () => {
        const page = shallowRenderComponent({ initialState });
        const markFilter = page.find('SalesFilterMark');
        const modelFilter = page.find('SalesFilterModel');

        markFilter.simulate('change', [ 'AUDI' ], { name: 'marks' });
        modelFilter.simulate('change', [ 'AUDI#A3' ], { name: 'models' });
        markFilter.simulate('change', [], { name: 'marks' });

        const updatedModelFilter = page.find('SalesFilterModel');
        expect(updatedModelFilter.prop('value')).toEqual([]);
    });

    it('отправляет метрику при смене фильтра', () => {
        const page = shallowRenderComponent({ initialState });
        const markFilter = page.find('SalesFilterMark');
        const modelFilter = page.find('SalesFilterModel');
        const priceFilter = page.find('PriceFromToFilter');

        markFilter.simulate('change', [ 'AUDI' ], { name: 'marks' });
        modelFilter.simulate('change', [ 'AUDI#A3' ], { name: 'models' });
        priceFilter.simulate('change', 7, { name: 'price_from' });

        expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
    });
});

describe('при клике на кнопку найти', () => {
    it('вызовет экшен на поиск объявлений', () => {
        const page = shallowRenderComponent({ initialState });
        const markFilter = page.find('SalesFilterMark');
        const modelFilter = page.find('SalesFilterModel');

        markFilter.simulate('change', [ 'AUDI' ], { name: 'marks' });
        markFilter.simulate('change', [ 'BMW' ], { name: 'marks' });
        modelFilter.simulate('change', [ 'AUDI#A3' ], { name: 'models' });

        const findButton = page.find('.SalesFilters__submitButton');
        findButton.simulate('click');

        expect(filterOffersMock).toHaveBeenCalledTimes(1);
        expect(filterOffersMock).toHaveBeenCalledWith({ mark_model: [ 'AUDI#A3', 'BMW' ] });
    });

    it('поменяет параметры в урле', () => {
        const page = shallowRenderComponent({ initialState });
        const markFilter = page.find('SalesFilterMark');
        markFilter.simulate('change', [ 'AUDI' ], { name: 'marks' });

        const findButton = page.find('.SalesFilters__submitButton');
        findButton.simulate('click');

        return filterOfferPromise
            .then(() => {
                expect(reactRouterRedux.replace).toHaveBeenCalledTimes(1);
                expect(reactRouterRedux.replace).toHaveBeenCalledWith('link/sales/?category=cars&mark_model=AUDI');
            });
    });

    it('ничего не будет делать, если ни один фильтр не выбран', () => {
        const page = shallowRenderComponent({ initialState });
        const findButton = page.find('.SalesFilters__submitButton');
        findButton.simulate('click');

        expect(filterOffersMock).toHaveBeenCalledTimes(0);
    });

    it('отправляет метрику при успехе', () => {
        const page = shallowRenderComponent({ initialState });
        const markFilter = page.find('SalesFilterMark');
        const priceFilter = page.find('PriceFromToFilter');

        markFilter.simulate('change', [ 'AUDI' ], { name: 'marks' });
        priceFilter.simulate('change', 7, { name: 'price_to' });

        const findButton = page.find('.SalesFilters__submitButton');
        findButton.simulate('click');

        return filterOfferPromise
            .then(() => {
                expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
            });
    });
});

describe('при клике на кнопку сбросить', () => {
    let page: ShallowWrapper;

    beforeEach(() => {
        initialState.sales = salesMock.withSearchParams({ mark_model: [ 'audi', 'bmw#x3' ] }).value();
        page = shallowRenderComponent({ initialState });
        const clearButton = page.find('.SalesFilters__clearButton');
        clearButton.simulate('click');
    });

    it('вызовет экшен на сброс параметров', () => {
        expect(filterOffersMock).toHaveBeenCalledTimes(1);
        expect(filterOffersMock).toHaveBeenCalledWith({ });
    });

    it('сбросит параметры в урле', () => {
        return filterOfferPromise
            .then(() => {
                expect(reactRouterRedux.replace).toHaveBeenCalledTimes(1);
                expect(reactRouterRedux.replace).toHaveBeenCalledWith('link/sales/?category=cars');
            });
    });

    it('установит исходный стейт', () => {
        return filterOfferPromise
            .then(() => {
                expect(page.find('SalesFilterMark').prop('value')).toEqual([]);
                expect(page.find('SalesFilterModel').prop('value')).toEqual([]);
                expect(page.find('PriceFromToFilter').prop('from')).toBeUndefined();
                expect(page.find('PriceFromToFilter').prop('to')).toBeUndefined();
            });
    });

    it('отправит метрику', () => {
        return filterOfferPromise
            .then(() => {
                expect(contextMock.metrika.sendPageEvent.mock.calls).toMatchSnapshot();
            });
    });
});

function shallowRenderComponent({ initialState }: { initialState: TAppState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <SalesFilters/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
