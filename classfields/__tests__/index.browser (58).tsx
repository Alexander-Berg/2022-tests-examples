import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';

import { OwnerHouseServicesMeterReadingsPreviewContainer } from '../container';

import { store } from './stub';

const renderOptions = [
    { viewport: { width: 1200, height: 300 } },
    { viewport: { width: 700, height: 300 } },
    { viewport: { width: 375, height: 300 } },
];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore> }> = (props) => (
    <AppProvider
        rootReducer={rootReducer}
        initialState={props.store}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <OwnerHouseServicesMeterReadingsPreviewContainer />
    </AppProvider>
);

describe('OwnerHouseServicesMeterReadingsPreview', () => {
    describe('Внешний вид', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
