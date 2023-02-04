import { h1, title, description, footerText } from '../index';

import mocks from './mocks/mocks.index';

describe('mortgage calculator seo texts check', () => {
    describe('mortgage calculator seo texts with first moc', () => {
        test('get mortgage calculator title', () => {
            const calculatorTitle = title(mocks.firstMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorTitle).toBe('Ипотечный калькулятор — расчет ипотеки онлайн по ежемесячному платежу или первому взносу в калькуляторе на Яндекс.Недвижимости в Санкт-Петербурге и ЛО');
        });

        test('get mortgage calculator description', () => {
            const calculatorDescription = description(mocks.firstMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorDescription).toBe('Все ставки и условия банков в ипотечном калькуляторе с расчетом по ежемесячному платежу, первому взносу или с частичным досрочным платежом в Санкт-Петербурге и ЛО');
        });

        test('get mortgage calculator h1', () => {
            const calculatorH1 = h1(mocks.firstMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorH1).toBe('Ипотечный калькулятор в Санкт-Петербурге и ЛО');
        });

        test('get mortgage calculator footer', () => {
            const calculatorFooter = footerText(mocks.firstMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorFooter).toBe('Ипотечный калькулятор в Санкт-Петербурге и ЛО - выберите с учетом вашего первоначального взноса и уровня дохода из 121 ипотечной программы от 23 банков с минимальной ставкой от 1% на срок 10 лет. Подберите подходящую недвижимость в Санкт-Петербурге и ЛО и рассчитайте платеж в ипотечном калькуляторе с Яндекс.Недвижимостью');
        });
    });

    describe('mortage calculator seo texts with second moc', () => {
        test('get mortgage calculator title', () => {
            const calculatorTitle = title(mocks.secondMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorTitle).toBe('Ипотечный калькулятор — расчет ипотеки онлайн по ежемесячному платежу или первому взносу в калькуляторе на Яндекс.Недвижимости в Москве и МО');
        });

        test('get mortgage calculator description', () => {
            const calculatorDescription = description(mocks.secondMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorDescription).toBe('Все ставки и условия банков в ипотечном калькуляторе с расчетом по ежемесячному платежу, первому взносу или с частичным досрочным платежом в Москве и МО');
        });

        test('get mortgage calculator h1', () => {
            const calculatorH1 = h1(mocks.secondMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorH1).toBe('Ипотечный калькулятор');
        });

        test('get mortgage calculator footer', () => {
            const calculatorFooter = footerText(mocks.secondMockVariant);

            // eslint-disable-next-line max-len
            expect(calculatorFooter).toBe('Ипотечный калькулятор в Москве и МО - выберите с учетом вашего первоначального взноса и уровня дохода из 80 ипотечных программ от 21 банка с минимальной ставкой от 2% на срок 15 лет. Подберите подходящую недвижимость в Москве и МО и рассчитайте платеж в ипотечном калькуляторе с Яндекс.Недвижимостью');
        });
    });

    describe('mortage calculator seo texts with moc without programs', () => {
        test('get mortgage calculator title', () => {
            const calculatorTitle = title(mocks.mockWithoutPrograms);

            // eslint-disable-next-line max-len
            expect(calculatorTitle).toBe('Ипотечный калькулятор — расчет ипотеки онлайн по ежемесячному платежу или первому взносу в калькуляторе на Яндекс.Недвижимости в Москве и МО');
        });

        test('get mortgage calculator description', () => {
            const calculatorDescription = description(mocks.mockWithoutPrograms);

            // eslint-disable-next-line max-len
            expect(calculatorDescription).toBe('Все ставки и условия банков в ипотечном калькуляторе с расчетом по ежемесячному платежу, первому взносу или с частичным досрочным платежом в Москве и МО');
        });

        test('get mortgage calculator h1', () => {
            const calculatorH1 = h1(mocks.mockWithoutPrograms);

            // eslint-disable-next-line max-len
            expect(calculatorH1).toBe('Ипотечный калькулятор');
        });

        test('get mortgage calculator footer', () => {
            const calculatorFooter = footerText(mocks.mockWithoutPrograms);

            // eslint-disable-next-line max-len
            expect(calculatorFooter).toBe('Ипотечный калькулятор в Москве и МО - выберите с учетом вашего первоначального взноса и уровня дохода. Подберите подходящую недвижимость в Москве и МО и рассчитайте платеж в ипотечном калькуляторе с Яндекс.Недвижимостью');
        });
    });
});
