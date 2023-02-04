import React from 'react';
import { render } from 'jest-puppeteer-react';
import { WithRouterProps } from 'react-router';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { resolvePromise } from 'realty-core/view/react/libs/test-helpers';

import { IBalance } from 'types/balance';

import { IPaymentSuccessResponse } from 'view/modules/balance/actions/payment';

import { SubheaderBudgetComponent } from '../';

const renderOptions = { viewport: { width: 320, height: 75 } };

const mock = {
    balanceRubles: 500,
    hasEditRight: true,
    hasPayRight: true,
    pay: () => resolvePromise<IPaymentSuccessResponse>(),
    edit: () => resolvePromise<IBalance>(),
    ...({} as WithRouterProps),
} as const;

describe('SubheaderBudget', () => {
    it('Рендерится', async () => {
        await render(<SubheaderBudgetComponent {...mock} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится без права на корректировку', async () => {
        await render(<SubheaderBudgetComponent {...mock} hasEditRight={false} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится без права на пополнение', async () => {
        await render(<SubheaderBudgetComponent {...mock} hasPayRight={false} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится без обоих прав', async () => {
        await render(<SubheaderBudgetComponent {...mock} hasEditRight={false} hasPayRight={false} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
