import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IEGRNPaidReport, PaidReportStatus } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EGRNPaidReportComponent } from '../';

import { fullReport } from './mock';

describe('EGRNPaidReport', () => {
    it(`рендерится без карты`, async () => {
        await render(
            <AppProvider initialState={{}} context={{ link: () => 'https://realty.yandex.ru' }}>
                <EGRNPaidReportComponent egrnPaidReport={fullReport} />
            </AppProvider>,
            { viewport: { width: 360, height: 14000, deviceScaleFactor: 0.5 } } // с обычным скейлингом создание скриншота падает из-за его размера
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`рендерится с ошибкой`, async () => {
        await render(
            <AppProvider initialState={{}} context={{ link: () => 'https://realty.yandex.ru' }}>
                <EGRNPaidReportComponent egrnPaidReport={{ reportStatus: PaidReportStatus.ERROR } as IEGRNPaidReport} />
            </AppProvider>,
            { viewport: { width: 700, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`рендерится в статусе прогресса`, async () => {
        await render(
            <AppProvider initialState={{}} context={{ link: () => 'https://realty.yandex.ru' }}>
                <EGRNPaidReportComponent
                    egrnPaidReport={{ reportStatus: PaidReportStatus.IN_PROGRESS } as IEGRNPaidReport}
                />
            </AppProvider>,
            { viewport: { width: 700, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
