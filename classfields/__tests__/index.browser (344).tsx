import React from 'react';
import { render } from 'jest-puppeteer-react';

import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { Right } from 'types/user';

import { StatisticsPageContainer } from '../container';

const mockState = {
    page: {
        name: 'partner-statsitcs',
    },
    client: {
        id: 1,
    },
    billingUser: {
        rights: { [Right.CAMPAIGN_BALANCE_CORRECT]: true, [Right.CAMPAIGN_BALANCE_PAY]: true },
    },
    agencyClients: [
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 10090064,
                name: 'ФСК Лидер',
                phone: '',
                regionId: 225,
                type: 'Other',
                url: '',
            },
            id: 10090064,
            isAgencyInService: false,
            resource: { developerId: '295544' },
        },
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 10090062,
                name: 'Мартин',
                phone: '',
                regionId: 225,
                type: 'IndividualPerson',
                url: '',
            },
            id: 10090062,
            isAgencyInService: false,
            resource: { developerId: '295479' },
        },
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 10090068,
                name: 'Capital Group',
                phone: '',
                regionId: 225,
                type: 'IndividualPerson',
                url: '',
            },
            id: 10090068,
            isAgencyInService: false,
            resource: { developerId: '295494' },
        },
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 10090070,
                name: 'Главстрой',
                phone: '',
                regionId: 225,
                type: 'IndividualPerson',
                url: '',
            },
            id: 10090070,
            isAgencyInService: false,
            resource: { developerId: '295499' },
        },
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 10090073,
                name: 'ЦНН',
                phone: '',
                regionId: 225,
                type: 'IndividualPerson',
                url: '',
            },
            id: 10090073,
            isAgencyInService: false,
            resource: { developerId: '295506' },
        },
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 10090076,
                name: 'БЭСТКОН',
                phone: '',
                regionId: 225,
                type: 'IndividualPerson',
                url: '',
            },
            id: 10090076,
            isAgencyInService: false,
            resource: { developerId: '295524' },
        },
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 10090079,
                name: 'ГК «Веста»',
                phone: '',
                regionId: 225,
                type: 'IndividualPerson',
                url: '',
            },
            id: 10090079,
            isAgencyInService: false,
            resource: { developerId: '295533' },
        },
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 13897620,
                name: 'Ремикс',
                phone: '',
                regionId: 225,
                type: 'IndividualPerson',
                url: '',
            },
            id: 13897620,
            isAgencyInService: false,
            resource: { developerId: '321667' },
        },
        {
            agencyId: 10046004,
            client: {
                agency: false,
                agencyId: 10046004,
                city: '',
                email: '',
                fax: '',
                id: 13897628,
                name: 'ООО "ЖК на Ивановcкой"',
                phone: '',
                regionId: 225,
                type: 'IndividualPerson',
                url: '',
            },
            id: 13897628,
            isAgencyInService: false,
            resource: { developerId: '321674' },
        },
    ],
    clientSites: [
        { id: 131977, name: 'Гагаринский (Жуковский)', location: { subjectFederationRgid: 741965 } },
        { id: 170725, name: 'Дыхание', location: { subjectFederationRgid: 741965 } },
        { id: 193551, name: 'Кварталы 21/19', location: { subjectFederationRgid: 741965 } },
        { id: 441250, name: 'Лисицына 5', location: { subjectFederationRgid: 741965 } },
        { id: 85348, name: 'Некрасовка (ДСК-1-Авеста-Строй)', location: { subjectFederationRgid: 741965 } },
        { id: 94712, name: 'Новое Тушино', location: { subjectFederationRgid: 741965 } },
        { id: 86941, name: 'Новый Раменский', location: { subjectFederationRgid: 741965 } },
        { id: 217598, name: 'Первый Андреевский', location: { subjectFederationRgid: 741965 } },
        { id: 558418, name: 'Первый Юбилейный', location: { subjectFederationRgid: 741965 } },
        { id: 376370, name: 'Скандинавский', location: { subjectFederationRgid: 741965 } },
        { id: 67810, name: 'Сколковский', location: { subjectFederationRgid: 741965 } },
        { id: 64153, name: 'Центр-2', location: { subjectFederationRgid: 741965 } },
    ],
    siteStatistics: {
        searchQuery: {
            siteId: ['376370'],
            statistics: ['siteClicksPerSubjectByDay', 'sitePricePerMeterByDay'],
            fromDate: '2021-11-05',
            untilDate: '2021-11-21',
            clientId: '10090064',
        },
        defaultSearchQuery: {
            siteId: ['376370'],
            fromDate: '2021-10-10',
            untilDate: '2021-11-10',
            statistics: [],
        },
        isLoading: false,
        queryId: 'fdf23edf23r',
        metrics: {
            siteClicksPerSubjectByDay: [
                { date: '2021-11-05', value: 0.01 },
                { date: '2021-11-06', value: 0.011 },
                { date: '2021-11-07', value: 0.012 },
                { date: '2021-11-08', value: 0.007 },
                { date: '2021-11-09', value: 0.0234555 },
                { date: '2021-11-10', value: 0.01901 },
                { date: '2021-11-11', value: 0.01 },
                { date: '2021-11-12', value: 0.023 },
                { date: '2021-11-13', value: 0.045 },
                { date: '2021-11-14', value: 0.1 },
                { date: '2021-11-15', value: 0.115 },
                { date: '2021-11-16', value: 0.09999 },
                { date: '2021-11-17', value: 0.1455 },
                { date: '2021-11-18', value: 0.2 },
                { date: '2021-11-19', value: 0.18 },
                { date: '2021-11-20', value: 0.21 },
                { date: '2021-11-21', value: 0.2312 },
            ],
            sitePricePerMeterByDay: [
                { date: '2021-11-05', value: 48000 },
                { date: '2021-11-06', value: 56334 },
                { date: '2021-11-07', value: 55000 },
                { date: '2021-11-08', value: 53234 },
                { date: '2021-11-09', value: 53235 },
                { date: '2021-11-10', value: 54900 },
                { date: '2021-11-11', value: 49999 },
                { date: '2021-11-12', value: 48888 },
                { date: '2021-11-13', value: 47777 },
                { date: '2021-11-14', value: 51000 },
                { date: '2021-11-15', value: 50834 },
                { date: '2021-11-16', value: 49342 },
                { date: '2021-11-17', value: 51000 },
                { date: '2021-11-18', value: 52034 },
                { date: '2021-11-19', value: 53498 },
                { date: '2021-11-20', value: 56324 },
                { date: '2021-11-21', value: 55324 },
            ],
        },
    },
};

advanceTo(new Date('2021-11-01'));

describe('StatisticsPage', () => {
    it('Рендерится с полным набором элементов', async () => {
        await render(
            <AppProvider initialState={mockState}>
                <StatisticsPageContainer />
            </AppProvider>,
            { viewport: { width: 1300, height: 700 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
