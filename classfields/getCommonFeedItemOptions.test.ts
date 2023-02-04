import articleMock from 'auto-core/react/dataDomain/mag/articleMock';
import type { MagPublishedArticle } from 'auto-core/react/dataDomain/mag/StateMag';

import getCommonFeedItemOptions from './getCommonFeedItemOptions';

it('возвращает правильные параметры для поста без категорий', () => {
    const result = getCommonFeedItemOptions(
        articleMock.withPublished().value() as MagPublishedArticle,
    );

    expect(result).toMatchSnapshot();
});

it('возвращает правильные параметры для поста с категориями', () => {
    const result = getCommonFeedItemOptions(
        articleMock.withCategories({ withDefault: true }).withPublished().value() as MagPublishedArticle,
    );

    expect(result.category).toBe('Тесты');
    expect(result.categories).toMatchSnapshot();
});

it('правильно формирует значение для автора, если он не указан в посту', () => {
    const result = getCommonFeedItemOptions({
        ...articleMock.withPublished().value(),
        authorHelp: undefined,
    } as MagPublishedArticle);

    expect(result.author).toBe('Редакция Авто.ру');
});
