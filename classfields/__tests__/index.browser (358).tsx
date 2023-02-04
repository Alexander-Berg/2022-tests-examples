import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MortgageProgramCardHeader } from '../index';

import { firstProgram, secondProgram, thirdProgram } from './mocks';

const title = 'Семейная ипотека от Банка Открытие';

describe('MortgageProgramCardHeader', () => {
    it('рисует хеддер', async () => {
        await render(
            <AppProvider>
                <MortgageProgramCardHeader card={firstProgram} title={title} rgid={1} />
            </AppProvider>,
            {
                viewport: { width: 1100, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует хеддер с полным описанием', async () => {
        await render(
            <AppProvider>
                <MortgageProgramCardHeader card={secondProgram} title={title} rgid={1} />
            </AppProvider>,
            {
                viewport: { width: 960, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('корректно рендерит описание для программы на загородку', async () => {
        const title = 'Ипотека на\u00a0строительство дома под\u00a0залог недвижимости от\u00a0Росбанка';

        await render(
            <AppProvider>
                <MortgageProgramCardHeader card={thirdProgram} title={title} rgid={1} />
            </AppProvider>,
            {
                viewport: { width: 960, height: 350 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
