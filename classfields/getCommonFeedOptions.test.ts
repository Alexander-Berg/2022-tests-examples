import articleMock from 'auto-core/react/dataDomain/mag/articleMock';
import type { MagPublishedArticle } from 'auto-core/react/dataDomain/mag/StateMag';

import getCommonFeedOptions from './getCommonFeedOptions';

const post = articleMock
    .withPublished()
    .withCategories({ withDefault: true })
    .value() as MagPublishedArticle;

it('возвращает правильные базовые параметры для всех фидов', () => {
    const result = getCommonFeedOptions({
        feedType: 'rss',
        lastPublishArticle: post,
    });

    expect(result).toMatchSnapshot();
});

it('возвращает правильный урл для фида zen', () => {
    const result = getCommonFeedOptions({
        feedType: 'zen',
        lastPublishArticle: post,
    });

    expect(result.feed_url).toBe('https://mag.autoru_frontend.base_domain/feeds/zen/');
});

it('возвращает правильный урл для фида turbo', () => {
    const result = getCommonFeedOptions({
        feedType: 'turbo',
        lastPublishArticle: post,
    });

    expect(result.feed_url).toBe('https://mag.autoru_frontend.base_domain/feeds/turbo/');
});

it('возвращает правильный title и description, если фид категорий', () => {
    const result = getCommonFeedOptions({
        feedType: 'rss',
        lastPublishArticle: post,
        isFeedCategory: true,
    });

    expect(result.title).toMatchSnapshot();
    expect(result.description).toMatchSnapshot();
});

it('возвращает правильную дату публикации, если передан параметр', () => {
    const result = getCommonFeedOptions({
        feedType: 'rss',
        lastPublishArticle: post,
        isPubDate: true,
    });

    expect(result.pubDate).toBe(post.publishAt);
});
