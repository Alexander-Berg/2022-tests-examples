const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const ImageGallery = require('./ImageGallery').default;

const galleryItems = [ {
    original: '...',
    thumbnail: '...',
    index: 0,
}, {
    original: '...',
    thumbnail: '...',
    index: 1,
} ];

describe('ImageGallery', () => {
    it('рендерит ImageFullscreenGallery c visible', () => {
        const tree = shallow(
            <ImageGallery
                items={ galleryItems }
            />,
        );

        tree.find('.ImageGallery__item').at(0).simulate('click', { currentTarget: { getAttribute: () => 0 } });
        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});
