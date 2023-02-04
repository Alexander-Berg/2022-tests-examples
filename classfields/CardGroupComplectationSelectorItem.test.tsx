import React from 'react';
import { shallow } from 'enzyme';

import { thinsp } from 'auto-core/react/lib/html-entities';

import CardGroupComplectationSelectorItem from './CardGroupComplectationSelectorItem';

const COMPLECTATION_NAME = 'Comfort';
const OPTIONS_COUNT = 15;
const PRICE_FROM = 3400000;

it('не должен содержать класс selected при isSelected !== true', () => {
    const tree = shallow(
        <CardGroupComplectationSelectorItem
            id="1"
            name={ COMPLECTATION_NAME }
            optionsCount={ OPTIONS_COUNT }
            priceFrom={ PRICE_FROM }
            isSelected={ false }
        />,
    );
    expect(tree.find('.CardGroupComplectationSelectorItem_selected')).toHaveLength(0);
});

it('должен содержать класс selected при isSelected === true', () => {
    const tree = shallow(
        <CardGroupComplectationSelectorItem
            id="1"
            name={ COMPLECTATION_NAME }
            optionsCount={ OPTIONS_COUNT }
            priceFrom={ PRICE_FROM }
            isSelected={ true }
        />,
    );
    expect(tree.find('.CardGroupComplectationSelectorItem_selected')).toHaveLength(1);
});

it('не должен отрендерить количество опций при их отсутствии', () => {
    const tree = shallow(
        <CardGroupComplectationSelectorItem
            id="1"
            name={ COMPLECTATION_NAME }
            optionsCount={ 0 }
            priceFrom={ PRICE_FROM }
            isSelected={ true }
        />,
    );
    expect(tree.find('.CardGroupComplectationSelectorItem__optionsCount')).toHaveLength(0);
});

it('должен отрендерить цену от', () => {
    const tree = shallow(
        <CardGroupComplectationSelectorItem
            id="1"
            name={ COMPLECTATION_NAME }
            optionsCount={ OPTIONS_COUNT }
            priceFrom={ PRICE_FROM }
            isSelected={ true }
        />,
    );
    expect(
        tree.find('Price').dive().text().replace(new RegExp(thinsp, 'g'), ' '),
    ).toContain('от 3 400 000 ₽');
});

it('должен отрендерить ссылку с метрикой', () => {
    const tree = shallow(
        <CardGroupComplectationSelectorItem
            id="1"
            name={ COMPLECTATION_NAME }
            optionsCount={ OPTIONS_COUNT }
            priceFrom={ PRICE_FROM }
            url="#"
            isSelected={ true }
        />,
    );
    expect(tree.find('Link').prop('metrika')).toBe('about_model,options,load_listing');
});
