import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { rejectPromise, resolvePromise, infinitePromise } from 'realty-core/view/react/libs/test-helpers';

import { SubheaderBudgetAdd } from '../';

import styles from '../styles.module.css';

describe('SubheaderBudgetAdd', () => {
    it('Рендерится закрытым', async () => {
        await render(<SubheaderBudgetAdd onSubmit={infinitePromise} />, { viewport: { width: 120, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится открытым', async () => {
        await render(<SubheaderBudgetAdd onSubmit={infinitePromise} className="trigger" />, {
            viewport: { width: 400, height: 230 },
        });

        await page.click('.trigger');
        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыт, загрузка', async () => {
        await render(<SubheaderBudgetAdd onSubmit={infinitePromise} className="trigger" />, {
            viewport: { width: 400, height: 230 },
        });

        await page.click('.trigger');
        await page.waitFor(200);

        await page.type('input', 'abc123');

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.submitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыт, ошибка', async () => {
        await render(<SubheaderBudgetAdd onSubmit={rejectPromise} className="trigger" />, {
            viewport: { width: 400, height: 230 },
        });

        await page.click('.trigger');
        await page.waitFor(200);

        await page.type('input', 'abc123');

        await page.click(`.${styles.submitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыт, ошибка тип range', async () => {
        await render(
            <SubheaderBudgetAdd
                onSubmit={() => rejectPromise({ type: 'payment_range', payload: { min: 1000, max: 5000 } })}
                className="trigger"
            />,
            {
                viewport: { width: 400, height: 260 },
            }
        );

        await page.click('.trigger');
        await page.waitFor(200);

        await page.type('input', 'abc123');

        await page.click(`.${styles.submitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыт, ошибка тип step', async () => {
        await render(
            <SubheaderBudgetAdd
                onSubmit={() => rejectPromise({ type: 'payment_step', payload: { step: 10 } })}
                className="trigger"
            />,
            {
                viewport: { width: 400, height: 260 },
            }
        );

        await page.click('.trigger');
        await page.waitFor(200);

        await page.type('input', 'abc123');

        await page.click(`.${styles.submitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыт, успех', async () => {
        await render(<SubheaderBudgetAdd onSubmit={resolvePromise} className="trigger" />, {
            viewport: { width: 400, height: 230 },
        });

        await page.click('.trigger');
        await page.waitFor(200);

        await page.type('input', 'abc123');

        await page.click(`.${styles.submitButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
