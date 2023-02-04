import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RequestStatus } from 'realty-core/types/network';

import { IServiceButton, ServiceButtonType } from 'types/flat';

import { IServiceButtonViewData } from 'types/navigation';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/public/reducer';
import { IUniversalStore } from 'view/modules/types';
import { IWithSPAProps } from 'view/enhancers/withSPA';
import { getServiceButtonViewData } from 'view/libs/navigation';

import { NavigationFlatMobileServiceButtons, INavigationFlatMobileServiceButtonsProps } from '..';

const renderOptions = [
    {
        viewport: {
            width: 1024,
            height: 800,
        },
    },
    {
        viewport: {
            width: 768,
            height: 1024,
        },
    },
    {
        viewport: {
            width: 375,
            height: 900,
        },
    },
];

const Component: React.FC<{
    store: DeepPartial<IUniversalStore>;
    props: Omit<INavigationFlatMobileServiceButtonsProps, keyof IWithSPAProps>;
}> = ({ store, props }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store}>
            <NavigationFlatMobileServiceButtons {...props} />
        </AppProvider>
    );
};

const serviceButtons: Partial<IServiceButton>[] = [
    { meterReadings: {} },
    { frontendTenants: {} },
    { frontendAboutFlat: {} },
    { paymentHistory: {} },
];

describe('NavigationFlatMobileServiceButtons', () => {
    serviceButtons.reduce((acc, value) => {
        acc = [
            ...acc,
            {
                ...getServiceButtonViewData(Object.keys(value)[0] as ServiceButtonType),
                type: value as ServiceButtonType,
            },
        ];

        describe(`Количество кнопок = ${acc.length}`, () => {
            renderOptions.forEach((option) => {
                it(`width:${option.viewport.width}`, async () => {
                    await render(
                        <Component
                            props={{
                                serviceButtonsViewData: acc,
                            }}
                            store={{
                                spa: {
                                    status: RequestStatus.LOADED,
                                },
                            }}
                        />,
                        option
                    );

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });

        return acc;
    }, [] as IServiceButtonViewData[]);
});
