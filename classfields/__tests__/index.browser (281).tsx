import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PaymentModalPromocodes } from '../';

describe('PaymentModalPromocodes', () => {
    describe('промокод покрывает часть суммы', () => {
        it('ширина 400px', async () => {
            await render(
                <PaymentModalPromocodes availableMoneyFeaturesPrice={100} price={123} onUsePromocode={noop} />,
                {
                    viewport: { width: 400, height: 600 },
                }
            );
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('ширина 600px', async () => {
            await render(
                <PaymentModalPromocodes availableMoneyFeaturesPrice={100} price={123} onUsePromocode={noop} />,
                {
                    viewport: { width: 600, height: 600 },
                }
            );
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('промокод покрывает полную сумму', () => {
        it('ширина 400px', async () => {
            await render(
                <PaymentModalPromocodes
                    availableMoneyFeaturesPrice={700}
                    moneyPromocodeSpentAmount={123}
                    price={123}
                    onUsePromocode={noop}
                />,
                {
                    viewport: { width: 400, height: 600 },
                }
            );
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('ширина 600px', async () => {
            await render(
                <PaymentModalPromocodes
                    availableMoneyFeaturesPrice={700}
                    moneyPromocodeSpentAmount={123}
                    price={123}
                    onUsePromocode={noop}
                />,
                {
                    viewport: { width: 600, height: 600 },
                }
            );
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
