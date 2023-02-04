import * as React from 'react';
import { shallow } from 'enzyme';
import i18n from 'realty-core/view/react/libs/i18n';

import { ProfSearchOfferArea } from '../';

const defaultOffer = {
    area: {
        value: 10,
        unit: 'SQUARE_METER'
    },
    lot: {
        lotArea: {
            value: 100,
            unit: 'ARE'
        }
    }
};

describe('ProfSearchOfferArea', () => {
    beforeEach(() => {
        i18n.setLang('ru');
    });

    it('renders area', () => {
        const component = <ProfSearchOfferArea offer={defaultOffer} />;
        const result = shallow(component);

        const childrenCount = result
            .children()
            .length;

        const text = result
            .childAt(0)
            .text();

        expect(childrenCount).toBe(1);
        expect(text).toBe('10\u00a0м²');
    });

    it('renders lot area if offerCategory is HOUSE', () => {
        const offer = {
            ...defaultOffer,
            offerCategory: 'HOUSE'
        };

        const component = <ProfSearchOfferArea offer={offer} />;

        const text = shallow(component)
            .childAt(1)
            .text();

        expect(text).toBe('участок100\u00a0соток');
    });

    it('renders living area', () => {
        const offer = {
            ...defaultOffer,
            offerCategory: 'ROOMS',
            livingSpace: {
                value: 15,
                unit: 'HECTARE'
            }
        };

        const component = <ProfSearchOfferArea offer={offer} />;
        const result = shallow(component);

        const childrenCount = result
            .children()
            .length;

        const text = result
            .childAt(0)
            .text();

        expect(childrenCount).toBe(1);
        expect(text).toBe('15\u00a0га');
    });

    it('does not render lot area if there is no the "lot" field', () => {
        const offer = {
            ...defaultOffer,
            offerCategory: 'HOUSE',
            lot: undefined
        };
        const component = <ProfSearchOfferArea offer={offer} />;

        const text = shallow(component).text();

        expect(text.includes('100\u00a0соток')).toBe(false);
    });
});
