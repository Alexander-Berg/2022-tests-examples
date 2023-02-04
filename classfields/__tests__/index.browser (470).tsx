import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SeoLinksDataNotFound } from '../index';

describe('SeoLinksDataNotFound', () => {
    it('Базовая отрисовка', async () => {
        await render(<SeoLinksDataNotFound text="Не найдено" linkText="Перейти" url="" />, {
            viewport: { width: 600, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
