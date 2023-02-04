import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SiteStatisticsDashboard, ISiteStatisticsDashboardProps } from '../';

const props = {
    searchQuery: {
        agencyId: '1041590',
        clientId: '38116742',
        siteId: ['57173'],
        statistics: ['siteClicksPerSubjectByDay', 'siteOffersNumberByDay'],
        fromDate: '2021-10-15',
        untilDate: '2021-11-15',
    },
    defaultSearchQuery: {
        clientId: '38116742',
        siteId: ['57173'],
        fromDate: '2021-10-15',
        untilDate: '2021-11-15',
        statistics: ['siteClicksPerSubjectByDay', 'siteOffersNumberByDay'],
    },
    queryId: '',
    metrics: {
        siteClicksPerSubjectByDay: [
            { date: '2021-10-15', value: 1 },
            { date: '2021-10-16', value: 2 },
            { date: '2021-10-17', value: 4 },
            { date: '2021-10-18', value: 6 },
            { date: '2021-10-19', value: 9 },
            { date: '2021-10-20', value: 12 },
            { date: '2021-10-21', value: 12 },
            { date: '2021-10-22', value: 25 },
            { date: '2021-10-23', value: 26 },
        ],
        siteOffersNumberByDay: [
            { date: '2021-10-15', value: 88 },
            { date: '2021-10-16', value: 33 },
            { date: '2021-10-17', value: 56 },
            { date: '2021-10-18', value: 7 },
            { date: '2021-10-19', value: 9 },
            { date: '2021-10-20', value: 120 },
            { date: '2021-10-21', value: 90 },
            { date: '2021-10-22', value: 14 },
            { date: '2021-10-23', value: 50 },
        ],
    },
    onMetricsChange: () => undefined,
    isLoading: false,
    noChartAnimation: true,
} as ISiteStatisticsDashboardProps;

describe('SiteStatisticsDashboard', () => {
    it('Рендерится корректно', async () => {
        await render(<SiteStatisticsDashboard {...props} isLoading={false} />, {
            viewport: { width: 1300, height: 700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot({
            failureThreshold: 1,
        });
    });

    it('Корректно обрабатывает состояние загрузки', async () => {
        await render(<SiteStatisticsDashboard {...props} isLoading={true} />, {
            viewport: { width: 1300, height: 700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot({
            failureThreshold: 1,
        });
    });

    it('Для региона без матрицы цен показывает не все метрики', async () => {
        const clientSite = {
            id: 1,
            name: 'abc',
            location: {
                subjectFederationRgid: 1,
            },
        };
        await render(<SiteStatisticsDashboard {...props} isLoading={false} clientSite={clientSite} />, {
            viewport: { width: 1300, height: 700 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot({
            failureThreshold: 1,
        });
    });
});
