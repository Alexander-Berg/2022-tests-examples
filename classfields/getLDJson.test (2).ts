/**
 * @jest-environment node
 */
/* eslint-disable max-len */
import { IMAGE_MOCK_1 } from 'core/mocks/image.mock';
import { POST_MOCK_1 } from 'core/services/post/mocks/post.mock';

import { getLDJson } from './getLDJson';

it('LDJson, когда есть автор и нет обложки', () => {
    const result = getLDJson(POST_MOCK_1);

    expect(result).toEqual({
        '@context': 'https://schema.org',
        '@type': 'Article',
        author:  {
            '@context': 'https://schema.org',
            '@type': 'Person',
            name: 'Майкл Скотт',
        },
        backstory: 'Это лид тестовой статьи со всеми блоками. Пожалуйста, не нужно править этот пост в проде, он является основным чтобы проверить весь функционал',
        dateModified: '2022-01-10T13:20:16.000Z',
        datePublished: '2021-12-27T14:12:19.000Z',
        headline: 'Тестовая статья со всеми блоками для тестирования',
        image: [
            '',
        ],
        publisher: {
            '@context': 'https://schema.org',
            '@type': 'Organization',
            name: 'Редакция Я так живу',
        },
    });
});

it('LDJson с дефолтным автором и с обложкой поста', () => {
    const post = { ...POST_MOCK_1 };
    delete post.authorHelp;
    post.cover = IMAGE_MOCK_1;

    const result = getLDJson(post);

    expect(result?.author).toEqual({
        '@context': 'https://schema.org',
        '@type': 'Person',
        name: 'Редакция Я так живу',
    });

    expect(result?.image).toEqual([ '', '' ]);
});
