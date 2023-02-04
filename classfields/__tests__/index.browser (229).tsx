import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RequestStatus } from 'realty-core/types/network';
import { MortgageCalculatorErrorTypes } from 'realty-core/types/mortgage/mortgageCalculator';

import { MortgageCalculator } from '../';
import styles from '../styles.module.css';

const commonProps = {
    programId: 1,
    regionId: 1,
    onCalculateMortgage: (): void => undefined,
    calculateMortgageStatus: RequestStatus.LOADED,
    calculator: {
        creditAmount: 2100000,
        monthlyPayment: 18875,
        monthlyPaymentParams: {
            rate: 7.6,
            propertyCost: 3000000,
            periodYears: 15,
            downPayment: 30,
            downPaymentSum: 900000,
        },
        calculatorLimits: {
            minCreditAmount: 500000,
            maxCreditAmount: 30000000,
            minPropertyCost: 555556,
            maxPropertyCost: 50000000,
            minPeriodYears: 3,
            maxPeriodYears: 30,
            minDownPayment: 10,
            maxDownPayment: 50,
            minRate: 7.6,
            maxRate: 15,
            minDownPaymentSum: 300000,
            maxDownPaymentSum: 1500000,
        },
        queryId: '',
    },
};

describe('MortgageCalculator', () => {
    it('рисует калькулятор с дефолтными данными', async () => {
        await render(<MortgageCalculator {...commonProps} />, {
            viewport: { width: 400, height: 450 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор с загрузкой', async () => {
        await render(<MortgageCalculator {...commonProps} calculateMortgageStatus={RequestStatus.PENDING} />, {
            viewport: { width: 400, height: 450 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор с ошибкой минимальной суммы', async () => {
        await render(
            <MortgageCalculator
                {...commonProps}
                calculateMortgageStatus={RequestStatus.FAILED}
                calculatorError={MortgageCalculatorErrorTypes.MORTGAGE_CREDIT_AMOUNT_TOO_SMALL}
            />,
            {
                viewport: { width: 400, height: 450 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор с ошибкой максимальной суммы', async () => {
        await render(
            <MortgageCalculator
                {...commonProps}
                calculateMortgageStatus={RequestStatus.FAILED}
                calculatorError={MortgageCalculatorErrorTypes.MORTGAGE_CREDIT_AMOUNT_TOO_LARGE}
            />,
            {
                viewport: { width: 400, height: 450 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор с неизвестной ошибкой', async () => {
        await render(
            <MortgageCalculator
                {...commonProps}
                calculateMortgageStatus={RequestStatus.FAILED}
                calculatorError={MortgageCalculatorErrorTypes.UNKNOWN}
            />,
            {
                viewport: { width: 400, height: 450 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор с загрузкой после ошибки', async () => {
        await render(
            <MortgageCalculator
                {...commonProps}
                calculateMortgageStatus={RequestStatus.PENDING}
                calculatorError={MortgageCalculatorErrorTypes.MORTGAGE_CREDIT_AMOUNT_TOO_SMALL}
            />,
            {
                viewport: { width: 400, height: 450 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор при редактировании ставки', async () => {
        await render(<MortgageCalculator {...commonProps} />, {
            viewport: { width: 320, height: 500 },
        });

        await page.click(`.${styles.sliderInput}:nth-child(4)`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор со скорректированным взносом при изменении', async () => {
        await render(<MortgageCalculator {...commonProps} />, {
            viewport: { width: 640, height: 450 },
        });

        const inputSelector = `.${styles.sliderInput}:first-child .SliderInputPromo__input`;
        const inputValue = await page.$eval(inputSelector, (el) => (el as HTMLInputElement).value);

        await page.focus(inputSelector);
        await Promise.all(inputValue.split('').map(() => page.keyboard.press('Backspace')));

        await page.keyboard.type('30000000');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.focus(inputSelector);
        await page.keyboard.press('Backspace');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор с кастомизацией Альфа банка', async () => {
        await render(<MortgageCalculator {...commonProps} view="alfa" />, {
            viewport: { width: 400, height: 450 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
