import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardDiscounts from '../';

import styles from '../styles.module.css';

import { developerWithImageTab, developerWithPhoneTab, developerWithButtonTab } from './mocks';

describe('DeveloperCardDiscounts', () => {
    it('рисует таб с картинкой', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithImageTab} />,
            { viewport: { width: 320, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таб с телефоном', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithPhoneTab} />,
            { viewport: { width: 400, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таб с кнопокой', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithButtonTab} />,
            { viewport: { width: 640, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таб с раскрытым телефоном после клика', async() => {
        await render(<DeveloperCardDiscounts developer={developerWithPhoneTab} />,
            { viewport: { width: 400, height: 450 } }
        );

        await page.click(`.${styles.button}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
