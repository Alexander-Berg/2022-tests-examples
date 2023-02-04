import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardDiscounts from '../';

import { developerWithOneTab, developerWithSeveralTabs } from './mocks';

describe('DeveloperCardDiscounts', () => {
    it('рисует один таб с кнопкой', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithOneTab} />,
            { viewport: { width: 800, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует кнопку другого цвета при наведении на табе с кнопкой', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithOneTab} />,
            { viewport: { width: 800, height: 450 } }
        );

        await page.hover('.DeveloperCardDiscounts__button');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует таб с кнопкой, таб с картинкой, таб с телефоном', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithSeveralTabs} />,
            { viewport: { width: 800, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует кнопку другого цвета при наведении на табе с телефоном', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithSeveralTabs} />,
            { viewport: { width: 800, height: 450 } }
        );

        await page.hover('.DeveloperCardDiscounts__tab:nth-child(2) .DeveloperCardDiscounts__button');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует полный телефон после клика на кнопку с телефоном', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithSeveralTabs} />,
            { viewport: { width: 800, height: 450 } }
        );

        await page.click('.DeveloperCardDiscounts__tab:nth-child(2) .DeveloperCardDiscounts__button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
