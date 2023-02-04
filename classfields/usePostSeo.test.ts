/**
 * @jest-environment node
 */
/* eslint-disable max-len */
import { POST_MOCK_1 } from 'core/services/post/mocks/post.mock';
import { mockStore } from 'core/mocks/store.mock';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { ServiceId } from 'core/services/ServiceId';

import { usePostSeo } from './usePostSeo';

describe('с основной категорией поста', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const data = usePostSeo({
        post: POST_MOCK_1,
        mainCategory: POST_MOCK_1.categories[0],
    });

    it('title, description и canonicalUrl', () => {
        expect(data?.title).toBe('Тестовая статья со всеми блоками для тестирования - читайте в разделе Идеи');
        expect(data?.description).toBe('Это лид тестовой статьи со всеми блоками. Пожалуйста, не нужно править этот пост в проде, он является основным чтобы проверить весь функционал - Идеи. Тестовая статья со всеми блоками для тестирования в Журнале Недвижимости');
        expect(data?.canonicalUrl).toBe('https://realty.yandex.ru/journal/post/testovaya-statya-so-vsemi-blokami-dlya-testirovaniya/');
    });

    it('мета информация', () => {
        expect(data?.meta).toEqual([
            {
                content: 'Тестовая статья со всеми блоками для тестирования',
                property: 'og:title',
            },
            {
                content: 'article',
                property: 'og:type',
            },
            {
                content: '2022-01-10T13:20:16.000Z',
                property: 'og:updated_time',
            },
            {
                content: '',
                property: 'og:image',
            },
            {
                content: '',
                property: 'vk:image',
            },
            {
                content: '2021-12-27T14:12:19.000Z',
                property: 'article:published_time',
            },
            {
                content: 'Идеи',
                property: 'yandex_recommendations_category',
            },
        ]);
    });
});

it('title и description без основной категории', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const data = usePostSeo({
        post: POST_MOCK_1,
    });

    expect(data?.title).toBe(POST_MOCK_1.title);
    expect(data?.description).toBe('Это лид тестовой статьи со всеми блоками. Пожалуйста, не нужно править этот пост в проде, он является основным чтобы проверить весь функционал. Тестовая статья со всеми блоками для тестирования в Журнале Недвижимости');
});
