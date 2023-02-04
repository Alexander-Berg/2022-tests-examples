/**
 * @jest-environment node
 */
import { renderHook, act } from '@testing-library/react-hooks';

import { mockStore } from 'core/mocks/store.mock';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { LISTING_BLOCK_TEXTBOOK_FOR_MAP_MOCK } from 'core/services/postListing/mocks/listingBlock.mock';
import { ServiceId } from 'core/services/ServiceId';

import { useListingBlockTextbook } from './useListingBlockTextbook';

describe('возвращает базовые параметры для первого таба по-умолчанию', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const { result } = renderHook(() =>
        useListingBlockTextbook(LISTING_BLOCK_TEXTBOOK_FOR_MAP_MOCK)
    );

    const textbook = LISTING_BLOCK_TEXTBOOK_FOR_MAP_MOCK.data.textbook;

    it('текст кнопки, ссылка на листинг и заголовок', () => {
        expect(result.current.buttonText).toBe('Больше материалов');
        expect(result.current.listingHref).toBe('/journal/category/uchebnik/');
        expect(result.current.title).toBe(textbook.title);
    });

    it('первый таб нулевой', () => {
        expect(result.current.currentTab.value).toBe(0);
    });

    it('список постов', () => {
        const currentTabsPostsUrlPart = result.current.currentTab.posts.map((post) => post.urlPart);

        expect(currentTabsPostsUrlPart).toEqual(LISTING_BLOCK_TEXTBOOK_FOR_MAP_MOCK.data.textbook.tabs[0].posts);
    });
});

describe('смена таба на второй', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const { result } = renderHook(() =>
        useListingBlockTextbook(LISTING_BLOCK_TEXTBOOK_FOR_MAP_MOCK)
    );

    act(() => {
        result.current.currentTab.onChange({
            value: 1,
        });
    });

    it('значение текущего выбранного таба', () => {
        expect(result.current.currentTab.value).toBe(1);
    });

    it('список постов', () => {
        const currentTabsPostsUrlPart = result.current.currentTab.posts.map((post) => post.urlPart);

        expect(currentTabsPostsUrlPart).toEqual(LISTING_BLOCK_TEXTBOOK_FOR_MAP_MOCK.data.textbook.tabs[1].posts);
    });
});
