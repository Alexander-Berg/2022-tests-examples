import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { TenantSearchStatsActivityLevel } from 'types/flat';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/user/reducer';
import { IUniversalStore } from 'view/modules/types';

import { TenantSearchStatsNotification, ITenantSearchStatsNotificationProps } from '../index';

const renderOptions = [
    { viewport: { width: 1024, height: 812 }, isMobile: false },
    { viewport: { width: 600, height: 812 }, isMobile: false },
    { viewport: { width: 360, height: 568 }, isMobile: true },
];

const initialState: DeepPartial<IUniversalStore> = {
    modal: {},
};

const Component = (props: ITenantSearchStatsNotificationProps) => (
    <AppProvider rootReducer={rootReducer} initialState={initialState}>
        <div style={{ backgroundColor: 'grey', padding: '10px' }}>
            <TenantSearchStatsNotification {...props} />
        </div>
    </AppProvider>
);

const propsList: {
    testCase: string;
    props: ITenantSearchStatsNotificationProps;
}[] = [
    {
        testCase: 'Базовый рендер',
        props: {
            data: {
                realtyUrl: 'https://realty.ru',
                calls: 23,
                views: 22,
                showings: 90,
                applications: 43,
                daysInExposition: 1,
                offerId: '231324543243234',
                activityLevel: TenantSearchStatsActivityLevel.MODERATE,
                currentRentalValue: '18000',
                currentAdValue: '20000',
            },
        },
    },
    {
        testCase: 'Статистика показа за три дня',
        props: {
            data: {
                realtyUrl: 'https://realty.ru',
                calls: 23,
                views: 22,
                showings: 90,
                applications: 43,
                daysInExposition: 3,
                offerId: '231324543243234',
                activityLevel: TenantSearchStatsActivityLevel.DISGUSTING,
                currentRentalValue: '18000',
                currentAdValue: '20000',
            },
        },
    },
    {
        testCase: 'Статистка показа за восемь дней',
        props: {
            data: {
                realtyUrl: 'https://realty.ru',
                calls: 23,
                views: 22,
                showings: 90,
                applications: 43,
                daysInExposition: 8,
                offerId: '231324543243234',
                activityLevel: TenantSearchStatsActivityLevel.LOW,
                currentRentalValue: '18000',
                currentAdValue: '20000',
            },
        },
    },
    {
        testCase: 'Большое числа в статистике',
        props: {
            data: {
                realtyUrl: 'https://realty.ru',
                calls: 500,
                views: 2939,
                showings: 1000000,
                applications: 98323,
                daysInExposition: 100,
                offerId: '231324543243234',
                activityLevel: TenantSearchStatsActivityLevel.NORMAL,
                currentRentalValue: '18000',
                currentAdValue: '20000',
            },
        },
    },
];

describe('TenantSearchStatsNotification', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            propsList.forEach(({ testCase, props }) => {
                it(`${testCase} ${renderOption.viewport.width}px`, async () => {
                    await render(<Component {...props} isMobile={renderOption.isMobile} />, renderOption);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });
    });
});
