/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/lib/geo');
jest.mock('auto-core/react/lib/cookie');

import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import geo from 'auto-core/react/lib/geo';
import cookie from 'auto-core/react/lib/cookie';
import geoStateMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';

import type { ReduxState, OwnProps } from './ListingGeoRadiusCounters';
import ListingGeoRadiusCounters, { COOKIE_NAME_RADIUS } from './ListingGeoRadiusCounters';

const cookiesSetForever = cookie.setForever as jest.MockedFunction<typeof cookie.setForever>;

const geoSave = geo.save as jest.MockedFunction<typeof geo.save>;
const geoRedirect = geo.redirect as jest.MockedFunction<typeof geo.redirect>;

let state: ReduxState;
let props: OwnProps;
let originalWindowLocation: Location;

beforeEach(() => {
    state = {
        geo: geoStateMock,
        listingGeoRadiusCounters: {
            data: [],
            pending: false,
        },
    };

    props = {
        geoRadius: 100,
        isGeoRadiusAllowed: true,
        gidsInfo: geoStateMock.gidsInfo,
    };

    originalWindowLocation = global.window.location;
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.window.location;
    global.window.location = { ...originalWindowLocation };
});

afterEach(() => {
    global.window.location = originalWindowLocation;
});

it('должен отрендерить геокольца, если их больше одного', () => {
    state.listingGeoRadiusCounters = {
        data: [
            {
                radius: 0,
                count: 100,
            },
            {
                radius: 100,
                count: 200,
            },
            {
                radius: 200,
                count: 500,
            },
        ],
        pending: false,
    };
    const wrapper = shallowRenderComponent({ state, props });
    expect(wrapper.isEmptyRender()).toBe(false);
});

it('не должен отрендерить геокольца, если их нет', () => {
    state.listingGeoRadiusCounters = {
        data: [
            {
                radius: 101,
                count: 200,
            },
        ],
        pending: false,
    };
    const wrapper = shallowRenderComponent({ state, props });

    expect(wrapper.isEmptyRender()).toBe(true);
});

it('не должен отрендерить геокольца, если их одно', () => {
    state.listingGeoRadiusCounters = {
        data: [],
        pending: false,
    };
    const wrapper = shallowRenderComponent({ state, props });

    expect(wrapper.isEmptyRender()).toBe(true);
});

it('не должен отрендерить геокольца, если нет расширения радиуса', () => {
    state.listingGeoRadiusCounters = {
        data: [
            {
                radius: 0,
                count: 100,
            },
            {
                radius: 100,
                count: 200,
            },
            {
                radius: 200,
                count: 500,
            },
        ],
        pending: false,
    };
    props.isGeoRadiusAllowed = false;
    const wrapper = shallowRenderComponent({ state, props });

    expect(wrapper.isEmptyRender()).toBe(true);
});

it('нарисует чипсину "Россия", если она доступна', () => {
    state.listingGeoRadiusCounters = {
        data: [
            { radius: 0, count: 100 },
            { radius: 100, count: 200 },
            { radius: 200, count: 500 },
            { radius: 1100, count: 1500 },
        ],
        pending: false,
    };
    const wrapper = shallowRenderComponent({ state, props });
    const globalRadiusItem = wrapper.find('.ListingGeoRadiusCounters__item').at(3);
    expect(globalRadiusItem.find('.ListingGeoRadiusCounters__itemRadius').text()).toBe('Россия');
});

describe('при клике на чипсу', () => {
    beforeEach(() => {
        state.listingGeoRadiusCounters = {
            data: [
                { radius: 0, count: 100 },
                { radius: 100, count: 200 },
                { radius: 200, count: 500 },
                { radius: 1100, count: 1500 },
            ],
            pending: false,
        };
    });

    it('должен удалить geo_radius из урла при редиректе', () => {
        global.location.href = 'https://auto.ru/moskva/cars/audi/rs5/20950508/all/?geo_radius=200';
        const wrapper = shallowRenderComponent({ state, props });

        wrapper.find('.ListingGeoRadiusCounters__item').at(0).simulate('click');
        expect(geoRedirect).toHaveBeenCalledTimes(1);
        expect(geoRedirect).toHaveBeenCalledWith({ url: 'https://auto.ru/moskva/cars/audi/rs5/20950508/all/', geo: [ 213 ] });
    });

    it('должен сохранить гео', () => {
        const wrapper = shallowRenderComponent({ state, props });

        wrapper.find('.ListingGeoRadiusCounters__item').at(0).simulate('click');
        expect(geoSave).toHaveBeenCalledTimes(1);
        expect(geoSave).toHaveBeenCalledWith([ 213 ]);
    });

    it('должен сохранить куку с радиусом', () => {
        const wrapper = shallowRenderComponent({ state, props });

        wrapper.find('.ListingGeoRadiusCounters__item').at(2).simulate('click');
        expect(cookiesSetForever).toHaveBeenCalledTimes(1);
        expect(cookiesSetForever).toHaveBeenCalledWith(COOKIE_NAME_RADIUS, '200');
    });

    it('должен сбросить гео, если выбрали чипсину "Россия"', () => {
        const wrapper = shallowRenderComponent({ state, props });

        wrapper.find('.ListingGeoRadiusCounters__item').at(3).simulate('click');
        expect(geoSave).toHaveBeenCalledTimes(1);
        expect(geoSave).toHaveBeenCalledWith([]);
    });
});

function shallowRenderComponent({ props, state }: { props: OwnProps; state: ReduxState }) {
    const page = shallow(
        <Provider store={ mockStore(state) }>
            <ListingGeoRadiusCounters { ...props }/>
        </Provider>,
        { context: contextMock },
    );

    return page.dive().dive();
}
