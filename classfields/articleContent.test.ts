import articleMock from 'auto-core/react/dataDomain/mag/articleMock';

import articleContent from './articleContent';

const initialArticle = articleMock.withBlocks({ withDefaultBlocks: true }).value();

it('должен отрендерить вариант, когда нет блоков', () => {
    const wrapper = articleContent(
        [],
        initialArticle.mainImage,
    );

    expect(wrapper.articleBody).toHaveLength(0);
});

it('должен отрендерить обычный вариант с дефолтными блоками', () => {
    const wrapper = articleContent(
        initialArticle.blocks || [],
        initialArticle.mainImage,
    );

    expect(wrapper).toMatchSnapshot();
});
