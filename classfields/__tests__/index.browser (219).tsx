import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';

import { EGRNPaymentModalSuccessStage } from '../';

describe('EGRNPaymentModalSuccess', () => {
    describe('отчет готов', () => {
        it('ширина 320px', async () => {
            await render(<EGRNPaymentModalSuccessStage email="test@test.ru" onCloseModal={noop} />, {
                viewport: { width: 320, height: 650 },
            });
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('ширина 400px', async () => {
            await render(<EGRNPaymentModalSuccessStage email="test@test.ru" onCloseModal={noop} />, {
                viewport: { width: 400, height: 650 },
            });
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('ширина 650px', async () => {
            await render(<EGRNPaymentModalSuccessStage email="test@test.ru" onCloseModal={noop} />, {
                viewport: { width: 650, height: 650 },
            });
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('ширина 320px, со ссылкой на отчёт', async () => {
            await render(
                <AppProvider>
                    <EGRNPaymentModalSuccessStage email="test@test.ru" onCloseModal={noop} paidReportId="123" />
                </AppProvider>,
                {
                    viewport: { width: 320, height: 650 },
                }
            );
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
