/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const { InView } = require('react-intersection-observer');
const VideoItem = require('./VideoItem');
const statApi = require('auto-core/lib/event-log/statApi').default;

const logImmediately = statApi.logImmediately;
const log = statApi.log;

const ContextProvider = createContextProvider(contextMock);

const video = {
    duration_in_seconds: 100,
    previews: { mqdefault: require('autoru-frontend/mockData/images/320').default },
    title: 'Заголовок от видео',
    youtube_iframe_url: 'foo',
    youtube_id: 'youtube_id',
};

afterEach(() => {
    logImmediately.mockClear();
    log.mockClear();
});

it('Должен открывать iframe по клику', () => {
    const wrapper = shallow(
        <ContextProvider>
            <VideoItem
                video={ video }
            />
        </ContextProvider>,
    ).dive();

    const videoItem = wrapper.find('.VideoItem');

    videoItem.simulate('click');

    const videoFrame = wrapper.find('.VideoItem__paranja');
    expect(shallowToJson(videoFrame)).toMatchSnapshot();
});

it('отправляет статистику при показе', () => {
    const wrapper = shallow(
        <ContextProvider>
            <VideoItem
                video={ video }

                pageRequestId="pageRequestId"
                contextPage="contextPage"
                contextBlock="contextBlock"
                searchID="searchID"
                selfType="selfType"
            />
        </ContextProvider>,
    ).dive();

    const observer = wrapper.find(InView);
    observer.simulate('change', true);

    expect(log).toHaveBeenCalledWith({
        original_request_id: 'pageRequestId',
        video_card_show: {
            context_block: 'contextBlock',
            context_page: 'contextPage',
            search_query_id: 'searchID',
            self_type: 'selfType',
            video_id: 'youtube_id',
        },
    });
});

it('НЕ отправляет статистику при скрытии', () => {
    const wrapper = shallow(
        <ContextProvider>
            <VideoItem
                video={ video }

                pageRequestId="pageRequestId"
                contextPage="contextPage"
                contextBlock="contextBlock"
                searchID="searchID"
                selfType="selfType"
            />
        </ContextProvider>,
    ).dive();

    const observer = wrapper.find(InView);
    observer.simulate('change', false);

    expect(log).not.toHaveBeenCalled();
});

it('отправляет статистику при клике', () => {
    const wrapper = shallow(
        <ContextProvider>
            <VideoItem
                video={ video }

                pageRequestId="pageRequestId"
                contextPage="contextPage"
                contextBlock="contextBlock"
                searchID="searchID"
                selfType="selfType"
            />
        </ContextProvider>,
    ).dive();

    const videoItem = wrapper.find('.VideoItem');

    videoItem.simulate('click');

    expect(logImmediately).toHaveBeenCalledWith({
        original_request_id: 'pageRequestId',
        video_card_click: {
            context_block: 'contextBlock',
            context_page: 'contextPage',
            search_query_id: 'searchID',
            self_type: 'selfType',
            video_id: 'youtube_id',
        },
    });
});
