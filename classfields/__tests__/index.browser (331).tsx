import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { HintModal } from '../';

const Component = () => (
    <HintModal anchorElement={<button id="trigger">trigger</button>}>
        <div style={{ width: 200, height: 200, alignItems: 'center', justifyContent: 'center', display: 'flex' }}>
            content
        </div>
    </HintModal>
);

describe('HintModal', () => {
    it('Рендерится закрытым', async () => {
        await render(<Component />, { viewport: { width: 100, height: 70 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится открытым', async () => {
        await render(<Component />, { viewport: { width: 300, height: 300 } });

        await page.click('#trigger');
        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
