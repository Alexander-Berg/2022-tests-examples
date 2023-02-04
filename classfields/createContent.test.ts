import articleMock from 'auto-core/react/dataDomain/mag/articleMock';
import type { MagPublishedArticle } from 'auto-core/react/dataDomain/mag/StateMag';

import createContent from './createContent';

const post = articleMock.withPublished().value() as MagPublishedArticle;

it('возвращает контент для поста если есть шапка', () => {
    const result = createContent({
        article: post,
        contentHtml: '<div>123</div>',
    });

    expect(result).toMatchSnapshot();
});

it('возвращает контент для поста если нет шапки', () => {
    const result = createContent({
        article: { ...post, cover: undefined },
        contentHtml: '<div>123</div>',
    });

    expect(result).toMatchSnapshot();
});
