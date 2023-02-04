/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { useSelector, useDispatch } from 'react-redux';
import { renderHook, act } from '@testing-library/react-hooks';
import { noop } from 'lodash';

import { ContextPage, ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import mockStore from 'autoru-frontend/mocks/mockStore';

import type { C2bAuctionApplicationState } from 'auto-core/react/dataDomain/c2bAuction/types';
import { C2bAuctionSubmitStatus } from 'auto-core/react/dataDomain/c2bAuction/types';

import type { TOfferCategory } from 'auto-core/types/proto/auto/api/api_offer_model';

import { offerDraftMock } from './mock';
import { useC2bAuctionMetrika } from './useC2bAuctionMetrika';
import { c2bAuctionDefaultPlace } from './useC2bAuction';

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

const reachGoalMock = jest.fn();
const context = {
    metrika: {
        reachGoal: reachGoalMock,
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

const initialParams = {
    context,
    date: [] as Array<string>,
    draftId: offerDraftMock.offerId,
    place: c2bAuctionDefaultPlace,
    time: '',
    submitStatus: C2bAuctionSubmitStatus.DEFAULT,
    category: 'cars' as TOfferCategory,
    logBuyoutApplicationSubmit: noop,
};

describe('useC2bAuctionMetrika', () => {

    afterEach(() => {
        reachGoalMock.mockClear();
    });

    it('отправляет цель в метрику при маунте', () => {
        createStore();

        renderHook((params) => useC2bAuctionMetrika(params), {
            initialProps: {
                ...initialParams,
            },
        });

        expect(reachGoalMock).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_INSPECTION_SHOW', {
            draft_id: offerDraftMock.offerId,
        });
    });

    it('отправляет цель в метрику при смене даты', () => {
        createStore();

        const { rerender } = renderHook((params) => useC2bAuctionMetrika(params), {
            initialProps: {
                ...initialParams,
            },
        });

        rerender({
            ...initialParams,
            date: [ '15.11.2021' ],
        });

        expect(reachGoalMock).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_INSPECTION_TIME_READY', {
            draft_id: offerDraftMock.offerId,
        });
    });

    it('отправляет цель в метрику при смене адреса', () => {
        createStore();

        const { rerender } = renderHook((params) => useC2bAuctionMetrika(params), {
            initialProps: {
                ...initialParams,
            },
        });

        rerender({
            ...initialParams,
            place: {
                address: 'Piter',
                lat: 12,
                lon: 23,
            },
        });

        expect(reachGoalMock).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_INSPECTION_ADDRESS_READY', {
            draft_id: offerDraftMock.offerId,
        });
    });

    it('отправляет цель в метрику при редиректе в лк', () => {
        createStore();

        const { result } = renderHook((params) => useC2bAuctionMetrika(params), {
            initialProps: {
                ...initialParams,
            },
        });

        act(() => {
            result.current.sendRedirectGoal();
        });

        expect(reachGoalMock).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_TRANSFER_LK_AUCTION', {
            draft_id: offerDraftMock.offerId,
        });
    });

    it('отправляет цель в метрику при успешной подаче заявки', () => {
        createStore();

        const { rerender } = renderHook((params) => useC2bAuctionMetrika(params), {
            initialProps: {
                ...initialParams,
            },
        });

        rerender({ ...initialParams, submitStatus: C2bAuctionSubmitStatus.SUCCESS });

        expect(reachGoalMock).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_INSPECTION_READY', {
            draft_id: offerDraftMock.offerId,
        });
    });

    describe('Фронтлог', () => {
        it('Отправляет фронтлог при подаче заявки для черновика', () => {
            createStore();

            const logMock = jest.fn();

            const props = {
                ...initialParams,
                draftId: '123',
                applicationId: '456',
                logBuyoutApplicationSubmit: logMock,
            };

            const { rerender } = renderHook((params) => useC2bAuctionMetrika(params), {
                initialProps: props,
            });

            expect(logMock).toHaveBeenCalledTimes(0);

            rerender({
                ...props,
                submitStatus: C2bAuctionSubmitStatus.SUCCESS,
            });

            expect(logMock).toHaveBeenLastCalledWith({
                category: 'cars',
                draftId: '123',
                offerId: undefined,
                applicationId: '456',
                contextPage: ContextPage.PAGE_BUYOUT_CREATE,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });

        it('Отправляет фронтлог при подаче заявки для оффера', () => {
            createStore();

            const logMock = jest.fn();

            const props = {
                ...initialParams,
                draftId: '111',
                offerId: '222',
                applicationId: '456',
                logBuyoutApplicationSubmit: logMock,
            };

            const { rerender } = renderHook((params) => useC2bAuctionMetrika(params), {
                initialProps: props,
            });

            expect(logMock).toHaveBeenCalledTimes(0);

            rerender({
                ...props,
                submitStatus: C2bAuctionSubmitStatus.SUCCESS,
            });

            expect(logMock).toHaveBeenLastCalledWith({
                category: 'cars',
                draftId: '111',
                offerId: '222',
                applicationId: '456',
                contextPage: ContextPage.PAGE_BUYOUT_CREATE,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });
    });

    describe('метрика для car price', () => {
        it('отправляет цель в метрику при смене даты и времени', () => {
            createStore();

            const { rerender } = renderHook((params) => useC2bAuctionMetrika(params), {
                initialProps: {
                    ...initialParams,
                    isCarPrice: true,
                },
            });

            rerender({
                ...initialParams,
                isCarPrice: true,
                date: [ '15.11.2021' ],
            });

            //метрика отправилась только при маунте
            expect(reachGoalMock).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_INSPECTION_SHOW', {
                draft_id: offerDraftMock.offerId,
            });

            rerender({
                ...initialParams,
                isCarPrice: true,
                date: [ '15.11.2021' ],
                time: '17:00',
            });

            expect(reachGoalMock).toHaveBeenLastCalledWith('C2B_AUCTION_REQUEST_INSPECTION_TIME_READY', {
                draft_id: offerDraftMock.offerId,
            });
        });
    });
});
