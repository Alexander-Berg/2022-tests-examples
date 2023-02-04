/**
 * @jest-environment node
 */
import { mockStore } from 'core/mocks/store.mock';
import { ServiceId } from 'core/services/ServiceId';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { CONFIG_SERVICE_MOCK_2 } from 'core/services/config/mocks/configService.mock';

import { POST_SERVICE_MOCK_1, POST_SERVICE_MOCK_2 } from '../mocks/postService.mock';

import { useBreadcrumbs } from './useBreadcrumbs';

it('не возвращает элементы, если нет поста', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
    });

    const result = useBreadcrumbs();

    expect(result.items).toHaveLength(0);
});

it('возвращает элементы, если есть пост, isWebView=false и нет mainCategory', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST]: POST_SERVICE_MOCK_1,
    });

    const result = useBreadcrumbs();

    expect(result.items).toEqual([
        { href: 'https://realty.yandex.ru', isHostItem: true, text: 'Я.Недвижимость' },
        { href: '/journal/', text: 'Журнал' },
        { href: '/journal/category/ideas/', text: 'Идеи' },
        { text: 'Тестовая статья со всеми блоками для тестирования' },
    ]);
});

it('возвращает элементы, если есть пост с категорией и isWebView=false', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST]: POST_SERVICE_MOCK_1,
    });

    const result = useBreadcrumbs();

    expect(result.items).toEqual([
        { href: 'https://realty.yandex.ru', isHostItem: true, text: 'Я.Недвижимость' },
        { href: '/journal/', text: 'Журнал' },
        { href: '/journal/category/ideas/', text: 'Идеи' },
        { text: 'Тестовая статья со всеми блоками для тестирования' },
    ]);
});

it('возвращает элементы, если есть пост без категории и isWebView=true', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST]: POST_SERVICE_MOCK_2,
        [ServiceId.CONFIG]: CONFIG_SERVICE_MOCK_2,
    });

    const result = useBreadcrumbs();

    expect(result.items).toEqual([
        { href: '/journal/', text: 'Журнал' },
        { text: '7 квартир и домов из фильмов и сериалов, которые можно просто взять и арендовать' },
    ]);
});
