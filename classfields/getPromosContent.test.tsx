import React from 'react';
import { shallow } from 'enzyme';

import garagePromoMock from 'auto-core/react/dataDomain/garagePromoAll/mocks';

import getPromosContent from './getPromosContent';

describe('getPromosContent', () => {
    it('вернет пустой массив так как среди промо рекламы нет с type=SUPER_PROMO', () => {
        const garagePromos = [
            garagePromoMock.value(),
            garagePromoMock.withId('1').value(),
            garagePromoMock.withId('2').value(),
        ];

        const actual = getPromosContent(garagePromos);

        expect(actual).toEqual([]);
    });

    it('вернет массив c одним компонентом так как есть с type=SUPER_PROMO', () => {
        const garagePromos = [
            garagePromoMock.withType('SUPER_PROMO').value(),
            garagePromoMock.withId('1').value(),
            garagePromoMock.withId('2').value(),
        ];

        const actual = getPromosContent(garagePromos);
        const wrapper = shallow(<div>{ actual }</div>);

        expect(wrapper.find('GarageCardMobileTocPromoItem')).toHaveLength(1);
    });
});
