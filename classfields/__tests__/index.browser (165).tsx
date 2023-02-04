import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EgrnReportsProblem } from '../';

describe('EgrnReportsProblem', () => {
    it('ошибка получения отчета', async () => {
        await render(<EgrnReportsProblem />, { viewport: { width: 1000, height: 400 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
