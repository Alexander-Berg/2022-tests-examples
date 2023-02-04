import React from 'react';
import { shallow } from 'enzyme';
import type { ShallowWrapper } from 'enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import SalePricePopupEditContent from './SalePricePopupEditContent';

const priceChangeMock = jest.fn();
const defaultProps = {
    onPriceChange: priceChangeMock,
    offer: cloneOfferWithHelpers({})
        .withCategory('cars')
        .withPrice(750000)
        .withDiscountOptions({ tradein: 1000, credit: 2000, max_discount: 5000 })
        .value(),
};

describe('должен вызвать колбэк с правильными аргументами, если', () => {

    it('изменилась только цена', () => {
        const tree = shallowRenderComponent();
        changeInputs(tree, [
            { name: 'price', value: 600000 },
        ]);
        tree.find('Button').simulate('click');

        expect(priceChangeMock).toHaveBeenCalledWith({ price: 600000 });
    });

    it('изменились только скидки', () => {
        const tree = shallowRenderComponent();
        changeInputs(tree, [
            { name: 'insuranceDiscount', value: 777 },
            { name: 'tradeInDiscount', value: '' },
        ]);
        tree.find('Button').simulate('click');

        expect(priceChangeMock).toHaveBeenCalledWith({
            creditDiscount: 2000,
            tradeInDiscount: 0,
            maxDiscount: 5000,
            insuranceDiscount: 777,
        });
    });

    it('изменились и скидки, и цена', () => {
        const tree = shallowRenderComponent();
        changeInputs(tree, [
            { name: 'price', value: 600000 },
            { name: 'insuranceDiscount', value: 777 },
            { name: 'tradeInDiscount', value: '' },
        ]);
        tree.find('Button').simulate('click');

        expect(priceChangeMock).toHaveBeenCalledWith({
            price: 600000,
            creditDiscount: 2000,
            tradeInDiscount: 0,
            maxDiscount: 5000,
            insuranceDiscount: 777,
        });
    });

});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <SalePricePopupEditContent { ...props }/>,
    );
}

function changeInputs(tree: ShallowWrapper, inputs: Array<{ name: string; value: number | ''}>) {
    const inputsList = tree.find('TextInputInteger');
    inputs.forEach(input => {
        inputsList.findWhere(node => node.key() === input.name).simulate('change', input.value, { name: input.name });
    });
}
