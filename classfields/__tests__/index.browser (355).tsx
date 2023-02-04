import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RequestStatus } from 'realty-core/types/network';

import { MortgageProgramCardCalculator } from '../';

const commonProps = {
    programId: 1,
    regionId: 1,
    calculateMortgage: () => Promise.resolve(undefined),
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

describe('MortgageProgramCardCalculator', () => {
    it('рисует калькулятор с дефолтными данными', async () => {
        await render(<MortgageProgramCardCalculator {...commonProps} />, {
            viewport: { width: 1100, height: 350 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует калькулятор с кастомизацией Альфа банка', async () => {
        await render(<MortgageProgramCardCalculator {...commonProps} integration="alfa" />, {
            viewport: { width: 1100, height: 350 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
