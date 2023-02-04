const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

// eslint-disable-next-line import/no-restricted-paths
const ListingCarouselItem = require('auto-core/react/components/desktop/ListingCarouselItem/ListingCarouselItem').default;
const CarouselOffersEmptyItem = require('../CarouselOffersEmptyItem/CarouselOffersEmptyItem');
const CarouselOffersFilters = require('./CarouselOffersFilters');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const offerMock = {
    category: 'cars',
    id: '1234',
};

const offers = Array(6).fill(offerMock);

const CarouselOffers = require('./CarouselOffers');

const renderTitle = () => 'Какой-то заголовок';
const renderFooter = () => 'Какой-то футер';

it('должен корректно отрендерить карусель', () => {
    const tree = shallow(
        <CarouselOffers
            itemComponent={ ListingCarouselItem }
            itemEmptyComponent={ CarouselOffersEmptyItem }
            offers={ offers }
            searchID="123"
            title={ renderTitle }
            footer={ renderFooter }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('не должен отрендерить карусель, если нет офферов', () => {
    const tree = shallow(
        <CarouselOffers
            itemComponent={ ListingCarouselItem }
            itemEmptyComponent={ CarouselOffersEmptyItem }
            offers={ [] }
            searchID="123"
            title={ renderTitle }
            footer={ renderFooter }
            hideIfSmallElements={ true }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toEqual('');
});

it('не должен отрендерить карусель, если количество офферов меньше минимума и hideIfSmallElements === true', () => {
    const minCount = 4;
    const offers = Array(3).fill(offerMock);

    const tree = shallow(
        <CarouselOffers
            itemComponent={ ListingCarouselItem }
            itemEmptyComponent={ CarouselOffersEmptyItem }
            offers={ offers }
            searchID="123"
            title={ renderTitle }
            footer={ renderFooter }
            minCount={ minCount }
            hideIfSmallElements={ true }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toEqual('');
});

describe('Заглушки если офферов меньше минимального количества', () => {
    const minCount = 4;
    const offers = Array(3).fill(offerMock);

    const tree = shallow(
        <CarouselOffers
            itemComponent={ ListingCarouselItem }
            itemEmptyComponent={ CarouselOffersEmptyItem }
            offers={ offers }
            searchID="123"
            title={ renderTitle }
            footer={ renderFooter }
            minCount={ minCount }
        />,
        { context: contextMock },
    );

    it('должен заменить недостающие офферы заглушками в десктопе', () => {
        expect(tree.find(ListingCarouselItem)).toHaveLength(3);
        expect(tree.find(CarouselOffersEmptyItem)).toHaveLength(1);
    });

    it('не должен заменить недостающие офферы заглушками в мобилке', () => {
        tree.setProps({
            isMobile: true,
        });
        expect(tree.find(ListingCarouselItem)).toHaveLength(3);
        expect(tree.find(CarouselOffersEmptyItem)).toHaveLength(0);
    });
});

describe('Фильтры', () => {
    const offers = Array(6).fill(offerMock);
    const filters = [
        {
            id: 'all',
            title: 'Все',
            onClick: jest.fn(),
            filter: (offers) => offers,
        },
        {
            id: 'first_two',
            title: 'Первые два',
            onClick: jest.fn(),
            filter: (offers) => offers.slice(0, 2),
        },
    ];

    const tree = shallow(
        <CarouselOffers
            itemComponent={ ListingCarouselItem }
            itemEmptyComponent={ CarouselOffersEmptyItem }
            offers={ offers }
            searchID="123"
            title={ renderTitle }
            footer={ renderFooter }
            filters={ filters }
        />,
        { context: contextMock },
    );

    it('не должен отрендерить фильтры, если shouldDisplayFilters вернет false', () => {
        tree.setProps({
            shouldDisplayFilters: () => false,
        });
        expect(tree.find('CarouselUniversal').dive().find(CarouselOffersFilters)).toHaveLength(0);
    });

    it('должен отрендерить фильтры, если shouldDisplayFilters вернет true', () => {
        tree.setProps({
            shouldDisplayFilters: () => true,
        });
        expect(tree.find('CarouselUniversal').dive().find(CarouselOffersFilters)).toHaveLength(1);
    });

    it('должен отфильтровать офферы при смене фильтра', () => {
        tree.setProps({
            shouldDisplayFilters: () => true,
        });
        expect(tree.find(ListingCarouselItem)).toHaveLength(6);
        tree.setState({
            activeFilter: filters[1],
        });
        expect(tree.find(ListingCarouselItem)).toHaveLength(2);
    });
});
