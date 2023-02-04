import * as React from 'react';
import { shallow } from 'enzyme';

import i18n from 'realty-core/view/react/libs/i18n';

import { ProfSearchOfferPriceComponent } from '../';

const defaultOffer = {
    offerType: 'SELL',
    price: {
        currency: 'RUR',
        period: 'WHOLE_LIFE',
        unitPerPart: 'SQUARE_METER',
        valueForWhole: 602296,
        valuePerPart: 38120
    }
};

describe('ProfSearchOfferPriceComponent', () => {
    beforeEach(() => {
        i18n.setLang('ru');
    });

    it('renders with main line', () => {
        const offer = {
            ...defaultOffer,
            price: {
                ...defaultOffer.price,
                valuePerPart: false
            }
        };

        const component = <ProfSearchOfferPriceComponent offer={offer} />;

        const wrapper = shallow(component);

        expect(wrapper.find('.profsearch-offer-price').text()).toBe('602\u00a0296\u00a0₽');
    });

    it('renders without main line', () => {
        const offer = {
            ...defaultOffer,
            price: null
        };

        const component = <ProfSearchOfferPriceComponent offer={offer} />;

        const wrapper = shallow(component);

        expect(wrapper.find('.profsearch-offer-price').text()).toBe('');
    });

    it('renders with second line if offerType is SELL', () => {
        const component = <ProfSearchOfferPriceComponent offer={defaultOffer} />;

        const wrapper = shallow(component);

        expect(wrapper.find('.profsearch-offer-price__secondary').text()).toBe('38\u00a0120\u00a0₽ / м²');
    });

    it('renders without second line if offerType is SELL', () => {
        const offer = {
            ...defaultOffer,
            price: {
                ...defaultOffer.price,
                valuePerPart: false
            }
        };

        const component = <ProfSearchOfferPriceComponent offer={offer} />;

        const wrapper = shallow(component);

        expect(wrapper.find('.profsearch-offer-price__secondary').text()).toBe('');
    });

    it('swaps prices if the "priceType" prop is "PER_METER"', () => {
        const offer = {
            ...defaultOffer,
            price: {
                ...defaultOffer.price,
                valuePerPart: 1000,
                unitPerPart: 'SQUARE_METER'
            }
        };

        const component = (
            <ProfSearchOfferPriceComponent
                offer={offer}
                priceType='PER_METER'
            />
        );

        const wrapper = shallow(component);

        expect(wrapper.childAt(0).text()).toBe('1\u00a0000\u00a0₽ / м²');
        expect(wrapper.find('.profsearch-offer-price__secondary').text()).toBe('602\u00a0296\u00a0₽');
    });

    it('does not swap prices if there is no the "priceType" prop', () => {
        const offer = {
            ...defaultOffer,
            price: {
                ...defaultOffer.price,
                valuePerPart: 1000,
                unitPerPart: 'SQUARE_METER'
            }
        };

        const component = (
            <ProfSearchOfferPriceComponent
                offer={offer}
            />
        );

        const wrapper = shallow(component);

        expect(wrapper.childAt(0).text()).toBe('602\u00a0296\u00a0₽');
        expect(wrapper.find('.profsearch-offer-price__secondary').text()).toBe('1\u00a0000\u00a0₽ / м²');
    });

    it('swaps prices if the "priceType" prop is "PER_ARE"', () => {
        const offer = {
            ...defaultOffer,
            price: {
                ...defaultOffer.price,
                valuePerPart: 1000,
                unitPerPart: 'ARE'
            }
        };

        const component = (
            <ProfSearchOfferPriceComponent
                offer={offer}
                priceType='PER_ARE'
            />
        );

        const wrapper = shallow(component);

        expect(wrapper.childAt(0).text()).toBe('1\u00a0000\u00a0₽ / сот.');
        expect(wrapper.find('.profsearch-offer-price__secondary').text()).toBe('602\u00a0296\u00a0₽');
    });

    it('renders with haggle flag', () => {
        const offer = {
            ...defaultOffer,
            offerType: 'RENT',
            transactionConditionsMap: {
                HAGGLE: true
            }
        };

        const component = <ProfSearchOfferPriceComponent offer={offer} />;

        const wrapper = shallow(component);

        expect(wrapper.find('.profsearch-offer-price__secondary').text()).toBe('Торг');
    });
});
