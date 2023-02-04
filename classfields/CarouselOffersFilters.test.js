const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const Button = require('auto-core/react/components/islands/Button');

const CarouselOffersFilters = require('./CarouselOffersFilters');

const FILTERS = [
    {
        id: 'all',
        title: 'Все',
    },
    {
        id: 'new',
        title: 'Новые',
    },
    {
        id: 'used',
        title: 'С пробегом',
    },
];

const ACTIVE_FILTER = FILTERS[0];

it('должен корректно отрендериться', () => {
    const tree = shallow(
        <CarouselOffersFilters
            filters={ FILTERS }
            activeFilter={ ACTIVE_FILTER }
            onFilterClick={ jest.fn }
            buttonSize={ Button.SIZE.M }
            buttonColor={ Button.COLOR.WHITE }
            buttonWidth={ Button.WIDTH.DEFAULT }
            isMobile={ false }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('не должен содержать класс CarouselOffersFilters_mobile при isMobile === false', () => {
    const tree = shallow(
        <CarouselOffersFilters
            filters={ FILTERS }
            activeFilter={ ACTIVE_FILTER }
            onFilterClick={ jest.fn }
            buttonSize={ Button.SIZE.M }
            buttonColor={ Button.COLOR.WHITE }
            buttonWidth={ Button.WIDTH.DEFAULT }
            isMobile={ false }
        />,
    );
    expect(tree.find('.CarouselOffersFilters_mobile')).toHaveLength(0);
});

it('должен содержать класс CarouselOffersFilters_mobile при isMobile === true', () => {
    const tree = shallow(
        <CarouselOffersFilters
            filters={ FILTERS }
            activeFilter={ ACTIVE_FILTER }
            onFilterClick={ jest.fn }
            buttonSize={ Button.SIZE.M }
            buttonColor={ Button.COLOR.WHITE }
            buttonWidth={ Button.WIDTH.DEFAULT }
            isMobile={ true }
        />,
    );
    expect(tree.find('.CarouselOffersFilters_mobile')).toHaveLength(1);
});
