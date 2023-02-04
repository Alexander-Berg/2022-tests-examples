/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/actions/scroll', () => {
    return () => {};
});

import React from 'react';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { journalArticleMock, journalArticlesMock } from 'auto-core/react/dataDomain/journalArticles/mocks';
import { Category } from 'auto-core/react/dataDomain/journalArticles/types';
import video from 'auto-core/react/dataDomain/video/mocks/video';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import PageVideo from './PageVideo';

beforeEach(() => {
    contextMock.metrika.sendParams.mockClear();
});

// тест метрики при показе переехал в тест хука useMetrikaArticlesShowEvent

describe('правильно формирует ссылки', () => {
    it('на всей странице', () => {
        const wrapper = renderWrapper();
        const videoJournalSection = wrapper
            .find('.VideoJournalSection')
            .find('.VideoContentItem')
            .find('a').map(a => a.props().href);
        const videoRelatedArticlesSection = wrapper
            .find('.VideoRelatedArticlesSection')
            .find('.VideoRelatedArticlesSection__article')
            .find('a').map(a => a.props().href);
        const videoRelatedArticlesSectionButton = wrapper.find('.VideoRelatedArticlesSection__moreLink')
            .find('a').props().href;
        expect([ ...videoJournalSection, ...videoRelatedArticlesSection, videoRelatedArticlesSectionButton ]).toMatchSnapshot();
    });

    it('в кнопке больше материалом при выбранной марке', () => {
        const searchParams = {
            catalog_filter: [ { mark: 'BMW' } ],
        };
        const wrapper = renderWrapper(searchParams);
        const videoRelatedArticlesSectionButton = wrapper.find('.VideoRelatedArticlesSection__moreLink')
            .find('a').props().href;
        expect(videoRelatedArticlesSectionButton).toMatchSnapshot();
    });

    it('в кнопке больше материалом при выбранной марке-модели', () => {
        const searchParams = {
            catalog_filter: [ { mark: 'BMW', model: 'X5' } ],
        };
        const wrapper = renderWrapper(searchParams);
        const videoRelatedArticlesSectionButton = wrapper.find('.VideoRelatedArticlesSection__moreLink')
            .find('a').props().href;
        expect(videoRelatedArticlesSectionButton).toMatchSnapshot();
    });
});

it('при смене ММП должен поменять урл, обновить выборку и показать загрузку страницы', () => {
    const wrapper = renderWrapper();
    const mmmFilter = wrapper.find('Connect(MMMMultiFilter)');

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    mmmFilter.invoke('onChange')({ catalog_filter: [ { mark: 'AUDI', model: 'A3' } ] });

    expect(contextMock.replaceState).toHaveBeenCalledTimes(1);
    expect(contextMock.replaceState).toHaveBeenCalledWith('link/video/?mark=AUDI&model=A3', { loadData: true });

    expect(wrapper.find('.PageVideoDumb__loaderOverlay')).toExist();
});

it('на пустой выборке при клике на очистить фильтр должен поменять урл на /video и обновить выборку', () => {
    const searchParams = {
        catalog_filter: [ { mark: 'ZENVO' } ],
    };
    const wrapper = renderWrapper(searchParams, true);
    const link = wrapper.find('Link.VideoNotFound__link');

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    link.invoke('onClick')();

    expect(contextMock.replaceState).toHaveBeenCalledTimes(1);
    expect(contextMock.replaceState).toHaveBeenCalledWith('link/video/?', { loadData: true });
});

function renderWrapper(searchParams?: TSearchParameters, hasEmptyStore?: boolean) {
    const journalArticles = journalArticlesMock.withArticles([
        journalArticleMock.withCategory(Category.VIDEO).value(),
        journalArticleMock.withCategory(Category.MAIN).value(),
    ]).value();

    const store = mockStore({
        journalArticles,
        video: {
            ...video.video,
            searchParams,
        },
    });

    const emptyStore = mockStore({
        journalArticles: journalArticlesMock.value(),
        video: {
            items: [],
            searchParams,
        },
    });

    const Context = createContextProvider(contextMock);

    return mount(
        <Context>
            <Provider store={ hasEmptyStore ? emptyStore : store }>
                <PageVideo/>
            </Provider>
        </Context>,
    );
}
