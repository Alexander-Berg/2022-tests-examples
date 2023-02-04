/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const TagCarousel = require('./TagCarousel');
const { mount } = require('enzyme');

beforeAll(() => {
    global.Object.defineProperty(HTMLElement.prototype, 'offsetWidth', { configurable: true, value: 10 });
});

describe('firstVisibleItemIndex', () => {
    it('должен установить корректный style.transform, ∑ ширин компонентов больше ширины страницы', () => {
        const items = [
            <div key={ 1 }> Тег 1 </div>,
            <div key={ 2 }> Тег 2 </div>,
            <div key={ 3 }> Тег 3 </div>,
            <div key={ 4 }> Тег 4 </div>,
        ];

        global.Element.prototype.getBoundingClientRect = jest.fn(() => ({ width: 20 }));

        const tagCarousel = mount(
            <TagCarousel>
                { items }
            </TagCarousel>
            ,
        );
        expect(tagCarousel.find('.TagCarousel__list').instance().style.transform).toBe('translate3d(0px, 0, 0)');
    });

    it('должен установить корректный style.transform, пренебрегая initialOffset и firstVisibleItemIndex, ' +
        'если ∑ ширин компонентов меньше ширины страницы', () => {
        const items = [
            <div key={ 1 }> Тег 1 </div>,
            <div key={ 2 }> Тег 2 </div>,
            <div key={ 3 }> Тег 3 </div>,
            <div key={ 4 }> Тег 4 </div>,
        ];

        global.Element.prototype.getBoundingClientRect = jest.fn(() => ({ width: 200 }));

        const tagCarousel = mount(
            <TagCarousel
                initialOffset={ -300 }
                firstVisibleItemIndex={ 1 }
            >
                { items }
            </TagCarousel>
            ,
        );
        expect(tagCarousel.find('.TagCarousel__list').instance().style.transform).toBe('translate3d(0px, 0, 0)');
    });

    it('должен установить корректный style.transform, если передан firstVisibleItemIndex', () => {
        const items = [
            <div key={ 1 }> Тег 1 </div>,
            <div key={ 2 }> Тег 2 </div>,
            <div key={ 3 }> Тег 3 </div>,
            <div key={ 4 }> Тег 4 </div>,
        ];

        global.Element.prototype.getBoundingClientRect = jest.fn(() => ({ width: 20 }));

        const tagCarousel = mount(
            <TagCarousel
                className=""
                firstVisibleItemIndex={ 1 }
            >
                { items }
            </TagCarousel>
            ,
        );

        expect(tagCarousel.find('.TagCarousel__list').instance().style.transform).toBe('translate3d(-18px, 0, 0)');
    });
});
