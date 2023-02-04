import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportPendingScreen } from '../';

const OPTIONS = { viewport: { width: 350, height: 600 } };

describe('EGRNPaidReportPendingScreen', () => {
    it('рендерится', async () => {
        await render(<EGRNPaidReportPendingScreen email="123@mail.ru" />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с длинной почтой', async () => {
        await render(
            <EGRNPaidReportPendingScreen email="122342dsflksjdlfkj234sdf2039rufjljsfsdlfj3@mail.ru" />,
            OPTIONS
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится без адреса почты', async () => {
        await render(<EGRNPaidReportPendingScreen />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
