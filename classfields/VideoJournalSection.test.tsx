/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { mount } from 'enzyme';
import _ from 'lodash';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import { journalArticleMock } from 'auto-core/react/dataDomain/journalArticles/mocks';
import { Category } from 'auto-core/react/dataDomain/journalArticles/types';

import VideoJournalSection from './VideoJournalSection';

const Context = createContextProvider(contextMock);

describe('кнопка показать ещё', () => {
    // временно, пока бэк не прикрутить оффсет для ручки(((
    it('должна отсутствовать при нерелевантной выдаче', () => {
        const journalArticles = [
            journalArticleMock.withCategory(Category.VIDEO).value(),
        ];

        const wrapper = mount(
            <Context>
                <VideoJournalSection
                    videoJournalArticles={ journalArticles }
                    hasRelevantVideos={ false }
                />
            </Context>,
        );

        expect(wrapper.find('Button.VideoJournalSection__button')).not.toExist();
    });

    it('должна отсутствовать при маленькой релевантной выдаче', () => {
        const journalArticles = [
            journalArticleMock.withCategory(Category.VIDEO).value(),
        ];

        const wrapper = mount(
            <Context>
                <VideoJournalSection
                    videoJournalArticles={ journalArticles }
                    hasRelevantVideos={ true }
                />
            </Context>,
        );

        expect(wrapper.find('Button.VideoJournalSection__button')).not.toExist();
    });

    it('должна отсутствовать если страниц больше нет', () => {
        const journalArticles = [
            journalArticleMock.withCategory(Category.VIDEO).value(),
        ];

        const wrapper = mount(
            <Context>
                <VideoJournalSection
                    videoJournalArticles={ journalArticles }
                    hasRelevantVideos={ true }
                    hasNextPage={ false }
                />
            </Context>,
        );

        expect(wrapper.find('Button.VideoJournalSection__button')).not.toExist();
    });

    it('должна быть, если статей больше 6, есть страницы дальше и это релевантная выдача', () => {
        const journalArticles = _.times(7, () => journalArticleMock.withCategory(Category.VIDEO).value());

        const wrapper = mount(
            <Context>
                <VideoJournalSection
                    videoJournalArticles={ journalArticles }
                    hasRelevantVideos={ true }
                    hasNextPage={ true }
                />
            </Context>,
        );

        expect(wrapper.find('Button.VideoJournalSection__button')).toExist();
    });

    it('должна вызывать функцию при клике на неё', () => {
        const journalArticles = _.times(7, () => journalArticleMock.withCategory(Category.VIDEO).value());

        const onFetchMoreClick = jest.fn();

        const wrapper = mount(
            <Context>
                <VideoJournalSection
                    videoJournalArticles={ journalArticles }
                    hasRelevantVideos={ true }
                    hasNextPage={ true }
                    onFetchMoreClick={ onFetchMoreClick }
                />
            </Context>,
        );
        const button = wrapper.find('Button.VideoJournalSection__button');

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        button.invoke('onClick')();

        expect(onFetchMoreClick).toHaveBeenCalledTimes(1);
    });
});
