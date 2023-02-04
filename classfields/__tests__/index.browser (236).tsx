import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageProgramCardHeader } from '../index';

import { firstProgram, secondProgram, thirdProgram } from './mocks';

describe('MortgageProgramCardHeader', () => {
    it('рисует хеддер', async () => {
        const title = 'Семейная ипотека от\u00a0Банка Открытие';

        await render(<MortgageProgramCardHeader card={firstProgram} title={title} />, {
            viewport: { width: 320, height: 250 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует хеддер с полным описанием', async () => {
        const title = 'Семейная ипотека от\u00a0АТБ';

        await render(<MortgageProgramCardHeader card={secondProgram} title={title} />, {
            viewport: { width: 400, height: 250 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('корректно рендерит описание для программы на загородку', async () => {
        const title = 'Ипотека на\u00a0строительство дома под\u00a0залог недвижимости от\u00a0Росбанка';

        await render(<MortgageProgramCardHeader card={thirdProgram} title={title} />, {
            viewport: { width: 400, height: 250 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
