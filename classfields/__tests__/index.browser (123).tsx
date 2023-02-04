import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IPriceStatisticsRoomItem } from 'realty-core/types/priceStatistics';

import { PriceStatisticsChart } from '../';

import { case12974Data, defaultProps, getData } from './mocks';

const render = async ({ data }: { data: IPriceStatisticsRoomItem[] }) => {
    await _render(
        <div style={{ width: 400, height: 400, padding: 10 }}>
            <PriceStatisticsChart data={data} {...defaultProps} />
        </div>,
        { viewport: { width: 420, height: 420 } }
    );
    await page.addStyleTag({ content: 'body{padding: 0}' });
};

describe('PriceStatisticsChart', () => {
    it('График рендерится всего 2 точки', async () => {
        await render({ data: getData('2019-03-31T00:00:00Z', 2) });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('График рендерится очень много точек', async () => {
        await render({ data: getData('2019-03-31T00:00:00Z', 500) });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('График рендерится первый месяц - декабрь', async () => {
        await render({ data: getData('2019-12-31T00:00:00Z', 50) });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('График рендерится последний месяц - январь', async () => {
        await render({ data: getData('2019-01-31T00:00:00Z', 50, true) });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Появляется точка', async () => {
        await render({ data: getData('2019-12-31T00:00:00Z', 50) });

        await page.mouse.move(200, 200);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('Верно рисует хвосты с пропущенными точками', async () => {
        await render({ data: case12974Data });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
