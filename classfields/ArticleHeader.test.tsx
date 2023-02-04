import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import articleMock from 'auto-core/react/dataDomain/mag/articleMock';
import configMock from 'auto-core/react/dataDomain/config/mock';

import type { ArticleHeaderProps } from './ArticleHeader';
import ArticleHeader from './ArticleHeader';

const initialArticle = articleMock.value();

function shallowRenderComponent(props: ArticleHeaderProps) {
    return shallow(
        <ArticleHeader { ...props }/>,
        {
            context: {
                ...contextMock,
                store: mockStore({
                    config: configMock.value(),
                }),
            },
        },
    );
}

it('должен отрендерить вариант с предыдущей статьёй', () => {
    const wrapper = shallowRenderComponent({
        title: initialArticle.title,
        withLinkComments: true,
        relatedArticle: initialArticle.before,
    });

    expect(wrapper.find('.ArticleHeader__previousArticle')
        .prop('href'))
        .toContain(`linkMag/mag-article/?article_id=${ initialArticle.before?.urlPart }`);
});

it('должен отрендерить обычный вариант с блоком "Поделиться"', () => {
    const wrapper = shallowRenderComponent({
        title: initialArticle.title,
    });

    expect(wrapper.find('Memo(Share)')).toHaveLength(1);
    expect(wrapper).toMatchSnapshot();
});
