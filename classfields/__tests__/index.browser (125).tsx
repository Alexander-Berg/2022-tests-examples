import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { QRCodePopup } from '../index';

import styles from '../styles.module.css';

describe('QrCodePopup', () => {
    it('обычный вид', async () => {
        await render(
            <QRCodePopup
                popupText={'Наведите камеру телефона на\u00a0код, чтобы быстро набрать номер'}
                content="tel:+70093342156"
            />,
            {
                viewport: { width: 300, height: 400 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.hover(`.${styles.container}`);
        await page.waitFor(30);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('темная тема', async () => {
        const content = 'Жилой комплекс категории De Luxe «Новая Остоженка» расположился на «Золотой миле» Остоженки';
        await render(
            <div style={{ display: 'inline-block', backgroundColor: 'grey' }}>
                <QRCodePopup
                    popupText={'Наведите камеру телефона на\u00a0код, чтобы быстро набрать номер'}
                    content={content}
                    view="dark"
                />
            </div>,
            {
                viewport: { width: 300, height: 400 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.hover(`.${styles.container}`);
        await page.waitFor(30);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
