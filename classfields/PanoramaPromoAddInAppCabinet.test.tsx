/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});
jest.mock('auto-core/react/dataDomain/cookies/actions/setToRoot');
jest.mock('auto-core/react/dataDomain/cookies/actions/setToRootForever');

import React from 'react';
import { shallow } from 'enzyme';
import { useSelector, useDispatch } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import { COOKIES_CHANGE } from 'auto-core/react/dataDomain/cookies/types';
import type { CookiesChangeAction } from 'auto-core/react/dataDomain/cookies/types';
import setCookieToRoot from 'auto-core/react/dataDomain/cookies/actions/setToRoot';
import setToRootForever from 'auto-core/react/dataDomain/cookies/actions/setToRootForever';

import type { AppState } from './PanoramaPromoAddInAppCabinet';
import PanoramaPromoAddInAppCabinet, { COOKIE_CLOSED, COOKIE_SEEN } from './PanoramaPromoAddInAppCabinet';

const setCookieToRootMock = setCookieToRoot as jest.MockedFunction<typeof setCookieToRoot>;
setCookieToRootMock.mockReturnValue({ type: COOKIES_CHANGE, payload: { bar: 'bar' } } as CookiesChangeAction);

const setToRootForeverMock = setToRootForever as jest.MockedFunction<typeof setToRootForever>;
setToRootForeverMock.mockReturnValue({ type: COOKIES_CHANGE, payload: { bar: 'bar' } } as CookiesChangeAction);

let initialState: AppState;

beforeEach(() => {
    initialState = {
        cookies: {},
    };
});

it('формирует правильную ссылку', () => {
    const page = shallowRenderComponent({ initialState });
    const promo = page.find('.PanoramaPromoAddInAppCabinet');

    expect(promo.prop('url')).toBe('link/dealers/?section=panoramas');
    expect(promo.prop('metrikaParams')).toBe('panoramas,dealer_banner,open');
});

it('при ховере отправляет метрику', () => {
    const page = shallowRenderComponent({ initialState });
    page.simulate('mouseEnter');

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'panoramas', 'dealer_banner', 'show' ]);
});

describe('при клике на крест', () => {
    it('отправляет метрику', () => {
        const page = shallowRenderComponent({ initialState });
        const closer = page.find('.PanoramaPromoAddInAppCabinet__closer');
        closer.simulate('click', { preventDefault: () => {} });

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'panoramas', 'dealer_banner', 'close' ]);
    });

    it('если не видели баннер до, поставит 2 куки', () => {
        const page = shallowRenderComponent({ initialState });
        const closer = page.find('.PanoramaPromoAddInAppCabinet__closer');
        closer.simulate('click', { preventDefault: () => {} });

        expect(setCookieToRoot).toHaveBeenCalledTimes(1);
        expect(setCookieToRoot).toHaveBeenCalledWith(COOKIE_CLOSED, 'true', { expires: 14 });

        expect(setToRootForeverMock).toHaveBeenCalledTimes(1);
        expect(setToRootForeverMock).toHaveBeenCalledWith(COOKIE_SEEN, 'true');
    });

    it('если видели баннер до, поставит 1 куку', () => {
        initialState.cookies[COOKIE_SEEN] = 'true';
        const page = shallowRenderComponent({ initialState });
        const closer = page.find('.PanoramaPromoAddInAppCabinet__closer');
        closer.simulate('click', { preventDefault: () => {} });

        expect(setCookieToRoot).toHaveBeenCalledTimes(1);
        expect(setCookieToRoot).toHaveBeenCalledWith(COOKIE_CLOSED, 'true', { expires: 30 });
    });
});

function shallowRenderComponent({ initialState }: { initialState: AppState }) {
    const ContextProvider = createContextProvider(contextMock);
    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation((selector) => selector(initialState));
    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockImplementation(() => () => ({}) as any);

    const page = shallow(
        <ContextProvider>
            <PanoramaPromoAddInAppCabinet/>
        </ContextProvider>,
    );

    return page.dive();
}
