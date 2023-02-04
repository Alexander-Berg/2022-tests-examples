import { promoFeatureMock, promoFeaturesStateMock } from '../mocks';

import getNonCashbackFeatures from './getNonCashbackFeatures';

it('getNonCashbackFeatures возвращает [], если есть только кэшбэк', () => {
    const state = {
        promoFeatures: promoFeaturesStateMock
            .withFeatures([
                promoFeatureMock.withName('CASHBACK').withLabel('100$').value(),
            ])
            .value(),
    };

    expect(getNonCashbackFeatures(state)).toHaveLength(0);
});

it('getNonCashbackFeatures возвращает непустой массив, но исключает кэшбэк', () => {
    const state = {
        promoFeatures: promoFeaturesStateMock
            .withFeatures([
                promoFeatureMock.withName('Турбо-продажа').value(),
                promoFeatureMock.withName('CASHBACK').withLabel('100$').value(),
            ])
            .value(),
    };

    expect(getNonCashbackFeatures(state)).toHaveLength(1);
    expect(getNonCashbackFeatures(state)[0].name).toBe('Турбо-продажа');
});
