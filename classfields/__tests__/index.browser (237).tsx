import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageProgramSnippetSearch } from '../';

import {
    getProgramWithoutRequirements,
    getProgram,
    getProgramLargeMonthlyPayment,
    getProgramAlfaIntegration,
    getProgramWithDiscount,
} from './mocks';

describe('MortgageProgramSnippetSearch', () => {
    it('Рисует минимально заполненный сниппет', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={getProgramWithoutRequirements()} />, {
            viewport: { width: 320, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует максимально заполненный сниппет', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={getProgram()} />, {
            viewport: { width: 320, height: 420 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует сниппет с большим месячным платежом', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={getProgramLargeMonthlyPayment()} />, {
            viewport: { width: 320, height: 420 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует максимально заполненный сниппет с проскроллом', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={getProgram()} />, {
            viewport: { width: 320, height: 420 },
        });

        await page.evaluate(() => {
            document.querySelector('div[class^="MortgageProgramSnippetSearch__content"]')!.scroll(100, 0);
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует сниппет с интеграцией Альфа банка', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={getProgramAlfaIntegration()} />, {
            viewport: { width: 320, height: 420 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует максимально заполненный сниппет (с суммой кредита)', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={getProgram()} withCreditSum />, {
            viewport: { width: 320, height: 420 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует сниппет со скидкой', async () => {
        await render(<MortgageProgramSnippetSearch mortgageProgram={getProgramWithDiscount()} withCreditSum />, {
            viewport: { width: 400, height: 420 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
