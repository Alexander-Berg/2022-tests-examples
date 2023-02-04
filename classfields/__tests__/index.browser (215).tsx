import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportRegistryDataBlock } from '../index';

const OPTIONS = {
    viewport: {
        width: 350,
        height: 500,
    },
};

describe('EGRNPaidReportEGRNDataBlock', () => {
    it('рендерится', async () => {
        await render(
            <EGRNPaidReportRegistryDataBlock
                excerptData={{
                    address: 'Крохановского 23',
                    area: 25,
                    cadastralCost: 123123123,
                    cadastralNumber: '1239870879384',
                    floor: 'чердак',
                    date: '2020-10-10',
                }}
            />,
            OPTIONS
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с длинными значениями адреса и кадастровой стоимости', async () => {
        await render(
            <EGRNPaidReportRegistryDataBlock
                excerptData={{
                    address: "Санкт-Петербург, Крохановского 229, Красногвардейский район, корп. 21 лит. А кв. 29 этаж 2 комната 3", // eslint-disable-line
                    area: 25,
                    cadastralCost: 123123123123123123123,
                    cadastralNumber: '1239870879384',
                    floor: 'чердак',
                    date: '2020-10-10',
                }}
            />,
            OPTIONS
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
