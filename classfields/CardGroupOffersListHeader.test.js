const React = require('react');
const { shallow } = require('enzyme');

const { nbsp } = require('auto-core/react/lib/html-entities');

const CardGroupOffersListHeader = require('./CardGroupOffersListHeader');

const PRICE_RANGE = {
    min: {
        price: 2572700,
    },
    max: {
        price: 2992300,
    },
};

it('должен правильно выводить текст заголовка для нескольких офферов', () => {
    const tree = shallow(
        <CardGroupOffersListHeader
            priceRange={ PRICE_RANGE }
            offersCount={ 2 }
        />,
    );
    expect(
        tree.find('.CardGroupOffersHeader__title').text().replace(new RegExp(nbsp, 'g'), ' '),
    ).toContain('2 предложения от 2 572 700 до 2 992 300 ₽');
});

it('должен правильно выводить текст заголовка для одного оффера', () => {
    const tree = shallow(
        <CardGroupOffersListHeader
            priceRange={ PRICE_RANGE }
            offersCount={ 1 }
        />,
    );
    expect(
        tree.find('.CardGroupOffersHeader__title').text().replace(new RegExp(nbsp, 'g'), ' '),
    ).toContain('Одно предложение');
});

it('должен правильно выводить текст заголовка, если нет офферов', () => {
    const tree = shallow(
        <CardGroupOffersListHeader
            priceRange={ PRICE_RANGE }
            offersCount={ 0 }
        />,
    );
    expect(
        tree.find('.CardGroupOffersHeader__title').text().replace(new RegExp(nbsp, 'g'), ' '),
    ).toContain('В выбранном регионе нет подходящих предложений');
});
