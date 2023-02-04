import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RangeChart, IProps } from '../';

const defaultCase = {
    min: 1,
    max: 10,
    rangeFrom: 3,
    rangeTo: 7,
};

const RANGE_TEST_CASES = [
    { description: 'рендерит график без пользовательских маркеров', ...defaultCase },
    {
        description: 'рендерит график с одним маркером вверху в левой половине графика',
        markers: [{ label: 'title', value: 4, vertical: 'top' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером вверху в правой половине графика',
        markers: [{ label: 'title', value: 7, vertical: 'top' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером вверху на левом конце графика',
        markers: [{ label: 'title', value: 1, vertical: 'top' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером вверху на правом конце графика',
        markers: [{ label: 'title', value: 10, vertical: 'top' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером вверху на левом конце диапазона',
        markers: [{ label: 'title', value: 3, vertical: 'top' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером вверху на правом конце диапазона',
        markers: [{ label: 'title', value: 7, vertical: 'top' }],
        ...defaultCase,
    },

    {
        description: 'рендерит график с одним маркером внизу в левой половине графика',
        markers: [{ label: 'title', value: 4, vertical: 'bottom' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером внизу в правой половине графика',
        markers: [{ label: 'title', value: 7, vertical: 'bottom' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером внизу на левом конце графика',
        markers: [{ label: 'title', value: 1, vertical: 'bottom' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером внизу на правом конце графика',
        markers: [{ label: 'title', value: 10, vertical: 'bottom' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером внизу на левом конце диапазона',
        markers: [{ label: 'title', value: 3, vertical: 'bottom' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с одним маркером внизу на правом конце диапазона',
        markers: [{ label: 'title', value: 7, vertical: 'bottom' }],
        ...defaultCase,
    },
    {
        description: 'рендерит график с двумя маркерами внизу и вверху',
        markers: [
            { label: 'title', value: 8, vertical: 'top' },
            { label: 'title2', value: 2, vertical: 'bottom' },
        ],
        ...defaultCase,
    },
];

describe('RangeChart', () => {
    RANGE_TEST_CASES.forEach(({ description, ...props }) => {
        it(`${description} | маленький размер`, async () => {
            await render(<RangeChart {...(props as IProps)} size="s" />, { viewport: { width: 400, height: 230 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    it('рендерится с очень узким range в правых 5% графика', async () => {
        const props = {
            min: 0,
            max: 100000,
            rangeFrom: 94500,
            rangeTo: 95500,
        };

        await render(<RangeChart {...(props as IProps)} size="s" />, { viewport: { width: 400, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с очень узким range в левых 5% графика', async () => {
        const props = {
            min: 0,
            max: 100000,
            rangeFrom: 4000,
            rangeTo: 6000,
        };

        await render(<RangeChart {...(props as IProps)} size="m" />, { viewport: { width: 400, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с очень узким range в центре графика', async () => {
        const props = {
            min: 0,
            max: 100000,
            rangeFrom: 49000,
            rangeTo: 51000,
        };

        await render(<RangeChart {...(props as IProps)} size="m" />, { viewport: { width: 400, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с rangeFrom-rangeTo больше, чем min-max и маркером больше, чем min-max', async () => {
        const props = {
            min: 10,
            max: 100,
            rangeFrom: 5,
            rangeTo: 110,
            markers: [{ label: 'title', value: 115, vertical: 'top' }],
        };

        await render(<RangeChart {...(props as IProps)} size="m" />, { viewport: { width: 400, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит график среднего размера с двумя маркерами', async () => {
        const props = {
            markers: [
                { label: 'title', value: 8, vertical: 'top' },
                { label: 'title2', value: 2, vertical: 'bottom' },
            ],
            ...defaultCase,
        };

        await render(<RangeChart {...(props as IProps)} size="m" />, { viewport: { width: 800, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
