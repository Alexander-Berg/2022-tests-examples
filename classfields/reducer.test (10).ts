import _ from 'lodash';

import { journalArticlesMock, journalArticleMock } from 'auto-core/react/dataDomain/journalArticles/mocks';
import actionTypes from 'auto-core/react/dataDomain/journalArticles/actionTypes';
import { Category } from 'auto-core/react/dataDomain/journalArticles/types';
import { FETCH_PAGE, PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';
import listingArticle from 'auto-core/react/dataDomain/mag/mocks/listingArticle';
import type { JournalArticleAction } from 'auto-core/react/dataDomain/journalArticles/types';
import type { TStateJournalArticles } from 'auto-core/react/dataDomain/journalArticles/TStateJournalArticles';

import reducer from './reducer';

describe('action PAGE_LOADING_SUCCESS', () => {
    it('кладёт данные в стор', () => {

        const state = journalArticlesMock.withArticles([
            journalArticleMock.withCategory(Category.MAIN).value(),
            journalArticleMock.withCategory(Category.TESTDRIVES).value(),
            journalArticleMock.withCategory(Category.EXPLAIN).value(),
        ]).value();

        const action: JournalArticleAction = {
            type: PAGE_LOADING_SUCCESS,
            payload: { journalArticles: state },
        };

        expect(reducer(undefined, action)).toEqual(state);
    });

    it('если пришли данные с undefined не должен поменять стейт', () => {

        const expectedState = journalArticlesMock.withArticles([
            journalArticleMock.withCategory(Category.MAIN).value(),
            journalArticleMock.withCategory(Category.EXPLAIN).value(),
        ]).value() as Partial<TStateJournalArticles>;

        const state = _.cloneDeep(expectedState);

        state[Category.TESTDRIVES] = undefined;

        const action: JournalArticleAction = {
            type: PAGE_LOADING_SUCCESS,
            payload: { journalArticles: state },
        };

        expect(reducer(undefined, action)).toEqual(expectedState);
    });
});

it('ставит статус загрузки у рубрики в сторе', () => {

    const state = journalArticlesMock.withArticles([
        journalArticleMock.withCategory(Category.MAIN).value(),
        journalArticleMock.withCategory(Category.TESTDRIVES).value(),
        journalArticleMock.withCategory(Category.EXPLAIN).value(),
    ]).value();

    const action: JournalArticleAction = {
        type: actionTypes.JOURNAL_ARTICLES_FETCHING,
        payload: {
            category: Category.MAIN,
        },
    };

    const expectedState = _.cloneDeep(state);
    expectedState.main.isFetching = true;

    expect(reducer(state, action)).toEqual(expectedState);
});

it('ставит статус ошибки у рубрики в сторе', () => {

    const state = journalArticlesMock.withArticles([
        journalArticleMock.withCategory(Category.MAIN).value(),
        journalArticleMock.withCategory(Category.EXPLAIN).value(),
    ]).value();

    const action: JournalArticleAction = {
        type: actionTypes.JOURNAL_ARTICLES_REJECTED,
        payload: {
            category: Category.MAIN,
        },
    };

    const expectedState = _.cloneDeep(state);
    expectedState.main.isError = true;

    expect(reducer(state, action)).toEqual(expectedState);
});

it('кладёт в рубрику статьи', () => {

    const state = journalArticlesMock.withArticles([
        journalArticleMock.withCategory(Category.TESTDRIVES).value(),
        journalArticleMock.withCategory(Category.EXPLAIN).value(),
    ]).value();

    const action: JournalArticleAction = {
        type: actionTypes.JOURNAL_ARTICLES_RESOLVED,
        payload: {
            data: {
                articles: [ listingArticle, listingArticle ],
                pagination: { totalArticlesCount: 100 },
            },
            category: Category.MAIN,
        },
    };

    const expectedState = _.cloneDeep(state);
    expectedState.main = {
        data: {
            articles: [ listingArticle, listingArticle ],
            pagination: { totalArticlesCount: 100 },
        },
        isError: false,
        isFetching: false,
    };

    expect(reducer(state, action)).toEqual(expectedState);
});

it('кладёт в рубрику статьи в конец', () => {
    const article = journalArticleMock.withCategory(Category.MAIN).value();
    const state = journalArticlesMock.withArticles([ article ]).value();

    const action: JournalArticleAction = {
        type: actionTypes.JOURNAL_MORE_ARTICLES_RESOLVED,
        payload: {
            data: {
                articles: [ listingArticle, listingArticle ],
                pagination: { totalArticlesCount: 100 },
            },
            category: Category.MAIN,
        },
    };

    const expectedState = _.cloneDeep(state);
    expectedState.main = {
        data: {
            articles: [ article, listingArticle, listingArticle ],
            pagination: { totalArticlesCount: 100 },
        },
        isError: false,
        isFetching: false,
    };

    expect(reducer(state, action)).toEqual(expectedState);
});

it('при загрузке загрузке страницы должен поставить статус загрузки у рубрик в сторе', () => {
    const state = journalArticlesMock.withArticles([
        journalArticleMock.withCategory(Category.MAIN).value(),
        journalArticleMock.withCategory(Category.VIDEO).value(),
    ]).value();

    const action: JournalArticleAction = {
        type: FETCH_PAGE,
    };

    const expectedState = _.cloneDeep(state);
    expectedState.main.isFetching = true;
    expectedState.video.isFetching = true;
    expectedState.explain.isFetching = true;
    expectedState.uchebnik.isFetching = true;
    expectedState.testdrives.isFetching = true;

    expect(reducer(state, action)).toEqual(expectedState);
});
