/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutClick');

import { renderHook, act } from '@testing-library/react-hooks';
import { noop } from 'lodash';

import { ContextPage, ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import { C2BCreateApplicationSource } from 'auto-core/react/components/common/C2BAuctions/types';
import * as logBuyoutClickModule from 'auto-core/react/components/common/C2BAuctions/frontLog/logBuyoutClick';

import { useAuctionBannerModals } from './useAuctionBannerModals';

const METRIKA_MOCK = { reachGoal: noop };

describe('Управление модалками баннера Аукционов', () => {
    it('По-умолчанию модалки выключены', () => {
        mockFrontLog();

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: METRIKA_MOCK,
            draftId: '',
            offerId: '',
            category: 'cars',
            source: C2BCreateApplicationSource.OfferCard,
        }));

        expect(result.current.info.isVisible).toBe(false);
        expect(result.current.form.isVisible).toBe(false);
    });

    it('Работает показ/скрытие модалки Информации', () => {
        mockFrontLog();

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: METRIKA_MOCK,
            draftId: '',
            offerId: '',
            category: 'cars',
            source: C2BCreateApplicationSource.OfferCard,
        }));

        expect(result.current.info.isVisible).toBe(false);

        act(() => result.current.info.show());
        expect(result.current.info.isVisible).toBe(true);

        act(() => result.current.info.hide());
        expect(result.current.info.isVisible).toBe(false);
    });

    it('Работает показ/скрытие модалки Формы заявки', () => {
        mockFrontLog();

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: METRIKA_MOCK,
            draftId: '',
            offerId: '',
            category: 'cars',
            source: C2BCreateApplicationSource.OfferCard,
        }));

        expect(result.current.form.isVisible).toBe(false);

        act(() => result.current.form.show());
        expect(result.current.form.isVisible).toBe(true);

        act(() => result.current.form.hide());
        expect(result.current.form.isVisible).toBe(false);
    });

    it('При открытии модалки с описанием, отправляется событие метрики для черновика', () => {
        mockFrontLog();

        const metrikaMock = { reachGoal: jest.fn() };

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: metrikaMock,
            draftId: '123',
            category: 'cars',
            source: C2BCreateApplicationSource.OfferCard,
        }));

        act(() => result.current.info.show());
        expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(1);
        expect(metrikaMock.reachGoal).toHaveBeenLastCalledWith('BANNER_C2B_AUCTION_DETAIL', {
            banner_c2b_auction: {
                card_offer_owner: {
                    detail: {
                        offer_id: '123',
                    },
                },
            },
        });
    });

    it('При открытии модалки с описанием, отправляется событие метрики для оффера', () => {
        mockFrontLog();

        const metrikaMock = { reachGoal: jest.fn() };

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: metrikaMock,
            draftId: '123',
            offerId: '456',
            category: 'cars',
            source: C2BCreateApplicationSource.OfferCard,
        }));

        act(() => result.current.info.show());
        expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(1);
        expect(metrikaMock.reachGoal).toHaveBeenLastCalledWith('BANNER_C2B_AUCTION_DETAIL', {
            banner_c2b_auction: {
                card_offer_owner: {
                    detail: {
                        offer_id: '456',
                    },
                },
            },
        });
    });

    it('При открытии модалки с формой, отправляется событие метрики для черновика', () => {
        mockFrontLog();

        const metrikaMock = { reachGoal: jest.fn() };

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: metrikaMock,
            draftId: '123',
            category: 'cars',
            source: C2BCreateApplicationSource.OfferCard,
        }));

        act(() => result.current.form.show());
        expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(1);
        expect(metrikaMock.reachGoal).toHaveBeenLastCalledWith('BANNER_C2B_AUCTION_CLICK', {
            banner_c2b_auction: {
                card_offer_owner: {
                    click: {
                        offer_id: '123',
                    },
                },
            },
        });
    });

    it('При открытии модалки с формой, отправляется событие метрики для оффера', () => {
        mockFrontLog();

        const metrikaMock = { reachGoal: jest.fn() };

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: metrikaMock,
            draftId: '123',
            offerId: '456',
            category: 'cars',
            source: C2BCreateApplicationSource.OfferCard,
        }));

        act(() => result.current.form.show());
        expect(metrikaMock.reachGoal).toHaveBeenCalledTimes(1);
        expect(metrikaMock.reachGoal).toHaveBeenLastCalledWith('BANNER_C2B_AUCTION_CLICK', {
            banner_c2b_auction: {
                card_offer_owner: {
                    click: {
                        offer_id: '456',
                    },
                },
            },
        });
    });

    it('При открытии модалки формы отправляет фронтлог для ЛК', () => {
        const logClickMock = mockFrontLog();

        const metrikaMock = { reachGoal: jest.fn() };

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: metrikaMock,
            draftId: '123',
            offerId: '456',
            category: 'cars',
            source: C2BCreateApplicationSource.MyOfferSnippet,
        }));

        expect(logClickMock).toHaveBeenCalledTimes(0);
        act(() => result.current.form.show());

        expect(logClickMock).toHaveBeenCalledTimes(1);
        expect(logClickMock).toHaveBeenLastCalledWith({
            category: 'cars',
            draftId: '123',
            offerId: '456',
            contextPage: ContextPage.PAGE_LK,
            contextBlock: ContextBlock.BLOCK_CARD,
        });
    });

    it('При открытии модалки формы отправляет фронтлог для Карточки оффера', () => {
        const logClickMock = mockFrontLog();

        const metrikaMock = { reachGoal: jest.fn() };

        const { result } = renderHook(() => useAuctionBannerModals({
            metrika: metrikaMock,
            draftId: '123',
            offerId: '456',
            category: 'cars',
            source: C2BCreateApplicationSource.OfferCard,
        }));

        expect(logClickMock).toHaveBeenCalledTimes(0);
        act(() => result.current.form.show());

        expect(logClickMock).toHaveBeenCalledTimes(1);
        expect(logClickMock).toHaveBeenLastCalledWith({
            category: 'cars',
            draftId: '123',
            offerId: '456',
            contextPage: ContextPage.PAGE_OFFER_OWNER,
            contextBlock: ContextBlock.BLOCK_CARD,
        });
    });
});

function mockFrontLog() {
    const logClickMock = jest.fn();

    jest.spyOn(logBuyoutClickModule, 'logBuyoutClick').mockImplementation(logClickMock);

    return logClickMock;
}
