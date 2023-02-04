/**
 * @jest-environment node
 */
/* eslint-disable max-len */
import { mockStore } from 'core/mocks/store.mock';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { POST_FILTERS_SERVICE_MOCK_1 } from 'core/services/postFilters/mocks/postFiltersService.mock';
import {
    POST_LISTING_SERVICE_MOCK_1,
    POST_LISTING_SERVICE_MOCK_2,
} from 'core/services/postListing/mocks/postListingService.mock';
import { ServiceId } from 'core/services/ServiceId';

import { usePostListingSeo } from './usePostListingSeo';

it('title, description, canonicalUrl если есть фильтры, но нет metaTitle', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST_FILTERS]: POST_FILTERS_SERVICE_MOCK_1,
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_1,
    });

    const result = usePostListingSeo();

    expect(result).toEqual({
        canonicalUrl: 'https://realty.yandex.ru/journal/category/uchebnik/',
        description: 'Исследования рынка недвижимости и статьи - Учебник - 85 полезных статьи в Журнале Недвижимости',
        title: '85 экспертных статей и исследований в разделе Учебник',
    });
});

it('меняется title, description если есть metaTitle', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST_FILTERS]: POST_FILTERS_SERVICE_MOCK_1,
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_2,
    });

    const result = usePostListingSeo();

    expect(result.title).toBe('85 экспертных статей и исследований в разделе Кека');
    expect(result.description).toBe('Исследования рынка недвижимости и статьи - Кека - 85 полезных статьи в Журнале Недвижимости');
});

it('возвращает дефолтный canonical, если нет фильтров', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_2,
    });

    const result = usePostListingSeo();

    expect(result.canonicalUrl).toEqual(ROUTER_SERVICE_MOCK_1.data?.fullUrl);
});
