/**
 * @jest-environment node
 */
import { mockStore } from 'core/mocks/store.mock';
import { ServiceId } from 'core/services/ServiceId';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import {
    POST_FILTERS_SERVICE_MOCK_1,
    POST_FILTERS_SERVICE_MOCK_2,
} from 'core/services/postFilters/mocks/postFiltersService.mock';
import {
    POST_LISTING_SERVICE_MOCK_1,
    POST_LISTING_SERVICE_MOCK_3,
} from 'core/services/postListing/mocks/postListingService.mock';
import { CONFIG_SERVICE_MOCK_1, CONFIG_SERVICE_MOCK_2 } from 'core/services/config/mocks/configService.mock';

import { useBreadcrumbs } from './useBreadcrumbs';

describe('не возвращает элементы, если', () => {
    it('нет фильтров', () => {
        mockStore({
            [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
            [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_1,
        });

        const result = useBreadcrumbs();

        expect(result.items).toHaveLength(0);
    });

    it('нет листинга', () => {
        mockStore({
            [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
            [ServiceId.POST_FILTERS]: POST_FILTERS_SERVICE_MOCK_1,
        });

        const result = useBreadcrumbs();

        expect(result.items).toHaveLength(0);
    });
});

it('элементы только для главной, журнала и учебника, если нет subListing и параметров поиска', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_1,
        [ServiceId.POST_FILTERS]: POST_FILTERS_SERVICE_MOCK_1,
    });

    const result = useBreadcrumbs();

    expect(result.items).toEqual([
        { href: 'https://realty.yandex.ru', isHostItem: true, text: 'Я.Недвижимость' },
        { href: '/journal/', text: 'Журнал' },
        { text: 'Учебник' },
    ]);
});

it('элементы для главной, журнала, учебника и аренды, если есть subListing и параметры поиска', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_3,
        [ServiceId.POST_FILTERS]: POST_FILTERS_SERVICE_MOCK_2,
        [ServiceId.CONFIG]: CONFIG_SERVICE_MOCK_1,
    });

    const result = useBreadcrumbs();

    expect(result.items).toEqual([
        { href: 'https://realty.yandex.ru', isHostItem: true, text: 'Я.Недвижимость' },
        { href: '/journal/', text: 'Журнал' },
        { href: '/journal/category/uchebnik/', text: 'Учебник' },
        { text: 'Аренда' },
    ]);
});

it('элементы для журнала, учебника и аренды, если есть isWebView=true', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_3,
        [ServiceId.POST_FILTERS]: POST_FILTERS_SERVICE_MOCK_2,
        [ServiceId.CONFIG]: CONFIG_SERVICE_MOCK_2,
    });

    const result = useBreadcrumbs();

    expect(result.items).toEqual([
        { href: '/journal/', text: 'Журнал' },
        { href: '/journal/category/uchebnik/', text: 'Учебник' },
        { text: 'Аренда' },
    ]);
});
