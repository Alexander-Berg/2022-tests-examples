import { promoFeatureMock, promoFeaturesStateMock } from '../mocks';

import getCashback from './getCashback';

it('getCashback возвращает строку `100 руб.`, если есть нужная фича', () => {
    const state = {
        promoFeatures: promoFeaturesStateMock
            .withFeatures([
                promoFeatureMock.withName('CASHBACK').withLabel('100$').value(),
                promoFeatureMock.value(),
            ])
            .value(),
    };

    expect(getCashback(state)).toEqual('100$');
});

it('getCashback возвращает undefined, если нет нужной фичи', () => {
    const state = {
        promoFeatures: promoFeaturesStateMock
            .withFeatures([
                promoFeatureMock.withName('Турбо-продажа').value(),
                promoFeatureMock.value(),
            ])
            .value(),
    };

    expect(getCashback(state)).toBeUndefined();
});
