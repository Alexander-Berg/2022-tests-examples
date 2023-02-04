import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RequestStatus } from 'realty-core/types/network';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { OwnerPaymentDataPreviewContainer } from '../container';

const renderOptions = [{ viewport: { width: 625, height: 1000 } }, { viewport: { width: 415, height: 1000 } }];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
}> = (props) => (
    <AppProvider initialState={props.store} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
        <OwnerPaymentDataPreviewContainer />
    </AppProvider>
);

describe('OwnerPaymentDataPreview', () => {
    describe(`Базовое состояние`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={{
                            spa: { status: RequestStatus.LOADED },
                            legacyUser: {
                                paymentData: {
                                    person: { name: 'Роман', surname: 'Полегуев', patronymic: '' },
                                    bik: '436123125',
                                    accountNumber: '123215676532421316',
                                    inn: '76999322146',
                                },
                            },
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Частично заполнена платежная информация`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={{
                            spa: { status: RequestStatus.LOADED },
                            legacyUser: {
                                paymentData: {
                                    person: { name: 'Роман', surname: 'Полегуев', patronymic: '' },
                                    inn: '76999322146',
                                },
                            },
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Не заполнен ИНН`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={{
                            spa: { status: RequestStatus.LOADED },
                            legacyUser: {
                                paymentData: {},
                            },
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Скелетон`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={{
                            spa: { status: RequestStatus.PENDING },
                            legacyUser: {
                                paymentData: {},
                            },
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
