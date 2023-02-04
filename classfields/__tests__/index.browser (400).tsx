import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteCardGalleryConciergeServiceBanner } from '../';
import styles from '../styles.module.css';

import { GatePending, GateSuccess, GateError, IGate } from './mocks';

const renderComponent = ({ Gate }: { Gate?: IGate; height?: number }) =>
    render(
        <AppProvider Gate={Gate}>
            <SiteCardGalleryConciergeServiceBanner />
        </AppProvider>,
        { viewport: { width: 400, height: 400 } }
    );

describe('SiteCardGalleryConciergeServiceBanner', () => {
    it('рендерится корректно', async () => {
        await renderComponent({});

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('телефон вбит, статус pending', async () => {
        await renderComponent({ Gate: GatePending });

        await page.type('input[type="tel"]', '1234567890');

        await page.click(`.${styles.btn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('сообщение успешно отправлено', async () => {
        await renderComponent({ Gate: GateSuccess });

        await page.type('input[type="tel"]', '1234567890');

        await page.click(`.${styles.btn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ошибка сервера', async () => {
        await renderComponent({ Gate: GateError });

        await page.type('input[type="tel"]', '1234567890');

        await page.click(`.${styles.btn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
