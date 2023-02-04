import { subscriptionMock } from 'auto-core/react/dataDomain/subscriptions/mocks';
import { nbsp } from 'auto-core/react/lib/html-entities';

import getDescriptionString from './getDescriptionString';

it('возвращает корректную строку для оф дилера без доп параметров', () => {
    const subscription = subscriptionMock
        .withSalonViews({ code: 'foo', is_official: true, loyalty_program: false })
        .withParamsDescription({ paramsInfo: [] })
        .withParams({ dealer_id: 'foo' })
        .value();

    const result = getDescriptionString(subscription);
    expect(result).toBe('Официальный дилер');
});

it('возвращает корректную строку если параметров меньше четырех', () => {
    const subscription = subscriptionMock
        .withParamsDescription({
            paramsInfo: [
                { key: 'drive', label: 'Привод', val: 'Полный' },
                { key: 'price_from', label: 'Цена', val: 'от 200 000 ₽' },
                { key: 'run_to', label: 'Пробег', val: 'до 100 000 км' },
            ],
        })
        .value();

    const result = getDescriptionString(subscription);
    expect(result).toBe('Полный, от 200 000 ₽, до 100 000 км');
});

it('возвращает корректную строку если параметров больше четырех', () => {
    const subscription = subscriptionMock
        .withParamsDescription({
            paramsInfo: [
                { key: 'drive', label: 'Привод', val: 'Полный' },
                { key: 'price_from', label: 'Цена', val: 'от 200 000 ₽' },
                { key: 'run_to', label: 'Пробег', val: 'до 100 000 км' },
            ],
            moreCount: 2,
        })
        .value();

    const result = getDescriptionString(subscription);
    expect(result).toBe(`Полный, от 200 000 ₽, до 100 000 км +${ nbsp }2`);
});

it('возвращает корректную строку если в параметрах есть состояние', () => {
    const subscription = subscriptionMock
        .withParamsDescription({
            paramsInfo: [
                { key: 'drive', label: 'Привод', val: 'Полный' },
                { key: 'damage_group', label: 'Состояние', val: 'Неважно' },
            ],
        })
        .value();

    const result = getDescriptionString(subscription);
    expect(result).toBe('Полный, Состояние: неважно');
});
