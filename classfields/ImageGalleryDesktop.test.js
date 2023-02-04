const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ImageGalleryDesktop = require('./ImageGalleryDesktop');

describe('отображение фото', () => {
    it('должен отрисовать widescreen фото, если оно есть', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { widescreen: 'foo', original: 'bar' } ] } store={ mockStore({}) }
            />,
        ).dive();
        const img = tree.find('img');
        expect(img.first().props().src).toEqual('foo');
    });

    it('должен отрисовать original фото, если нет widescreen', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { someOtherSize: 'foo', original: 'bar' } ] } store={ mockStore({}) }
            />,
        ).dive();
        const img = tree.find('img');
        expect(img.first().props().src).toEqual('bar');
    });
});

describe('листание фото', () => {
    it('должен показать следующее фото при листании вперед', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] } store={ mockStore({}) }
            />,
        ).dive();
        tree.find('.ImageGalleryDesktop__right-nav').simulate('click');
        expect(tree.state().currentIndex).toBe(1);
    });

    it('должен показать первое фото при листании вперед на последнем фото', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] } store={ mockStore({}) }
            />,
        ).dive();
        tree.setState({ currentIndex: 1 });
        tree.find('.ImageGalleryDesktop__right-nav').simulate('click');
        expect(tree.state().currentIndex).toBe(0);
    });

    it('должен показать предыдущее фото при листании назад', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] } store={ mockStore({}) }
            />,
        ).dive();
        tree.setState({ currentIndex: 1 });
        tree.find('.ImageGalleryDesktop__left-nav').simulate('click');
        expect(tree.state().currentIndex).toBe(0);
    });

    it('должен показать последнее фото при листании вперед на первом фото', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] } store={ mockStore({}) }
            />,
        ).dive();
        tree.find('.ImageGalleryDesktop__left-nav').simulate('click');
        expect(tree.state().currentIndex).toBe(1);
    });
});

it('должен отрендерить то, что передали в renderItem, если оно есть', () => {
    const tree = shallow(
        <ImageGalleryDesktop
            items={ [
                { widescreen: 'foo', thumbnail: 'bar', renderItem: () => 'фотка', renderThumbInner: () => 'мелкая фотка' },
                { widescreen: 'foo2', thumbnail: 'bar2' },
            ] }
            store={ mockStore({}) }
        />,
    ).dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен вызвать handleFullscreenEnter при переходе в фуллскрин', () => {
    const handleFullscreenEnter = jest.fn();
    const tree = shallow(
        <ImageGalleryDesktop
            items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] }
            handleFullscreenEnter={ handleFullscreenEnter }
            store={ mockStore({}) }
        />,
    ).dive();
    tree.find('.ImageGalleryDesktop__fullscreen-button').simulate('click');
    expect(handleFullscreenEnter).toHaveBeenCalled();
});

it('должен вызвать onSlide при переходе в фуллскрин', () => {
    const onSlide = jest.fn();
    const tree = shallow(
        <ImageGalleryDesktop
            items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] }
            onSlide={ onSlide }
            store={ mockStore({}) }
        />,
    ).dive();
    tree.find('.ImageGalleryDesktop__right-nav').simulate('click');
    expect(onSlide).toHaveBeenCalledWith(1);
});

describe('ховер по тумбе', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    it('поменяет активный слайд если включен slideOnThumbnailHover', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] }
                store={ mockStore({}) }
                slideOnThumbnailHover={ true }
            />,
        ).dive();

        const secondThumb = tree.find('.ImageGalleryDesktop__thumb-container[data-index=1]');
        expect(secondThumb.hasClass('ImageGalleryDesktop__thumb-container_active')).toBe(false);
        secondThumb.simulate('mouseenter', { currentTarget: { getAttribute: () => '1' } });

        jest.advanceTimersByTime(50);

        const updatedSecondThumb = tree.find('.ImageGalleryDesktop__thumb-container[data-index=1]');
        expect(updatedSecondThumb.hasClass('ImageGalleryDesktop__thumb-container_active')).toBe(true);
    });

    it('не поменяет активный слайд если выключен slideOnThumbnailHover', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] }
                store={ mockStore({}) }
                slideOnThumbnailHover={ false }
            />,
        ).dive();

        const secondThumb = tree.find('.ImageGalleryDesktop__thumb-container[data-index=1]');
        expect(secondThumb.hasClass('ImageGalleryDesktop__thumb-container_active')).toBe(false);
        secondThumb.simulate('mouseenter', { currentTarget: { getAttribute: () => '1' } });

        jest.advanceTimersByTime(50);

        const updatedSecondThumb = tree.find('.ImageGalleryDesktop__thumb-container[data-index=1]');
        expect(updatedSecondThumb.hasClass('ImageGalleryDesktop__thumb-container_active')).toBe(false);
    });

    it('не поменяет активный слайд во время вращения панорамы', () => {
        const tree = shallow(
            <ImageGalleryDesktop
                items={ [ { widescreen: 'foo' }, { widescreen: 'bar' } ] }
                store={ mockStore({}) }
                slideOnThumbnailHover={ true }
                isPanoramaDragging={ true }
            />,
        ).dive();

        const secondThumb = tree.find('.ImageGalleryDesktop__thumb-container[data-index=1]');
        expect(secondThumb.hasClass('ImageGalleryDesktop__thumb-container_active')).toBe(false);
        secondThumb.simulate('mouseenter', { currentTarget: { getAttribute: () => '1' } });

        jest.advanceTimersByTime(50);

        const updatedSecondThumb = tree.find('.ImageGalleryDesktop__thumb-container[data-index=1]');
        expect(updatedSecondThumb.hasClass('ImageGalleryDesktop__thumb-container_active')).toBe(false);
    });
});
