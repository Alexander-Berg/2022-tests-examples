jest.mock('www-mobile/react/dataDomain/carTinder/actions/setSearchParams', () => jest.fn(() => {}));

import React from 'react';
import { Provider } from 'react-redux';
import userEvent from '@testing-library/user-event';
import { render, act, screen, fireEvent } from '@testing-library/react';
import MockDate from 'mockdate';

import flushPromises from 'autoru-frontend/jest/unit/flushPromises';
import MockInputComponentGenerator, { runEventPropCallbackOn } from 'autoru-frontend/jest/unit/MockInputComponentGenerator';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { findByChildrenText } from 'autoru-frontend/jest/unit/queryHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import breadcrumbsPublicApiMock from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';
import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TO_KM_AGE_MAX, DEFAULT_FROM_YEAR, DEFAULT_RADIUS_MAX } from 'auto-core/data/car-tinder/car_tinder';
import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import setSearchParams from 'www-mobile/react/dataDomain/carTinder/actions/setSearchParams';

import '@testing-library/jest-dom';
import CarTinderFilters from './CarTinderFilters';

type HandlerTypes = 'change';
jest.mock('./components/CarTinderMMMFilter/CarTinderMMMFilter', () => MockInputComponentGenerator<HandlerTypes>('CarTinderMMMFilter'));
const onLoad = jest.fn(() => Promise.resolve());
const onRequestHide = jest.fn(() => Promise.resolve());
const ContextProvider = createContextProvider(contextMock);

const searchParameters = {
    catalog_filter: [
        {
            mark: 'AUDI',
            model: 'A5',
        },
    ],
    body_type_group: [ 'HATCHBACK_5_DOORS' ],
    section: 'all',
    category: 'cars',
    price_from: 1000,
    price_to: 2000,
    year_from: 1980,
    year_to: 2000,
    km_age_to: 10000,
} as TSearchParameters;

const currentOffer = cloneOfferWithHelpers(offer)
    .withSellerLocation({ ...offer.seller?.location, geobase_id: '10' })
    .withSaleId('111-333')
    .value();

const INITIAL_STORE = {
    carTinder: {
        currentOffer,
        filteredOffersCount: 100,
        search_parameters: searchParameters,
    },
    breadcrumbsPublicApi: breadcrumbsPublicApiMock,
    geo: geoMock,
};

beforeAll(() => {
    MockDate.set('2020-04-20');
});

it('при выборе марки меняет данные фильтра', async() => {

    renderComponent();

    const mmmMulitFilter = screen.getByLabelText('CarTinderMMMFilter');

    const values = {
        catalog_filter: [ {
            mark: 'AUDI',
            model: '200',
        },
        ],
    };

    await act(async() => {
        runEventPropCallbackOn({ eventType: 'change', node: mmmMulitFilter, values, params: 10 });
    });

    await flushPromises();

    expect(setSearchParams).toHaveBeenCalledTimes(1);

    expect(setSearchParams).toHaveBeenCalledWith({ ...searchParameters, ...values });
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'filters', 'mmm', 'click' ]);

});

it('при выборе кузова меняет данные фильтра', async() => {
    const { findAllByRole } = renderComponent();

    const element = document.querySelector('.ListingFiltersItem__title') as Element;

    await act(async() => {
        await userEvent.click(element);
    });
    const item = (await findAllByRole('button')).find(findByChildrenText('Седан')) as Element;
    const bodyTypeGroup = searchParameters.body_type_group as Array<string>;

    userEvent.click(item);
    await flushPromises();

    expect(setSearchParams).toHaveBeenCalledTimes(1);
    expect(setSearchParams).toHaveBeenCalledWith({ ...searchParameters, body_type_group: [ ...bodyTypeGroup, 'SEDAN' ] });
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'filters', 'body_type_group', 'click' ]);

});

it('при изменении значений цены меняет данные фильтра', async() => {
    const { findByLabelText } = renderComponent();

    const priceFrom = await findByLabelText('от');
    const priceTo = await findByLabelText('до');

    await userEvent.type(priceFrom, '2');
    await userEvent.type(priceTo, '1');

    expect(setSearchParams).toHaveBeenCalledTimes(2);
    expect(setSearchParams).toHaveBeenNthCalledWith(1, { ...searchParameters, price_from: 10002 });
    expect(setSearchParams).toHaveBeenNthCalledWith(2, { ...searchParameters, price_to: 20001 });
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'filters', 'price_from', 'click' ]);

});

it('при изменении года выпуска меняет данные фильтра', async() => {
    renderComponent();

    const filterContainer = document.querySelector('.CarTinderYearFilter');

    const slider = filterContainer?.querySelector('.Slider__click-bar') as Element;

    await act(async() => {
        await fireEvent.click(slider, { clientX: 46 });
    });

    expect(setSearchParams).toHaveBeenCalledTimes(1);
    expect(setSearchParams).toHaveBeenCalledWith({ ...searchParameters, year_to: 2021 });
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'filters', 'year', 'click' ]);

});

it('при изменении пробегом меняет данные фильтра', async() => {
    renderComponent();

    const filterContainer = document.querySelector('.CarTinderKmAgeFilter');

    const slider = filterContainer?.querySelector('.Slider__click-bar') as Element;

    await act(async() => {
        await fireEvent.click(slider, { clientX: 46 });
    });

    expect(setSearchParams).toHaveBeenCalledTimes(1);
    expect(setSearchParams).toHaveBeenCalledWith({ ...searchParameters, km_age_to: 99999999 });
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'filters', 'km_age_to', 'click' ]);

});

it('при изменении слайдера с гео меняет данные фильтра', async() => {
    renderComponent();

    const filterContainer = document.querySelector('.CarTinderGeoFilter');

    const slider = filterContainer?.querySelector('.Slider__click-bar') as Element;

    await act(async() => {
        await fireEvent.click(slider, { clientX: 46 });
    });

    expect(setSearchParams).toHaveBeenCalledTimes(1);
    expect(setSearchParams).toHaveBeenCalledWith({ ...searchParameters, geo_radius: '1000' });
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'filters', 'geo_radius', 'click' ]);

});

it('при клике на сбросить параметры сбросятся до максимальных', async() => {
    MockDate.set('2020-04-20');

    const nextSearchParameters: TSearchParameters = {
        section: searchParameters.section,
        category: searchParameters.category,
        km_age_to: TO_KM_AGE_MAX,
        year_to: 2020,
        year_from: DEFAULT_FROM_YEAR,
        geo_id: [ Number(currentOffer.seller?.location?.geobase_id) ],
        geo_radius: DEFAULT_RADIUS_MAX,
        self_offer_id: '111-333',
    };

    renderComponent();
    const resetBtn = document.querySelector('.FiltersPopup__reset') as Element;

    userEvent.click(resetBtn);

    expect(setSearchParams).toHaveBeenCalledTimes(1);
    expect(setSearchParams).toHaveBeenCalledWith(nextSearchParameters);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'filters', 'reset', 'click' ]);

});

it('при клике на Показать меняем урл, вызываем функцию загрузки фильтрованных пофферов и закрываем модал с фильтрами', async() => {
    const { getByText } = renderComponent();
    // debug();
    const doneBtn = getByText('Показать', { exact: false }) as Element;
    userEvent.click(doneBtn);

    await flushPromises();
    expect(onLoad).toHaveBeenCalledTimes(1);
    expect(onRequestHide).toHaveBeenCalledTimes(1);
    expect(contextMock.replaceState).toHaveBeenCalledTimes(1);
    // eslint-disable-next-line max-len
    expect(contextMock.replaceState).toHaveBeenCalledWith('link/car-tinder/?catalog_filter=mark%3DAUDI%2Cmodel%3DA5&body_type_group=HATCHBACK_5_DOORS&section=all&category=cars&price_from=1000&price_to=2000&year_from=1980&year_to=2000&km_age_to=10000');
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'filters', 'save', 'click' ]);

});

function renderComponent() {
    const store = mockStore(INITIAL_STORE);

    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();

    mockUseSelector(INITIAL_STORE);
    mockUseDispatch(store);

    return render(
        <ContextProvider>
            <Provider store={ store }>
                <CarTinderFilters
                    onRequestHide={ onRequestHide }
                    onLoad={ onLoad }
                />
            </Provider>
        </ContextProvider>,
    );
}
