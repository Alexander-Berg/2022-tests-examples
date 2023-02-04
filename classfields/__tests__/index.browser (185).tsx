import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { ConciergeProfitsScrollableBlock } from '..';
import { IProfitBlock } from '../..';

import testingStyles from './styles.module.css';

const PROFIT_BLOCKS: IProfitBlock[] = [
    { key: 'easy', isHeading: false },
    { key: 'safety', isHeading: true },
    { key: 'comfortable', isHeading: false },
];

const renderComponent = () => (
    <div className={testingStyles.container}>
        <ConciergeProfitsScrollableBlock profits={PROFIT_BLOCKS} />
    </div>
);

describe('ConciergeProfitsScrollableBlock', () => {
    it('корректно рендерится (320px)', async () => {
        await render(renderComponent(), {
            viewport: { width: 320, height: 550 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('корректно рендерится (375px)', async () => {
        await render(renderComponent(), {
            viewport: { width: 375, height: 550 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('корректно рендерится (640px)', async () => {
        await render(renderComponent(), {
            viewport: { width: 640, height: 550 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('корректно проставляет сео-заголовки', async () => {
        await render(
            <div className={`${testingStyles.container} ${testingStyles.withHighlitedHeader}`}>
                <ConciergeProfitsScrollableBlock profits={PROFIT_BLOCKS} />
            </div>,
            {
                viewport: { width: 640, height: 550 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
