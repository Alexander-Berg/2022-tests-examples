import { ContentSource } from '@vertis/schema-registry/ts-types-snake/auto/lenta/content';

import type { LentaItem } from 'auto-core/react/dataDomain/lenta/TStateLenta';
import lentaMock from 'auto-core/react/dataDomain/lenta/mocks/lenta';

import lentaArticlesToTabDataArray from './lentaArticlesToTabDataArray';

const unrecognizedItem = {
    id: 'review_9092468165031835615',
    created: '2020-02-07T15:31:44.855Z',
    title: 'title',
    url: 'url',
    snippetText: 'text',
    source: ContentSource.UNRECOGNIZED,
} as unknown as LentaItem;

it('должен правильно перевести данные из стора lenta.items в массив из TabData для гаража', () => {
    const items = [ ...lentaMock.items, unrecognizedItem ];
    const actual = lentaArticlesToTabDataArray({ items });

    const expected = [
        {
            type: 'ARTICLE',
            id: 'magazine_16843',
            date: '28 января 2022',
            image: 'picture-of-cat',
            title: 'Неочевидные факты о марке Nissan, которые вы могли не знать',
            url: 'https://mag.autoru_frontend.base_domain/article/neochevidnye-fakty-o-nissan-kotorye-vy-mogli-ne-znat/',
            kind: 'Новости',
        },
        {
            type: 'ARTICLE',
            id: 'magazine_17786',
            date: '15 марта 2022',
            image: 'picture-of-cat',
            title: 'Nissan GT-R покинет европейский рынок — всё из-за экологии и цены',
            url: 'https://mag.autoru_frontend.base_domain/article/nissan-gtr-ushyol-iz-evropy-ego-podkosili-ekologiya-i-cena/',
            kind: 'Новости',
        },
        {
            date: '07 февраля 2020',
            id: 'review_9092468165031835615',
            image: 'picture-of-cat',
            kind: 'Отзывы',
            title: 'Надежный японец',
            type: 'REVIEW',
            url: '/review/cars/nissan/bluebird/6016197/9092468165031835615',
        },
    ];

    expect(actual).toHaveLength(3);
    expect(actual).toEqual(expected);
});
