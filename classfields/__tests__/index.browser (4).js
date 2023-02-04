import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MonthlyLineChart } from '../';

const data = [
    { date: '2010-10-10', value1: 42, value2: 47, value3: 49 },
    { date: '2011-01-10', value1: 72, value2: 77, value3: 79 },
    { date: '2011-10-10', value1: 52, value2: 57, value3: 59 },
    { date: '2012-10-10', value1: 32, value2: 37, value3: 39 },
    { date: '2013-01-10', value1: 42, value2: 47, value3: 49 },
    { date: '2013-10-10', value1: 52, value2: 57, value3: 59 },
    { date: '2014-01-10', value1: 42, value2: 47, value3: 49 },
    { date: '2014-10-10', value1: 52, value2: 57, value3: 59 },
    { date: '2014-12-10', value1: 32, value2: 37, value3: 39 },
    { date: '2015-01-10', value1: 42, value2: 47, value3: 49 },
    { date: '2016-01-10', value1: 52, value2: 57, value3: 59 },
    { date: '2017-10-10', value1: 32, value2: 37, value3: 39 },
    { date: '2018-01-10', value1: 42, value2: 47, value3: 49 },
    { date: '2019-10-10', value1: 52, value2: 57, value3: 59 },
    { date: '2020-10-10', value1: 32, value2: 37, value3: 39 },
    { date: '2021-01-10', value1: 42, value2: 47, value3: 49 },
    { date: '2022-10-10', value1: 52, value2: 57, value3: 59 },
    { date: '2023-01-10', value1: 42, value2: 47, value3: 49 },
    { date: '2024-10-10', value1: 52, value2: 57, value3: 59 },
    { date: '2025-12-10', value1: 32, value2: 37, value3: 39 },
    { date: '2026-01-10', value1: 42, value2: 47, value3: 49 },
    { date: '2027-01-10', value1: 52, value2: 57, value3: 59 }
];

const lines = [
    { dataKey: 'value1', color: '#58bfe7', label: 'label1' },
    { dataKey: 'value2', color: '#eba704', label: 'label2' },
    { dataKey: 'value3', color: '#00b341', label: 'label3' }
];

describe('MonthlyLineChart', () => {
    it('рендерится c одной записью', async() => {
        await render(
            <MonthlyLineChart
                data={[ data[0] ]}
                lines={lines}
                height={500}
                width={500}
                size='m'
            />,
            { viewport: { width: 540, height: 740 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится c двумя записями', async() => {
        await render(
            <MonthlyLineChart
                data={[ data[0], data[1] ]}
                lines={lines}
                height={500}
                width={500}
                size='m'
            />,
            { viewport: { width: 540, height: 740 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в среднем размере', async() => {
        await render(
            <MonthlyLineChart
                data={data}
                lines={lines}
                height={500}
                width={500}
                size='m'
            />,
            { viewport: { width: 540, height: 740 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в маленьком размере', async() => {
        await render(
            <MonthlyLineChart
                data={data}
                lines={lines}
                height={260}
                width={260}
                size='s'
            />,
            { viewport: { width: 300, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
