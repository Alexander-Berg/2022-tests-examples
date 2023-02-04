import { renderHook } from '@testing-library/react-hooks';

import metrika from 'autoru-frontend/mocks/metrikaMock';

import { journalArticleMock } from 'auto-core/react/dataDomain/journalArticles/mocks';

import useMetrikaArticlesShowEvent from './useMetrikaArticlesShowEvent';

beforeEach(() => {
    metrika.sendParams.mockClear();
});

it('отправит метрику при загрузке страницы', () => {
    const articles = {
        videoJournalArticles: [ journalArticleMock.withUrlPart('test1').value() ],
        mainJournalArticles: [ journalArticleMock.withUrlPart('test2').value() ],
    };

    renderHook(() => useMetrikaArticlesShowEvent(articles, metrika));

    expect(metrika.sendParams).toHaveBeenCalledTimes(2);
    expect(metrika.sendParams.mock.calls).toMatchSnapshot();
});

it('не отправит метрику если по какой-то статье уже была отправлена', () => {
    const articles = {
        videoJournalArticles: [ journalArticleMock.withUrlPart('test1').value() ],
        mainJournalArticles: [ journalArticleMock.withUrlPart('test2').value() ],
    };

    const { rerender } = renderHook(() => useMetrikaArticlesShowEvent(articles, metrika));

    articles.videoJournalArticles = [
        journalArticleMock.withUrlPart('test1').value(), journalArticleMock.withUrlPart('test3').value(),
    ];
    articles.mainJournalArticles = [
        journalArticleMock.withUrlPart('test2').value(), journalArticleMock.withUrlPart('test4').value(),
    ];

    rerender();

    // если бы метрика отправлялась повторно, то вызовов было бы 6
    expect(metrika.sendParams).toHaveBeenCalledTimes(4);
});
