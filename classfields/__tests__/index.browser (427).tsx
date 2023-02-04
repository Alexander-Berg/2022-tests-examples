import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FakeChatWidgetInput } from '../index';

describe('FakeChatWidgetInput', () => {
    it('Базовая отрисовка', async () => {
        await render(<FakeChatWidgetInput page="page" pageType="pageType" onOpenChat={() => null} />, {
            viewport: { width: 400, height: 100 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Кнопка отправки появляется только если введён текст', async () => {
        await render(<FakeChatWidgetInput page="page" pageType="pageType" onOpenChat={() => null} />, {
            viewport: { width: 400, height: 100 },
        });

        await page.type('input', 'text');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
