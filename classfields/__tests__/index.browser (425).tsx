import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { CheckType } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EGRNPaidReportCheckResultBlock } from '../';

const OPTIONS = { viewport: { width: 900, height: 300 } };

const TEST_CASES_CHECKUP = Object.keys(CheckType).map((checkupEnumKey) => ({
    type: checkupEnumKey as CheckType,
    ownerCount: 5,
}));

describe('EGRNPaidReportCheckResultBlock', () => {
    TEST_CASES_CHECKUP.forEach(({ type, ownerCount }) => {
        it(`рендерит результат проверки типа: ${type}`, async () => {
            await render(<EGRNPaidReportCheckResultBlock type={type} ownerCount={ownerCount} />, OPTIONS);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
