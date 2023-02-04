const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const videoList = require('autoru-frontend/mocks/youtubeVideosMock.js');

const VideoCarousel = require('./VideoCarousel');

it('должен отрендерить блок видео для валидной марки', () => {
    const pageParams = {
        mark: 'AUDI',
    };
    const tree = shallow(
        <VideoCarousel
            className="PageCard__videoCarousel"
            params={ pageParams }
            videoList={ videoList }
            isCardPage={ true }
        />,
        { context: contextMock },
    );
    expect(tree).toMatchSnapshot();
});

const excludedVideoMarks = require('auto-core/data/video/excluded-mark-list.json');

excludedVideoMarks.forEach(mark => {
    it(`не должен отрендерить блок видео для марки ${ mark } из списка исключений`, () => {
        const pageParams = {
            mark,
        };
        const tree = shallow(
            <VideoCarousel
                className="PageCard__videoCarousel"
                params={ pageParams }
                videoList={ videoList }
                isCardPage={ true }
            />,
            { context: contextMock },
        );
        expect(tree).toMatchSnapshot();
    });
});
