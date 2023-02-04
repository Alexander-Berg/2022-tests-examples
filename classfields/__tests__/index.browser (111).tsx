import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ErrorPlaceholder } from '../index';

describe('ErrorPlaceholder', () => {
    it('рисует в дефолтном состоянии', async () => {
        await render(<ErrorPlaceholder />, { viewport: { width: 850, height: 250 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует с ссылкой "повторить"', async () => {
        await render(<ErrorPlaceholder onRepeat={() => void 0} />, { viewport: { width: 850, height: 250 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
