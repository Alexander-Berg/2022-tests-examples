import MockDate from 'mockdate';

import { h1, title, description, footerText } from '../index';

import mocks from './mocks/mocks.index';

describe('mortgage program seo texts', () => {
    beforeEach(() => {
        MockDate.set('2021-01-01');
    });

    afterEach(() => {
        MockDate.reset();
    });

    test('get mortgage program title', () => {
        const calculatorTitle = title(mocks);

        // eslint-disable-next-line max-len
        expect(calculatorTitle).toBe('Ипотека на строящееся жильё от Альфа-Банка 2021 в Москве и МО – ставки и актуальные условия покупки квартиры в ипотеку от Альфа-Банка на Яндекс.Недвижимости');
    });

    test('get mortgage program description', () => {
        const calculatorDescription = description(mocks);

        // eslint-disable-next-line max-len
        expect(calculatorDescription).toBe('Выгодные условия на покупку квартиры от Альфа-Банка – Ипотека на строящееся жильё 2021 для покупки квартиры в новостройке или на вторичном рынке в Москве и МО');
    });

    test('get mortgage program h1', () => {
        const calculatorH1 = h1(mocks);

        // eslint-disable-next-line max-len
        expect(calculatorH1).toBe('Ипотека на строящееся жильё от Альфа-Банка в Москве и МО');
    });

    test('get mortgage program footer', () => {
        const calculatorFooter = footerText(mocks);

        // eslint-disable-next-line max-len
        expect(calculatorFooter).toBe('Ипотека на строящееся жильё от Альфа-Банка в Москве и МО - оформите ипотеку на недвижимость с минимальным первоначальным взносом от 10% и ставкой от 8.79% на срок до 30 лет. Получите ипотеку на недвижимость в Москве и МО со скидкой на Яндекс Недвижимости');
    });
});
