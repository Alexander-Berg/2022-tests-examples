const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const Link = require('auto-core/react/components/islands/Link/Link');
const CarouselUniversal = require('./CarouselUniversal');

const renderTitle = () => 'Какой-то заголовок';
const renderFooter = () => 'Какой-то футер';
const renderCustomHeaderButtons = () => <div className="CustomHeaderButtons"> Какие-то кнопки</div>;

const itemsList = Array(10).fill('Здесь могла бы быть ваша реклама').map(
    (item, index) => <div key={ index } className="someClassName">{ item }</div>,
);

it('должен отрендерить карусель в десктопной версии', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить карусель в мобильной версии', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            isMobile={ true }
            title={ renderTitle }
            footer={ renderFooter }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить карусель c заголовком-ссылкой при hasMore === true', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
            hasMore={ true }
            pageLink="/"
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find(Link)).toHaveLength(1);
});

it('должен отрендерить карусель c без ссылки в заголовке при hasMore === false', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
            hasMore={ false }
            pageLink="/"
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find(Link)).toHaveLength(0);
});

it('должен отрендерить карусель c кнопками навигации в десктопной версии', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find('.CarouselUniversal__navButton')).toHaveLength(1);
});

it('должен отрендерить карусель без кнопок навигации в десктопной версии при showNavigation === false', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
            showNavigation={ false }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find('.CarouselUniversal__navButton')).toHaveLength(0);
});

it('должен отрендерить карусель без кнопок навигации в мобильной версии', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            isMobile={ true }
            title={ renderTitle }
            footer={ renderFooter }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find('.CarouselUniversal__navButton')).toHaveLength(0);
});

it('должен отрендерить карусель без кастомной навигации и без кастомного бара в десктопной версии', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find('.CustomHeaderButtons')).toHaveLength(0);
    expect(tree.find('.CustomBarClass')).toHaveLength(0);
});

it('должен отрендерить карусель без кастомной навигации и без кастомного бара в мобильной версии', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
            isMobile={ true }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find('.CustomHeaderButtons')).toHaveLength(0);
    expect(tree.find('.CustomBarClass')).toHaveLength(0);
});

it('должен отрендерить карусель c кастомной навигацией в десктопной версии', () => {
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
            renderCustomHeaderButtons={ renderCustomHeaderButtons }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find('.CustomHeaderButtons')).toHaveLength(1);
});

it('должен отрендерить карусель c кастомным баром в десктопной версии', () => {
    const renderCustomBar = () => <div className="CustomBarClass"> Какой-то текст </div>;
    const tree = shallow(
        <CarouselUniversal
            size="l"
            dir="horizontal"
            minCount={ 4 }
            initialOffset={ 0 }
            onNavigation={ jest.fn }
            title={ renderTitle }
            footer={ renderFooter }
            renderCustomBar={ renderCustomBar }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find('.CustomBarClass')).toHaveLength(1);
});

it('должен отрендерить карусель с кастомными кнопками в десктопе', () => {
    const CustomButton = () => <div className="CustomButton">туда-сюда</div>;
    const tree = shallow(
        <CarouselUniversal
            navigationButton={ CustomButton }
        >
            { itemsList }
        </CarouselUniversal>,
    );
    expect(tree.find('CustomButton')).toHaveLength(1);
});
