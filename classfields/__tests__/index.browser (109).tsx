import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { VoteValue } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EGRNPaidReportVote } from '../';

const MOBILE_WIDTH = 360;
const DESKTOP_WIDTH = 900;

describe('EGRNPaidReportVote', () => {
    it('рендерится в дефолтном состоянии тач', async () => {
        await render(<EGRNPaidReportVote size="s" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: MOBILE_WIDTH, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии десктоп', async () => {
        await render(<EGRNPaidReportVote size="l" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: DESKTOP_WIDTH, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится когда отзыв уже был отправлен ранее (передан проп vote извне) тач', async () => {
        await render(<EGRNPaidReportVote size="s" vote={VoteValue.POSITIVE} onSubmit={() => Promise.resolve()} />, {
            viewport: { width: MOBILE_WIDTH, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится когда отзыв уже был отправлен ранее (передан проп vote извне) десктоп', async () => {
        await render(<EGRNPaidReportVote size="l" vote={VoteValue.POSITIVE} onSubmit={() => Promise.resolve()} />, {
            viewport: { width: DESKTOP_WIDTH, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с выбранной реакцией и TextArea тач', async () => {
        await render(<EGRNPaidReportVote size="s" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: MOBILE_WIDTH, height: 500 },
        });

        await page.click('button');
        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с выбранной реакцией и TextArea десктоп', async () => {
        await render(<EGRNPaidReportVote size="l" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: DESKTOP_WIDTH, height: 500 },
        });

        await page.click('button');
        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии ошибки тач', async () => {
        await render(<EGRNPaidReportVote size="s" onSubmit={() => Promise.reject()} />, {
            viewport: { width: MOBILE_WIDTH, height: 500 },
        });

        await page.click('button');
        await page.waitFor(300);
        await page.click('.Button_theme_realty');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии ошибки десктоп', async () => {
        await render(<EGRNPaidReportVote size="l" onSubmit={() => Promise.reject()} />, {
            viewport: { width: DESKTOP_WIDTH, height: 500 },
        });

        await page.click('button');
        await page.waitFor(300);
        await page.click('.Button_theme_realty');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии успеха для негативного отзыва тач', async () => {
        await render(<EGRNPaidReportVote size="s" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: MOBILE_WIDTH, height: 500 },
        });

        await page.click('button');
        await page.waitFor(300);
        await page.click('.Button_theme_realty');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии успеха для негативного отзыва десктоп', async () => {
        await render(<EGRNPaidReportVote size="l" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: DESKTOP_WIDTH, height: 500 },
        });

        await page.click('button');
        await page.waitFor(300);
        await page.click('.Button_theme_realty');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии успеха для нейтрального / позитивного отзыва тач', async () => {
        await render(<EGRNPaidReportVote size="s" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: MOBILE_WIDTH, height: 250 },
        });

        await page.click('button + button');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии успеха для нейтрального / позитивного отзыва десктоп', async () => {
        await render(<EGRNPaidReportVote size="l" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: DESKTOP_WIDTH, height: 250 },
        });

        await page.click('button + button');
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с введённым текстом тач', async () => {
        await render(<EGRNPaidReportVote size="s" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: MOBILE_WIDTH, height: 500 },
        });

        await page.click('button');
        await page.waitFor(200);
        await page.type('textarea', 'очень плохой отчёт, даже не ожидал такого');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с введённым текстом десктоп', async () => {
        await render(<EGRNPaidReportVote size="l" onSubmit={() => Promise.resolve()} />, {
            viewport: { width: DESKTOP_WIDTH, height: 500 },
        });

        await page.click('button');
        await page.waitFor(200);
        await page.type('textarea', 'очень плохой отчёт, даже не ожидал такого');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
