import garagePromoMock from 'auto-core/react/dataDomain/garagePromoAll/mocks';

import getDefaultPromo from './getDefaultPromo';

const ID = 'some_promo';
const PROMOS = [
    garagePromoMock.withId(ID).value(),
    garagePromoMock.withId('2').value(),
    garagePromoMock.withId('3').value(),
];

it('должен вернуть промку из списка, если все ок, параметр строка', () => {
    expect(getDefaultPromo(PROMOS, ID)).toEqual(PROMOS[0]);
});

it('должен вернуть промку из списка, если все ок, параметр массив', () => {
    const param = [ 'show_me_some_other_promo', 'some_promo' ];
    expect(getDefaultPromo(PROMOS, param)).toEqual(PROMOS[0]);
});

it('не должен вернуть промку из списка, параметр строка', () => {
    const param = 'weird_promo_id';
    expect(getDefaultPromo(PROMOS, param)).toEqual(undefined);
});

it('не должен вернуть промку из списка, параметр массив', () => {
    const param = [ 'show_me_some_other_promo', 'weird_promo_id' ];
    expect(getDefaultPromo(PROMOS, param)).toEqual(undefined);
});

it('не должен вернуть промку из списка, так как нет параметра', () => {
    expect(getDefaultPromo(PROMOS, undefined)).toEqual(undefined);
});

it('не должен вернуть промку из списка, так как нет промок', () => {
    expect(getDefaultPromo([], ID)).toEqual(undefined);
});
