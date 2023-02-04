import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferCardExpandableData } from '../index';

describe('OfferCardExpandableData', () => {
    it('Базовая отрисовка', async () => {
        await render(
            <OfferCardExpandableData expandText={'Показать ещё'}>
                {({ collapsed }) => (collapsed ? <span>Это ещё не всё</span> : <span>А теперь всё</span>)}
            </OfferCardExpandableData>,
            { viewport: { width: 200, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('[role=button]');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
