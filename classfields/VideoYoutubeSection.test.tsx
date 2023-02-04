jest.mock('auto-core/react/actions/scroll');

import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import scrollTo from 'auto-core/react/actions/scroll';
import video from 'auto-core/react/dataDomain/video/mocks/video';

import VideoYoutubeSection from './VideoYoutubeSection';

const ContextProvider = createContextProvider(contextMock);

const videoMock = video.video.items;

const renderWithMiniplayer = () => {
    const wrapper = shallow(
        <ContextProvider>
            <VideoYoutubeSection videoList={ videoMock }/>
        </ContextProvider>,
    ).dive();

    // открываем видео
    wrapper.find('Memo(VideoContentItem)').first().simulate('itemClick', 0, 1);
    // типа скроллим
    wrapper.find('InView').simulate('change', false);

    return wrapper;
};

it('в миниплеере при нажатии на крестик закроет видео', () => {
    const wrapper = renderWithMiniplayer();

    // зокрываем
    wrapper.find('CloseButton').simulate('click');

    expect(wrapper.find('VideoYoutubeSection__videoRow')).not.toExist();
});

it('в миниплеере при нажатии на тайтл подскроллит к видео', () => {
    const wrapper = renderWithMiniplayer();

    // кликоем на татйтл
    wrapper.find('.VideoYoutubeSection__controlContent').simulate('click');

    expect(scrollTo).toHaveBeenCalledWith('VideoYoutubeSection__videoRow', { offset: -120 });
});
