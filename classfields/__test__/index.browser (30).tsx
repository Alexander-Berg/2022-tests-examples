import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PhoneButtonWithQR } from '../';
import styles from '../styles.module.css';

describe('PhoneButtonWithQR', () => {
    it('рендерит кнопку в дефолтном состоянии', async () => {
        await render(<PhoneButtonWithQR />, {
            viewport: { width: 1000, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит кнопку с опциональнм оформлением', async () => {
        await render(<PhoneButtonWithQR defaultView="blue" buttonLabel="свой текст" />, {
            viewport: { width: 1000, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('отрабатывает клик', async () => {
        await render(<PhoneButtonWithQR phones={['+73333333333']} />, {
            viewport: { width: 1000, height: 500 },
        });

        await page.click(`.${styles.container}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('показывает QR-код при ховере на миниатюру', async () => {
        await render(<PhoneButtonWithQR phones={['+73333333333']} />, {
            viewport: { width: 1000, height: 500 },
        });

        await page.click(`.${styles.container}`);

        await page.hover(`.${styles.QRcodeContainer}`);

        await page.waitForSelector(`.Popup_visible`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рендерит кнопку с несколькими номерами', async () => {
        await render(<PhoneButtonWithQR phones={['+73333333333', '+79999999999']} />, {
            viewport: { width: 1000, height: 500 },
        });

        await page.click(`.${styles.container}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
