import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OwnerType } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EGRNPaidReportOwnershipRight } from '../';

const OPTIONS = { viewport: { width: 350, height: 200 } };

const currentOwnershipRight = {
    owners: [
        {
            type: OwnerType.NATURAL_PERSON,
            name: 'Иванов А.',
        },
    ],
    registration: {
        idRecord: '12345',
        regNumber: '54321',
        type: '',
        name: '',
        regDate: '2010-10-10',
        shareText: '1/2',
    },
};

const previousOwnershipRight = {
    owners: [
        {
            type: OwnerType.NATURAL_PERSON,
            name: 'Иванов А.',
        },
    ],
    registration: {
        idRecord: '12345',
        regNumber: '54321',
        type: '',
        name: '',
        regDate: '2010-10-10',
        shareText: '1/2',
        endDate: '2015-10-10',
    },
};

describe('EGRNPaidReportOwnershipRight', () => {
    it('рендерит предыдущее право собственности', async () => {
        await render(<EGRNPaidReportOwnershipRight ownershipRight={previousOwnershipRight} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерит текущее право собственности', async () => {
        await render(<EGRNPaidReportOwnershipRight ownershipRight={currentOwnershipRight} />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
