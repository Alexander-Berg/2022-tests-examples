import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { OwnerHouseServicesSettingsFormContainer } from '../container';

import { store, skeletonStore, onlyContentStore } from './stub/store';

import 'view/styles/common.css';

const renderOptions = [
    { viewport: { width: 960, height: 1200 } },
    { viewport: { width: 625, height: 1200 } },
    { viewport: { width: 360, height: 1200 } },
];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
    Gate?: AnyObject;
}> = (props) => (
    <div>
        <AppProvider
            initialState={props.store}
            Gate={props.Gate}
            rootReducer={userReducer}
            bodyBackgroundColor={AppProvider.PageColor.USER_LK}
        >
            <OwnerHouseServicesSettingsFormContainer />
        </AppProvider>
    </div>
);

describe('OwnerHouseServicesSettingsForm', () => {
    describe(`Сценарий оплаты жильцом`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`OnlyContent`, () => {
        it(`${renderOptions[2].viewport.width}px`, async () => {
            await render(<Component store={onlyContentStore} />, renderOptions[2]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
