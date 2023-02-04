import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import CrossIconHeader from '../';

describe('CrossIconHeader', () => {
    it('рендерится по умолчанию', async () => {
        await render(<CrossIconHeader />, { viewport: { width: 320, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с выравниванием налево', async () => {
        await render(<CrossIconHeader align="left" />, { viewport: { width: 320, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с иконкой назад', async () => {
        await render(<CrossIconHeader align="left" icon="back" />, { viewport: { width: 320, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
