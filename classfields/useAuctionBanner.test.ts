/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/lib/gateApi');
jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});
jest.mock('auto-core/react/dataDomain/cookies/actions/set');
jest.mock('auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutShow');

import { renderHook, act } from '@testing-library/react-hooks';
import { noop } from 'lodash';
import { useSelector, useDispatch } from 'react-redux';

import { ContextPage, ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import mockStore from 'autoru-frontend/mocks/mockStore';

import * as logBuyoutShowModule from 'auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutShow';
import setCookie from 'auto-core/react/dataDomain/cookies/actions/set';
import gateApi from 'auto-core/react/lib/gateApi';
import { C2BCreateApplicationSource } from 'auto-core/react/components/common/C2BAuctions/types';
import { SOURCE_TO_COOKIE_NAME } from 'auto-core/react/components/common/C2BAuctions/constants/sourceToCookiesName';
import type { CookiesChangeAction } from 'auto-core/react/dataDomain/cookies/types';
import { COOKIES_CHANGE } from 'auto-core/react/dataDomain/cookies/types';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import { useAuctionBanner } from './useAuctionBanner';

const MOCK_OFFER = {
    saleId: '1111-111',
    category: 'cars',
    tags: [ 'available_for_c2b_auction' ],
    state: { c2b_auction_info: { price_range: { from: '1000', to: '2000' } } },
} as Offer;
const MOCK_DRAFT = {
    saleId: '2222-222',
    category: 'cars',
    status: 'DRAFT',
} as Offer;
const METRIKA_MOCK = { reachGoal: noop };

jest.useFakeTimers();

describe('Управление баннером Аукционов', () => {
    describe('Оффер', () => {
        it('Отрисовывает баннер, если оффер подходит под Аукцион (Выкуп) и нет куки о его закрытии', () => {
            mockFrontLog();
            mockRedux();
            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_OFFER,
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.isBannerVisible).toBe(true);
        });

        it('Не отрисовывает баннер, если оффер не подходит под Аукцион (Выкуп)', () => {
            // Например, если нет тега
            // Полные тесты на соответствие оффера условиям Выкупа здесь:
            // auto-core/react/components/common/C2BAuctions/helpers/isOfferEligibleForAcution.test.ts
            mockFrontLog();
            mockRedux();
            const { result } = renderHook(() => useAuctionBanner({
                offer: { ...MOCK_OFFER, tags: [] },
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.isBannerVisible).toBe(false);
        });

        it('Не показывает баннер, если есть кука скрытия баннера', () => {
            const source = C2BCreateApplicationSource.OfferCard;
            const cookieName = SOURCE_TO_COOKIE_NAME[source];

            mockFrontLog();
            mockRedux({
                cookies: { [cookieName]: 'true' },
            });
            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_OFFER,
                metrika: METRIKA_MOCK,
                source,
            }));

            expect(result.current.isBannerVisible).toBe(false);
        });

        it('При закрытии баннера выставляет соответствующую куку и отправляет метрику', async() => {
            const metrikaMock = { reachGoal: jest.fn() };
            const source = C2BCreateApplicationSource.OfferCard;
            const cookieName = SOURCE_TO_COOKIE_NAME[source];

            mockFrontLog();
            const store = mockRedux({
                cookies: {},
            });

            const setCookieMock = jest.fn((key, value): CookiesChangeAction => ({ type: COOKIES_CHANGE, payload: { key, value } }));
            (setCookie as jest.MockedFunction<typeof setCookie>).mockImplementation((key, value) => setCookieMock(key, value));

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_OFFER,
                metrika: metrikaMock,
                source,
            }));

            act(() => result.current.closeBanner());
            expect(store.getActions()).toEqual([ { type: 'COOKIES_CHANGE', payload: { key: cookieName, value: 'true' } } ]);
            expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(2); // первый раз при показе баннера
            expect(metrikaMock.reachGoal).toHaveBeenLastCalledWith('BANNER_C2B_AUCTION_CLOSE', {
                banner_c2b_auction: {
                    card_offer_owner: {
                        close: {
                            offer_id: '1111-111',
                        },
                    },
                },
            });
        });

        it('При показе баннера отправляет метрику', async() => {
            const metrikaMock = { reachGoal: jest.fn() };
            mockFrontLog();
            mockRedux();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_OFFER,
                metrika: metrikaMock,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.isBannerVisible).toBe(true);
            expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(1);
            expect(metrikaMock.reachGoal).toHaveBeenLastCalledWith('BANNER_C2B_AUCTION_SHOW', {
                banner_c2b_auction: {
                    card_offer_owner: {
                        show: {
                            offer_id: '1111-111',
                        },
                    },
                },
            });
        });

        it('Не возвращает draftId для оффера', () => {
            const metrikaMock = { reachGoal: jest.fn() };
            mockFrontLog();
            mockRedux();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_OFFER,
                metrika: metrikaMock,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.draftId).toBe(undefined);
        });
    });

    describe('Черновик', () => {
        it('Отправляет запрос при первом рендере и отрисовывает баннер, если ручка ответила true', async() => {
            mockFrontLog();
            const getResourceSpy = mockGateApi();
            mockRedux();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_DRAFT,
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.draftId).toBe(MOCK_DRAFT.saleId);
            expect(result.current.priceRange).toEqual({ from: 0, to: 0 });
            expect(result.current.isBannerVisible).toBe(false);

            // Ждем выполнения запросов
            await act(async() => {
                await jest.runAllTicks();
            });

            expect(result.current.priceRange).toEqual({ from: 1000, to: 2000 });
            expect(result.current.isBannerVisible).toBe(true);

            expect(getResourceSpy).toHaveBeenCalledTimes(1);
            expect(getResourceSpy).toHaveBeenLastCalledWith('getC2bAuctionInfo', {
                offer_id: MOCK_DRAFT.saleId,
                parent_category: 'cars',
            });
        });

        it('Не отрисовывает баннер, если ручка ответила false', async() => {
            mockFrontLog();
            const getResourceSpy = mockGateApi({ can_apply: false });
            mockRedux();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_DRAFT,
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.isBannerVisible).toBe(false);
            expect(result.current.priceRange).toEqual({ from: 0, to: 0 });

            // Ждем выполнения запросов
            await act(async() => {
                await jest.runAllTicks();
            });

            expect(result.current.isBannerVisible).toBe(false);
            expect(result.current.priceRange).toEqual({ from: 0, to: 0 });

            expect(getResourceSpy).toHaveBeenCalledTimes(1);
            expect(getResourceSpy).toHaveBeenLastCalledWith('getC2bAuctionInfo', {
                offer_id: MOCK_DRAFT.saleId,
                parent_category: 'cars',
            });
        });

        it('Не отрисовывает баннер и не отправляет запрос с проверкой оффера, если есть кука скрытия баннера', async() => {
            const source = C2BCreateApplicationSource.OfferCard;

            mockFrontLog();
            const getResourceSpy = mockGateApi();
            const cookieName = SOURCE_TO_COOKIE_NAME[source];
            mockRedux({
                cookies: { [cookieName]: 'true' },
            });

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_DRAFT,
                metrika: METRIKA_MOCK,
                source,
            }));

            expect(result.current.isBannerVisible).toBe(false);
            expect(result.current.priceRange).toEqual({ from: 0, to: 0 });

            // Здесь запросов не уходит, но для чистоты эксперимента все равно ждем
            await act(async() => {
                await jest.runAllTicks();
            });

            expect(result.current.isBannerVisible).toBe(false);
            expect(result.current.priceRange).toEqual({ from: 0, to: 0 });

            expect(getResourceSpy).toHaveBeenCalledTimes(0);
        });

        it('Возвращает draftId', async() => {
            mockFrontLog();
            mockGateApi();
            mockRedux();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_DRAFT,
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.draftId).toBe(MOCK_DRAFT.saleId);

            // Ждем выполнения запросов, потому что стейт обновляется асинхронно
            // Если не ждать, то засыпет warning'ами в консоль об обновлении стейта не в act()
            await act(async() => {
                await jest.runAllTicks();
            });
        });
    });

    describe('Фронтлог', () => {
        it('При показе баннера отправляет фронтлог черновика', async() => {
            const logShowMock = mockFrontLog();
            mockRedux();
            mockGateApi();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_DRAFT,
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.isBannerVisible).toBe(false);

            // Ждем выполнения запросов
            await act(async() => {
                await jest.runAllTicks();
            });
            expect(result.current.isBannerVisible).toBe(true);
            expect(logShowMock).toHaveBeenCalledTimes(1);
            expect(logShowMock).toHaveBeenLastCalledWith({
                category: MOCK_DRAFT.category,
                draftId: MOCK_DRAFT.saleId,
                offerId: undefined,
                contextPage: ContextPage.PAGE_OFFER_OWNER,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });

        it('При показе баннера отправляет фронтлог оффера', async() => {
            const logShowMock = mockFrontLog();
            mockRedux();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_OFFER,
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.OfferCard,
            }));

            expect(result.current.isBannerVisible).toBe(true);
            expect(logShowMock).toHaveBeenCalledTimes(1);
            expect(logShowMock).toHaveBeenLastCalledWith({
                category: MOCK_OFFER.category,
                draftId: undefined,
                offerId: MOCK_OFFER.saleId,
                contextPage: ContextPage.PAGE_OFFER_OWNER,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });

        it('При показе баннера отправляет фронтлог из ЛК для черновика', async() => {
            const logShowMock = mockFrontLog();
            mockRedux();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_DRAFT,
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.MyOfferSnippet,
            }));

            expect(result.current.isBannerVisible).toBe(false);

            // Ждем выполнения запросов
            await act(async() => {
                await jest.runAllTicks();
            });

            expect(result.current.isBannerVisible).toBe(true);
            expect(logShowMock).toHaveBeenCalledTimes(1);
            expect(logShowMock).toHaveBeenLastCalledWith({
                category: MOCK_DRAFT.category,
                draftId: MOCK_DRAFT.saleId,
                offerId: undefined,
                contextPage: ContextPage.PAGE_LK,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });

        it('При показе баннера отправляет фронтлог из ЛК для оффера', async() => {
            const logShowMock = mockFrontLog();
            mockRedux();

            const { result } = renderHook(() => useAuctionBanner({
                offer: MOCK_OFFER,
                metrika: METRIKA_MOCK,
                source: C2BCreateApplicationSource.MyOfferSnippet,
            }));

            expect(result.current.isBannerVisible).toBe(true);
            expect(logShowMock).toHaveBeenCalledTimes(1);
            expect(logShowMock).toHaveBeenLastCalledWith({
                category: MOCK_OFFER.category,
                draftId: undefined,
                offerId: MOCK_OFFER.saleId,
                contextPage: ContextPage.PAGE_LK,
                contextBlock: ContextBlock.BLOCK_CARD,
            });
        });
    });
});

function mockGateApi(getC2BAuctionInfoResult = {}) {
    const getResourceSpy = jest.spyOn(gateApi, 'getResource').mockImplementation((resource: string) => {
        return new Promise((resolve) => {
            if (resource === 'getC2bAuctionInfo') {
                resolve({
                    can_apply: true,
                    price_range: { from: 1000, to: 2000 },
                    ...getC2BAuctionInfoResult,
                });
                return;
            }

            resolve({});
        });
    });

    return getResourceSpy;
}

function mockRedux(state: Record<string, any> = {}) {
    const store = mockStore({
        ...state,
        c2bAuction: {
            submitStatus: 'success',
            ...state?.c2bAuction,
        },
    });

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );

    return store;
}

function mockFrontLog() {
    const logShowMock = jest.fn();

    jest.spyOn(logBuyoutShowModule, 'logBuyoutShow').mockImplementation(logShowMock);

    return logShowMock;
}
