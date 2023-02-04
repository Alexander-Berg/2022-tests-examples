import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import {
    withMortgageCalculations
} from 'realty-core/view/react/modules/alfa-bank-mortgage/enhancers/withMortgageCalculations';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { AlfaBankMortgage as AlfaBankMortgageComponent } from '../';
import styles from '../styles.module.css';

const AlfaBankMortgage = withMortgageCalculations()(props => (
    <AppProvider>
        <AlfaBankMortgageComponent {...props} />
    </AppProvider>
));

async function clearInputAndType(selector, text) {
    const inputValue = await page.$eval(selector, el => el.value);

    await page.focus(selector);
    await Promise.all(inputValue.split('').map(() => page.keyboard.press('Backspace')));

    return page.keyboard.type(text);
}

const getInputSelector = nth => `.SliderInputPromo:nth-child(${nth}) .SliderInputPromo__input`;

const commonProps = {
    subjectFederationId: 1,
    subjectFederationRgid: 123,
    user: {},
    type: 'new',
    loadAlfaBankMortgageParams() {},
    areParamsLoaded: true,
    params: {
        rateBaseNewMin: 0.0879,
        rateBaseNewMax: 0.0929,
        rateBaseSecondary: 0.0839,
        rateBaseHouse: 0.0979,
        rateDiscountYandex: 0.004,
        rateDiscountYandexSecondary: 0.007,
        rateDiscountBank: 0.001,
        rateSupportMax: 0.0619,
        rateSupportMin: 0.0599,
        regionalParams: [
            { geoId: 1, sumTransit: 6000000, sumSupportMax: 8000000 },
            { geoId: 10174, sumTransit: 5000000, sumSupportMax: 7000000 },
            { geoId: 225, sumTransit: 2500000, sumSupportMax: 3000000 }
        ],
        sumMin: 600000,
        sumMax: 40000000,
        costMin: 670000,
        costMax: 50000000,
        costDefault: 10000000,
        downpaymentBaseMax: 0.8,
        downpaymentSupportMin: 0.2,
        downpaymentSupportMax: 0.8,
        downpaymentDefault: 0.3,
        downpaymentBaseNewMin: 0.1,
        downpaymentBaseSecondaryMin: 0.2,
        downpaymentBaseNewTransit: 0.2,
        downpaymentHouseMin: 0.5,
        periodBaseMin: 3,
        periodBaseMax: 30,
        periodSupportMin: 3,
        periodSupportMax: 20,
        periodDefault: 20
    }
};

describe('AlfaBankMortgage', () => {
    describe('не отображается', () => {
        it('если калькулятор не загрузился', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} areParamsLoaded={false} />,
                { viewport: { width: 450, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('если цена больше максимальной', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} cost={51000000} />,
                { viewport: { width: 450, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('если цена меньше минимальной', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} cost={500000} />,
                { viewport: { width: 450, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('для ЖК', () => {
        it('рисует калькулятор с дефолтными параметрами', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 400, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при смене цены меняется первоначальный взнос и параметры', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 320, height: 700 } }
            );

            await clearInputAndType(getInputSelector(1), '5580000');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при вводе цены меньше минимальной подставляет минимальную при потере фокуса', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 640, height: 600 } }
            );

            await clearInputAndType(getInputSelector(1), '5000');
            await page.tap(`.${styles.terms}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при вводе цены больше максимальной подставляет максимальную', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 450, height: 600 } }
            );

            await clearInputAndType(getInputSelector(1), '999999000');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при сумме кредита меньше минимальной показывает информационное сообщение', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 450, height: 600 } }
            );

            await clearInputAndType(getInputSelector(1), '1200000');
            await clearInputAndType(getInputSelector(2), '700000');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при смене первоначального взноса меняются параметры', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 450, height: 600 } }
            );

            await clearInputAndType(getInputSelector(2), '6580000');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при вводе первоначального взноса меньше минимального подставляет минимальный при потере фокуса',
            async() => {
                await render(
                    <AlfaBankMortgage {...commonProps} />,
                    { viewport: { width: 450, height: 600 } }
                );

                await clearInputAndType(getInputSelector(2), '1000000');
                await page.tap(`.${styles.columns}`);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

        it('при вводе первоначального взноса больше максимального подставляет максимальный', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 450, height: 600 } }
            );

            await clearInputAndType(getInputSelector(2), '8100000');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при смене срока меняются параметры', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 450, height: 600 } }
            );

            await clearInputAndType(getInputSelector(3), '7');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при вводе срока меньше минимального подставляет минимальный при потере фокуса', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 450, height: 600 } }
            );

            await clearInputAndType(getInputSelector(3), '1');
            await page.tap(`.${styles.columns}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при вводе срока больше максимального подставляет максимальный', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} />,
                { viewport: { width: 450, height: 600 } }
            );

            await clearInputAndType(getInputSelector(3), '33');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('для первичного оффера', () => {
        it('рисует калькулятор с дефолтными параметрами', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} cost={23567050} />,
                { viewport: { width: 450, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует калькулятор с дефолтными параметрами (дефолтная сумма кредита меньше минимальной)', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} cost={800000} />,
                { viewport: { width: 450, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при вводе первого взноса больше максимального подставляет максимальный',
            async() => {
                await render(
                    <AlfaBankMortgage {...commonProps} cost={23567050} />,
                    { viewport: { width: 450, height: 600 } }
                );

                await clearInputAndType(getInputSelector(2), '999900000');

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

        it('при вводе первого взноса меньше минимального подставляет минимальный после потери фокуса',
            async() => {
                await render(
                    <AlfaBankMortgage {...commonProps} cost={23567050} />,
                    { viewport: { width: 450, height: 600 } }
                );
                await clearInputAndType(getInputSelector(2), '10');
                await page.click(`.${styles.terms}`);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
    });

    describe('для вторичного оффера', () => {
        it('рисует калькулятор с дефолтными параметрами', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} cost={15500000} type='secondary' />,
                { viewport: { width: 450, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при вводе первого взноса больше максимального подставляет максимальный',
            async() => {
                await render(
                    <AlfaBankMortgage {...commonProps} cost={15500000} type='secondary' />,
                    { viewport: { width: 450, height: 600 } }
                );

                await clearInputAndType(getInputSelector(2), '999900000');

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

        it('при вводе первого взноса меньше минимального подставляет минимальный после потери фокуса',
            async() => {
                await render(
                    <AlfaBankMortgage {...commonProps} cost={15500000} type='secondary' />,
                    { viewport: { width: 450, height: 600 } }
                );

                await clearInputAndType(getInputSelector(2), '10');
                await page.click(`.${styles.terms}`);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
    });

    describe('для оффера дома', () => {
        it('рисует калькулятор с дефолтными параметрами', async() => {
            await render(
                <AlfaBankMortgage {...commonProps} cost={15500000} type='house' />,
                { viewport: { width: 450, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('при вводе первого взноса больше максимального подставляет максимальный',
            async() => {
                await render(
                    <AlfaBankMortgage {...commonProps} cost={15500000} type='house' />,
                    { viewport: { width: 450, height: 600 } }
                );

                await clearInputAndType(getInputSelector(2), '999900000');

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
    });
});
