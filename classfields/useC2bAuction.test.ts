/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

import { useSelector, useDispatch } from 'react-redux';
import { renderHook, act } from '@testing-library/react-hooks';

import mockStore from 'autoru-frontend/mocks/mockStore';

import gateApi from 'auto-core/react/lib/gateApi';
import type { C2bAuctionApplicationState } from 'auto-core/react/dataDomain/c2bAuction/types';
import { C2bAuctionSubmitStatus } from 'auto-core/react/dataDomain/c2bAuction/types';

import { offerDraftMock } from './mock';
import { useC2bAuction } from './useC2bAuction';

const getResourceMock = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

interface State {
    c2bAuction: C2bAuctionApplicationState;
}

const defaultState: State = {
    c2bAuction: {
        offerDraft: offerDraftMock,
        submitStatus: C2bAuctionSubmitStatus.DEFAULT,
        applicationInfo: {
            can_apply: true,
            price_range: {
                from: 0,
                to: 1200000,
            },
            price_prediction: 25000,
        },
    },
};

const context = {
    metrika: {
        reachGoal: jest.fn(),
    },
};

function createStore(state: Record<string, any> = defaultState) {
    const store = mockStore(state);

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(state),
    );

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );
}

describe('useC2bAuction', () => {

    afterEach(() => {
        getResourceMock.mockClear();
    });

    it('значения по умолчанию', () => {
        createStore();

        const { result } = renderHook(() => useC2bAuction({ context }));

        expect(result.current.time).toBe('');
        expect(result.current.date).toEqual([]);
        expect(result.current.place).toEqual({
            address: offerDraftMock.offer.place.address,
            lat: offerDraftMock.offer.place.coord.latitude,
            lon: offerDraftMock.offer.place.coord.longitude,
        });
        expect(result.current.offerDraft).toEqual(offerDraftMock);
        expect(result.current.offerPriceRange).toEqual({
            from: 0,
            to: 1200000,
        });
        expect(result.current.offerPriceSingle).toEqual(25000);
    });

    it('правильно меняет дату', () => {
        createStore();

        const { rerender, result } = renderHook(() => useC2bAuction({ context }));

        expect(result.current.date).toEqual([]);

        act(() => {
            result.current.handleChangeDate([ '12.11.2021' ]);
            result.current.handleChangeDate([ '15.11.2021', '20.11.2021' ]);
        });

        rerender();

        expect(result.current.date).toEqual([ '15.11.2021', '20.11.2021' ]);
    });

    it('правильно меняет время', () => {
        createStore();

        const { rerender, result } = renderHook(() => useC2bAuction({ context }));

        expect(result.current.time).toEqual('');

        act(() => {
            result.current.handleChangeTime('12:00');
        });

        rerender();

        expect(result.current.time).toEqual('12:00');
    });

    it('правильно меняет адрес', () => {
        createStore();

        const { rerender, result } = renderHook(() => useC2bAuction({ context }));

        act(() => {
            result.current.handleChangeAddress({
                address: 'Piter',
                lat: 12,
                lon: 23,
            });
        });

        rerender();

        expect(result.current.place).toEqual({
            address: 'Piter',
            lat: 12,
            lon: 23,
        });
    });

    it('правильно меняет комментарий', () => {
        createStore();

        const { rerender, result } = renderHook(() => useC2bAuction({ context }));

        act(() => {
            result.current.handleChangeComment('у шлагбаума');
        });

        rerender();

        expect(result.current.place).toEqual({
            address: offerDraftMock.offer.place.address,
            lat: offerDraftMock.offer.place.coord.latitude,
            lon: offerDraftMock.offer.place.coord.longitude,
            comment: 'у шлагбаума',
        });
    });

    it('правильно делает запрос создания заявки для аукциона авто', () => {
        getResourceMock.mockResolvedValue({});
        createStore();

        const { rerender, result } = renderHook(() => useC2bAuction({ context }));

        act(() => {
            result.current.handleChangeAddress({
                address: 'Piter',
                lat: 12,
                lon: 23,
            });
            result.current.handleChangeDate([ '12.11.2021', '13.11.2021' ]);
            result.current.handleChangeTime('12:00');
        });

        rerender();

        act(() => {
            result.current.handleCreateApplication();
        });

        expect(getResourceMock).toHaveBeenCalledWith('createC2bApplicationAuto', {
            date: '12.11.2021,13.11.2021',
            time: '12:00',
            place: {
                address: 'Piter',
                lat: 12,
                lon: 23,
            },
            draftId: offerDraftMock.offerId,
            category: 'cars',
        });
    });

    it('правильно делает запрос создания заявки для аукциона carprice', () => {
        getResourceMock.mockResolvedValue({});
        createStore();

        const { rerender, result } = renderHook(() => useC2bAuction({
            context,
            isCarPrice: true,
        }));

        act(() => {
            result.current.handleChangeAddress({
                address: 'Piter',
                lat: 12,
                lon: 23,
                id: '3432432',
            });
            result.current.handleChangeDate([ '12-11-2021' ]);
            result.current.handleChangeTime('12:00');
        });

        rerender();

        act(() => {
            result.current.handleCreateApplication();
        });

        expect(getResourceMock).toHaveBeenCalledWith('createC2bApplicationCarPrice', {
            date: '12-11-2021',
            time: '12:00',
            place: {
                address: 'Piter',
                lat: 12,
                lon: 23,
                id: '3432432',
            },
            draftId: offerDraftMock.offerId,
            category: 'cars',
        });

    });
});
