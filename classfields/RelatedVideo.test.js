const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const responseMock = require('autoru-frontend/mocks/youtubeVideosMock.js');

const RelatedVideo = require('./RelatedVideo');

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const getResource = require('auto-core/react/lib/gateApi').getResource;

it('должен отрендерить блок видео для валидной марки', () => {
    const gateApiPromise = Promise.resolve(responseMock);
    getResource.mockImplementation(() => gateApiPromise);
    const mmmInfo = {
        mark: { id: 'AUDI' },
    };
    const tree = shallow(
        <RelatedVideo
            from="reviews-listing"
            mmmInfo={ mmmInfo }
            title="Популярные видео"
            topCategory="cars"
            key={ 1 }
        />,
        { context: contextMock },
    ).dive();
    tree.find('InView').simulate('change', true);
    return gateApiPromise.then(() => {
        expect(tree.find('InView').dive().find('VideoItem')).toHaveLength(2);
    });
});

const excludedVideoMarks = require('auto-core/data/video/excluded-mark-list.json');

excludedVideoMarks.forEach(mark => {
    it(`не должен отрендерить блок видео для марки ${ mark } из списка исключений`, () => {
        const gateApiPromise = Promise.resolve(responseMock);
        getResource.mockImplementation(() => gateApiPromise);
        const mmmInfo = {
            mark: { id: mark },
        };
        const tree = shallow(
            <RelatedVideo
                from="reviews-listing"
                mmmInfo={ mmmInfo }
                title="Популярные видео"
                topCategory="cars"
                key={ 1 }
            />,
            { context: contextMock },
        ).dive();
        tree.find('InView').simulate('change', true);
        return gateApiPromise.then(() => {
            expect(tree.find('InView').dive().find('RelatedItems')).toEqual({});
        });
    });
});
