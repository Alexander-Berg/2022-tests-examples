import { render } from 'jest-puppeteer-react';

import React from 'react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import Presets from '../index';

import { links } from './mock';

describe('Отрисовка блока с ссылками:', () => {
    Object.keys(links).forEach((k) => {
        it(`блок ${k}`, async () => {
            await render(
                <AppProvider>
                    <Presets sections={[links[k]]} />
                </AppProvider>,
                {
                    viewport: { width: 700, height: 500 },
                }
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
