import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageProgramSnippetSearch } from '../index';
import styles from '../styles.module.css';

import {
    simpleProgram,
    longNameProgram,
    largeMonthlyPaymentProgram,
    programWithoutMinExperience,
    programWithoutExperience,
    programWithAlfaIntegration,
    programWithDiscount,
    programWithBankUrl,
    programWithVeryLongName,
} from './mocks';

describe('MortgageProgramSnippetSearch', () => {
    it('рисует снипет', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={simpleProgram} />, {
            viewport: { width: 900, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет с длинным названием', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={longNameProgram} />, {
            viewport: { width: 900, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет с большим месячным платежем', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={largeMonthlyPaymentProgram} />, {
            viewport: { width: 900, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет с ховером', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={longNameProgram} />, {
            viewport: { width: 900, height: 100 },
        });

        await page.hover(`.${styles.link}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

        await page.hover(`.${styles.payment}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует снипет с ховером на кнопках', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={longNameProgram} />, {
            viewport: { width: 900, height: 100 },
        });

        await page.hover(`.${styles.link}`);
        await page.hover(`.${styles.actionButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

        await page.hover(`.${styles.payment}`);
        await page.hover(`.${styles.expandButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует открытый снипет', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={longNameProgram} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover(`.${styles.link}`);
        await page.click(`.${styles.expandButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует открытый снипет с 5 ячеками', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={programWithoutMinExperience} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover(`.${styles.link}`);
        await page.click(`.${styles.expandButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует открытый снипет с 4 ячейками', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={programWithoutExperience} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover(`.${styles.link}`);
        await page.click(`.${styles.expandButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует снипет с интеграцией Альфа банка', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={programWithAlfaIntegration} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover(`.${styles.link}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

        await page.hover(`.${styles.actionButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует снипет с суммой кредита', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={simpleProgram} withCreditSum />, {
            viewport: { width: 900, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует открытый снипет со скидкой', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={programWithDiscount} />, {
            viewport: { width: 900, height: 500 },
        });

        await page.hover(`.${styles.link}`);
        await page.click(`.${styles.expandButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует снипет с ссылкой на банк', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={programWithBankUrl} />, {
            viewport: { width: 900, height: 400 },
        });

        await page.hover(`.${styles.link}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

        await page.hover(`.${styles.actionButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует снипет с очень длинным названием', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={programWithVeryLongName} />, {
            viewport: { width: 900, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.hover(`.${styles.link}`);
        await page.click(`.${styles.expandButton}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
