import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EncumbranceType, IEncumbrance } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EGRNPaidReportEncumbrancesBlock } from '../';

const fullEncumbranceList = [
    {
        type: EncumbranceType.ARREST,
    },
    {
        type: EncumbranceType.LEASE_SUBLEASE,
    },
    {
        type: EncumbranceType.MORTGAGE,
    },
    {
        type: EncumbranceType.OTHER_RESTRICTIONS,
    },
    {
        type: EncumbranceType.PROHIBITION,
    },
    {
        type: EncumbranceType.RENT,
    },
    {
        type: EncumbranceType.SEIZURE_DECISION,
    },
    {
        type: EncumbranceType.TRUST_MANAGEMENT,
    },
];

describe('EGRNPaidReportEncumbrancesBlock', () => {
    it('рендерит пустой список обременений', async () => {
        await render(<EGRNPaidReportEncumbrancesBlock encumbrances={[]} />, { viewport: { width: 350, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит полный список активных обременений', async () => {
        await render(<EGRNPaidReportEncumbrancesBlock encumbrances={fullEncumbranceList as Array<IEncumbrance>} />, {
            viewport: { width: 350, height: 1300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
