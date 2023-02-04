import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject, FunctionReturnAny } from 'realty-core/types/utils';

import { IHouseService, IHouseServiceMeter, IHouseServicesSettings } from 'types/houseService';
import { FlatId } from 'types/flat';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { HouseServicesSettingsPreviewContainer } from '../container';

import { ownerFilledStore, ownerMinFilledStore, tenantFilledStore, skeletonStore } from './stub/store';

import 'view/styles/common.css';

const renderOptions = [
    { viewport: { width: 960, height: 1200 } },
    { viewport: { width: 625, height: 1200 } },
    { viewport: { width: 360, height: 1200 } },
];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
    Gate?: AnyObject;
    componentProps: {
        houseServicesSettings?: IHouseServicesSettings;
        houseServices?: IHouseService[];
        onCounterClick: FunctionReturnAny<[IHouseServiceMeter]>;
        flatId?: FlatId;
    };
}> = (props) => (
    <div style={{ padding: '20px' }}>
        <AppProvider initialState={props.store} Gate={props.Gate} rootReducer={userReducer}>
            <HouseServicesSettingsPreviewContainer {...props.componentProps} />
        </AppProvider>
    </div>
);

describe('HouseServicesSettingsPreview', () => {
    describe(`Сценарий оплаты жильцом`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={tenantFilledStore}
                        componentProps={{
                            flatId: '' as FlatId,
                            houseServicesSettings: tenantFilledStore.houseServicesSettings
                                ?.settings as IHouseServicesSettings,
                            houseServices: tenantFilledStore.houseServicesSettings?.houseServices as IHouseService[],
                            onCounterClick: noop,
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Сценарий оплаты собственником`, () => {
        describe(`Все поля заполнены`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(
                        <Component
                            store={ownerFilledStore}
                            componentProps={{
                                flatId: '' as FlatId,
                                houseServicesSettings: ownerFilledStore.houseServicesSettings
                                    ?.settings as IHouseServicesSettings,
                                houseServices: ownerFilledStore.houseServicesSettings?.houseServices as IHouseService[],
                                onCounterClick: noop,
                            }}
                        />,
                        renderOption
                    );

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Минимальное заполнение`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(
                        <Component
                            store={ownerMinFilledStore}
                            componentProps={{
                                flatId: '' as FlatId,
                                houseServicesSettings: ownerMinFilledStore.houseServicesSettings
                                    ?.settings as IHouseServicesSettings,
                                onCounterClick: noop,
                            }}
                        />,
                        renderOption
                    );

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(
                    <Component
                        store={skeletonStore}
                        componentProps={{
                            flatId: '' as FlatId,
                            houseServicesSettings: skeletonStore.houseServicesSettings
                                ?.settings as IHouseServicesSettings,
                            onCounterClick: noop,
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
