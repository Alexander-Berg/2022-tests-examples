import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportPriceLineChartBlock } from '../';

const OPTIONS = { viewport: { width: 350, height: 600 } };

const props = {
    priceDynamics: {
        building: [
            { date: '2010-10-10', value: 30 },
            { date: '2011-01-10', value: 40 },
            { date: '2011-10-10', value: 50 },
            { date: '2012-10-10', value: 30 },
            { date: '2013-01-10', value: 40 },
            { date: '2013-10-10', value: 50 },
            { date: '2014-01-10', value: 40 },
            { date: '2014-10-10', value: 50 },
            { date: '2014-12-10', value: 30 },
            { date: '2015-01-10', value: 40 },
            { date: '2016-01-10', value: 50 },
            { date: '2017-10-10', value: 30 },
            { date: '2018-01-10', value: 40 },
            { date: '2019-10-10', value: 50 },
            { date: '2020-10-10', value: 30 },
            { date: '2021-01-10', value: 40 },
            { date: '2022-10-10', value: 50 },
            { date: '2023-01-10', value: 40 },
            { date: '2024-10-10', value: 50 },
            { date: '2025-12-10', value: 30 },
            { date: '2026-01-10', value: 40 },
            { date: '2027-01-10', value: 50 },
        ],
        fifteenMin: [
            { date: '2010-10-10', value: 22 },
            { date: '2011-01-10', value: 12 },
            { date: '2011-10-10', value: 93 },
            { date: '2012-10-10', value: 30 },
            { date: '2013-01-10', value: 40 },
            { date: '2013-10-10', value: 50 },
            { date: '2014-01-10', value: 40 },
            { date: '2014-10-10', value: 50 },
            { date: '2014-12-10', value: 30 },
            { date: '2015-01-10', value: 40 },
            { date: '2016-01-10', value: 50 },
            { date: '2017-10-10', value: 30 },
            { date: '2018-01-10', value: 40 },
            { date: '2019-10-10', value: 50 },
            { date: '2020-10-10', value: 30 },
            { date: '2021-01-10', value: 40 },
            { date: '2022-10-10', value: 50 },
            { date: '2023-01-10', value: 40 },
            { date: '2024-10-10', value: 50 },
            { date: '2025-12-10', value: 30 },
            { date: '2026-01-10', value: 40 },
            { date: '2027-01-10', value: 50 },
        ],
        district: [
            { date: '2010-10-10', value: 46 },
            { date: '2011-01-10', value: 78 },
            { date: '2011-10-10', value: 51 },
            { date: '2012-10-10', value: 30 },
            { date: '2013-01-10', value: 40 },
            { date: '2013-10-10', value: 50 },
            { date: '2014-01-10', value: 40 },
            { date: '2014-10-10', value: 50 },
            { date: '2014-12-10', value: 30 },
            { date: '2015-01-10', value: 40 },
            { date: '2016-01-10', value: 50 },
            { date: '2017-10-10', value: 30 },
            { date: '2018-01-10', value: 40 },
            { date: '2019-10-10', value: 50 },
            { date: '2020-10-10', value: 30 },
            { date: '2021-01-10', value: 40 },
            { date: '2022-10-10', value: 50 },
            { date: '2023-01-10', value: 40 },
            { date: '2024-10-10', value: 50 },
            { date: '2025-12-10', value: 30 },
            { date: '2026-01-10', value: 40 },
            { date: '2027-01-10', value: 50 },
        ],
    },
};

describe('EGRNPaidReportPriceLineChartBlock', () => {
    it('рендерится', async () => {
        await render(<EGRNPaidReportPriceLineChartBlock {...props} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
