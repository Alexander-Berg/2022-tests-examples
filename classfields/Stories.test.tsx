/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import mock from 'auto-core/react/dataDomain/stories/mock';
import type { TStoriesState } from 'auto-core/react/dataDomain/stories/TStories';

import Stories from './Stories';

let store: ThunkMockStore<{ stories: TStoriesState }>;

describe('должен отправить метрику', () => {
    beforeEach(() => {
        store = mockStore({
            stories: mock.stories.value(),
        });
    });

    it('при показе непустого листинга сториз', async() => {
        shallow(
            <Provider store={ store }>
                <Stories/>
            </Provider>,
            { context: contextMock },
        ).dive().dive();

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'stories', 'list', 'show' ]);
    });

    it('при показе превью сториз', async() => {
        const wrapper = shallow(
            <Provider store={ store }>
                <Stories/>
            </Provider>,
            { context: contextMock },
        ).dive().dive();

        wrapper.find('StoryPreview').first().dive()
            .find('InView').simulate('change', true);

        expect(contextMock.metrika.sendPageEvent)
            .toHaveBeenNthCalledWith(2, [ 'stories', 'c1abab30-161c-4dde-a52f-b1d5975b5a49', 'preview', 'show' ]);
    });
});

describe('не должен отправлять метрики', () => {
    beforeEach(() => {
        store = mockStore({
            stories: [],
        });
    });

    it('если листинг сториз пуст', async() => {
        shallow(
            <Provider store={ store }>
                <Stories/>
            </Provider>,
            { context: contextMock },
        ).dive().dive();

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
    });
});
