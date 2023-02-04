import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import Button from 'vertis-react/components/Button';

import { GeoSelector } from '../';

import { getGeo, getSuggestions } from './mocks';

const BUTTON_ID = 'button-id';

const Component = () => (
    <AppProvider>
        {/* eslint-disable-next-line @typescript-eslint/ban-ts-comment */}
        {/* @ts-ignore */}
        <GeoSelector geo={getGeo()} suggestions={getSuggestions()} fetchSuggestions={noop}>
            {({ geo, onOpen }) => (
                <Button id={BUTTON_ID} onClick={onOpen} theme="realty" view="yellow">
                    {geo.locative}
                </Button>
            )}
        </GeoSelector>
    </AppProvider>
);

describe('GeoSelector', () => {
    it('Должен отрисоваться в желтую кнопку', async () => {
        await render(<Component />, { viewport: { width: 320, height: 80 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открытый саджест с дефолтными значениями', async () => {
        await render(<Component />, { viewport: { width: 320, height: 400 } });

        await page.click(`#${BUTTON_ID}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открытый саджест с загруженными саджестами', async () => {
        await render(<Component />, { viewport: { width: 320, height: 400 } });

        await page.click(`#${BUTTON_ID}`);
        await page.type('input', 'При');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
