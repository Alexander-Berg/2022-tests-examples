import MockDate from 'mockdate';

import { h1, title, description, footerText } from '../index';

import mocks from './mocks/mocks.index';

describe('mortgage search seo texts check', () => {
    beforeEach(() => {
        MockDate.set('2021-01-01');
    });

    afterEach(() => {
        MockDate.reset();
    });

    describe('mortgage search seo texts with first moc', () => {
        test('get mortgage search title', () => {
            const calculatorTitle = title(mocks.firstMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorTitle).toBe('Ипотека в Санкт-Петербурге и ЛО — условия по 75 ипотечным программам от 19 банков — купить недвижимость в ипотеку в Санкт-Петербурге и ЛО на Яндекс.Недвижимости');
        });

        test('get mortgage search description', () => {
            const calculatorDescription = description(mocks.firstMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorDescription).toBe('Выгодные условия на покупку недвижимости в ипотеку в Санкт-Петербурге и ЛО — выбрать из 75 ипотечных программ от 19 банков');
        });

        test('get mortgage search h1', () => {
            const calculatorH1 = h1(mocks.firstMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorH1).toBe('Ипотека');
        });

        test('get mortgage search footer', () => {
            const calculatorFooter = footerText(mocks.firstMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorFooter).toBe('Ипотека в Санкт-Петербурге и ЛО - выберите из 75 ипотечных программ от 19 банков с минимальной ставкой от 2% на срок 15 лет. Подберите подходящую недвижимость в Санкт-Петербурге и ЛО и отправьте заявку на оформление ипотеки с Яндекс.Недвижимостью');
        });
    });

    describe('mortage search seo texts with second moc', () => {
        test('get mortgage search title', () => {
            const calculatorTitle = title(mocks.secondMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorTitle).toBe('Семейная ипотека 2021 в Москве и МО — ставки и актуальные условия покупки квартиры в ипотеку на Яндекс.Недвижимости');
        });

        test('get mortgage search description', () => {
            const calculatorDescription = description(mocks.secondMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorDescription).toBe('Выгодные условия на покупку квартиры в Москве и МО – Семейная ипотека 2021 для покупки квартиры в новостройке или на вторичном рынке');
        });

        test('get mortgage search h1', () => {
            const calculatorH1 = h1(mocks.secondMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorH1).toBe('Семейная ипотека');
        });

        test('get mortgage search footer', () => {
            const calculatorFooter = footerText(mocks.secondMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorFooter).toBe('Семейная ипотека в Москве и МО - выберите из 71 ипотечной программы от 21 банка с минимальной ставкой от 2% на срок 15 лет. Подберите подходящую недвижимость в Москве и МО и отправьте заявку на оформление ипотеки с Яндекс.Недвижимостью');
        });
    });

    describe('mortage search seo texts with moc without programs', () => {
        test('get mortgage search title', () => {
            const calculatorTitle = title(mocks.mockWithoutPrograms);

            // eslint-disable-next-line max-len
            expect(calculatorTitle).toBe('Ипотека в Москве и МО — купить недвижимость в ипотеку в Москве и МО на Яндекс.Недвижимости');
        });

        test('get mortgage search description', () => {
            const calculatorDescription = description(mocks.mockWithoutPrograms);

            // eslint-disable-next-line max-len
            expect(calculatorDescription).toBe('Выгодные условия на покупку недвижимости в ипотеку в Москве и МО');
        });

        test('get mortgage search h1', () => {
            const calculatorH1 = h1(mocks.mockWithoutPrograms);

            // eslint-disable-next-line max-len
            expect(calculatorH1).toBe('Ипотека');
        });

        test('get mortgage search footer', () => {
            const calculatorFooter = footerText(mocks.mockWithoutPrograms);

            // eslint-disable-next-line max-len
            expect(calculatorFooter).toBe('Ипотека в Москве и МО - подберите подходящую недвижимость и отправьте заявку на оформление ипотеки с Яндекс.Недвижимостью');
        });
    });
});
