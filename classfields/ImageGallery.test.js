/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ImageGallery = require('./ImageGallery');

const store = mockStore();

it('должен отрисовать widescreen фото, если оно есть', () => {
    const tree = shallow(
        <ImageGallery
            items={ [ { widescreen: 'foo', original: 'bar' } ] }
            startIndex={ 0 }
        />, { context: { store } },
    ).dive();
    const gallery = tree.find('ReactImageGallery').dive();
    const img = gallery.find('img');
    expect(img.first().props().src).toEqual('foo');
});

it('должен отрисовать original фото, если нет widescreen', () => {
    const tree = shallow(
        <ImageGallery
            items={ [ { someOtherSize: 'foo', original: 'bar' } ] }
            startIndex={ 0 }
        />, { context: { store } },
    ).dive();
    const gallery = tree.find('ReactImageGallery').dive();
    const img = gallery.find('img');
    expect(img.first().props().src).toEqual('bar');
});
