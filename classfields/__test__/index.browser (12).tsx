import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/user/reducer';
import { IUniversalStore } from 'view/modules/types';
import { IWithNavigationFlatData } from 'view/enhancers/withNavigationFlatData';

import { NavigationFlatHeader, INavigationFlatHeaderProps } from '..';

import { testProps } from './stubs';

const renderOptions = [
    {
        viewport: {
            width: 1400,
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
    props: Omit<INavigationFlatHeaderProps, keyof IWithNavigationFlatData>;
}> = ({ store, props }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store}>
            <NavigationFlatHeader {...props} />
        </AppProvider>
    );
};

describe('NavigationFlatHeader', () => {
    testProps.forEach(({ title, props, store }) => {
        describe(title, () => {
            renderOptions.forEach((option) => {
                it(`width:${option.viewport.width}`, async () => {
                    await render(<Component store={store} props={props} />, option);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });
    });
});
