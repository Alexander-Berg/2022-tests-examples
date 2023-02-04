import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { rejectPromise, resolvePromise, infinitePromise } from 'realty-core/view/react/libs/test-helpers';

import { SubheaderBudgetEdit } from '../';

describe('SubheaderBudgetEdit', () => {
    it('Рендерится закрытым', async () => {
        await render(<SubheaderBudgetEdit onSubmit={infinitePromise} />, { viewport: { width: 120, height: 100 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится открытым', async () => {
        await render(<SubheaderBudgetEdit onSubmit={infinitePromise} className="trigger" />, {
            viewport: { width: 400, height: 430 },
        });

        await page.click('.trigger');
        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыт, загрузка', async () => {
        await render(<SubheaderBudgetEdit onSubmit={infinitePromise} className="trigger" />, {
            viewport: { width: 400, height: 430 },
        });

        await page.click('.trigger');
        await page.waitFor(200);

        await page.type('input', 'abc123');

        await page.click('form button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыт, ошибка', async () => {
        await render(<SubheaderBudgetEdit onSubmit={(v) => rejectPromise(v)} className="trigger" />, {
            viewport: { width: 400, height: 430 },
        });

        await page.click('.trigger');
        await page.waitFor(200);

        await page.type('input', 'abc123');

        await page.click('form button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открыт, успех', async () => {
        await render(<SubheaderBudgetEdit onSubmit={(v) => resolvePromise(v)} className="trigger" />, {
            viewport: { width: 400, height: 430 },
        });

        await page.click('.trigger');
        await page.waitFor(200);

        await page.type('input', 'abc123');
        await page.type('textarea', 'cba321');

        await page.click('form button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
