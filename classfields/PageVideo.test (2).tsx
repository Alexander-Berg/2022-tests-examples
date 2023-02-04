/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { journalArticlesMock } from 'auto-core/react/dataDomain/journalArticles/mocks';
import video from 'auto-core/react/dataDomain/video/mocks/video';

import PageVideo from './PageVideo';

const Context = createContextProvider(contextMock);

it('должен открыть и закрыть видео в попапе по клику', () => {
    const journalArticles = journalArticlesMock.value();

    const store = mockStore({
        journalArticles,
        video: video.video,
    });

    const wrapper = mount(
        <Context>
            <Provider store={ store }>
                <PageVideo/>
            </Provider>
        </Context>,
    );
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    wrapper.find('.PageVideoMobileDumb__youtubeItem').first().invoke('onItemClick')(0);

    expect(wrapper.find('.PageVideoMobileDumb__videoFrame')).toExist();
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    wrapper.find('CloseButton').invoke('onClick')();

    expect(wrapper.find('.PageVideoMobileDumb__videoFrame')).not.toExist();
});
