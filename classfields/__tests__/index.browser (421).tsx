import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IEGRNPaidReport, PaidReportStatus } from 'realty-core/view/react/common/types/egrnPaidReport';

import { EGRNPaidReport } from '../';

import { fullReport } from './mock';

describe('EGRNPaidReport', () => {
    it(`рендерится без карты с навигацией в хедере`, async () => {
        await render(
            <AppProvider initialState={{}} context={{ link: () => 'https://realty.yandex.ru' }}>
                <EGRNPaidReport egrnPaidReport={fullReport} disableIntersectionObserver />
            </AppProvider>,
            { viewport: { width: 940, height: 10600, deviceScaleFactor: 0.5 } } // с обычным скейлингом создание скриншота падает из-за его размера
        );

        await page.waitFor(500);

        expect(await takeScreenshot()).toMatchImageSnapshot({
            failureThreshold: 1, // В CI рандомно один пиксель сбоит
        });
    });

    it(`рендерится без карты с навигацией в сайдбаре`, async () => {
        await render(
            <AppProvider initialState={{}} context={{ link: () => 'https://realty.yandex.ru' }}>
                <EGRNPaidReport egrnPaidReport={fullReport} disableIntersectionObserver />
            </AppProvider>,
            { viewport: { width: 1200, height: 10600, deviceScaleFactor: 0.5 } } // с обычным скейлингом создание скриншота падает из-за его размера
        );

        await page.waitFor(500);

        expect(await takeScreenshot()).toMatchImageSnapshot({
            failureThreshold: 1, // В CI рандомно один пиксель сбоит
        });
    });

    it(`рендерится с ошибкой`, async () => {
        await render(
            <AppProvider initialState={{}} context={{ link: () => 'https://realty.yandex.ru' }}>
                <EGRNPaidReport
                    egrnPaidReport={{ reportStatus: PaidReportStatus.ERROR } as IEGRNPaidReport}
                    disableIntersectionObserver
                />
            </AppProvider>,
            { viewport: { width: 940, height: 540 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot({
            failureThreshold: 1, // В CI рандомно один пиксель сбоит
        });
    });

    it(`рендерится в статусе прогресса`, async () => {
        await render(
            <AppProvider initialState={{}} context={{ link: () => 'https://realty.yandex.ru' }}>
                <EGRNPaidReport
                    egrnPaidReport={{ reportStatus: PaidReportStatus.IN_PROGRESS } as IEGRNPaidReport}
                    disableIntersectionObserver
                />
            </AppProvider>,
            { viewport: { width: 940, height: 540 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot({
            failureThreshold: 1, // В CI рандомно один пиксель сбоит
        });
    });
});
