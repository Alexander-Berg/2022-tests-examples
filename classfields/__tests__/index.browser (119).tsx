import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNReportNotSupportedModal } from '../';

describe('EGRNReportNotSupportedModal', () => {
    it('Должна отрендериться модалка об ошибке оплаты', async () => {
        await render(<EGRNReportNotSupportedModal onClose={() => undefined} visible />, {
            viewport: { width: 900, height: 700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
