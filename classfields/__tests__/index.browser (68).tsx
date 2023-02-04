import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RequestStatus } from 'realty-core/types/network';

import { getFields } from 'app/libs/payment-data-form';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { OwnerPaymentDataFormContainer } from '../container';

const renderOptions = [{ viewport: { width: 625, height: 1000 } }, { viewport: { width: 415, height: 1000 } }];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
}> = (props) => (
    <AppProvider initialState={props.store} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
        <OwnerPaymentDataFormContainer />
    </AppProvider>
);

describe('OwnerPaymentDataPreviewForm', () => {
    describe(`Частично заполнена платежная информация`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                const paymentData = {
                    person: { name: 'Роман', surname: 'Полегуев', patronymic: '' },
                    inn: '76999322146',
                };

                await render(
                    <Component
                        store={{
                            spa: { status: RequestStatus.LOADED },
                            legacyUser: {
                                paymentData,
                            },
                            paymentDataForm: { fields: getFields(paymentData), network: {} },
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
                const paymentData = {};

                await render(
                    <Component
                        store={{
                            spa: { status: RequestStatus.LOADED },
                            legacyUser: { paymentData },
                            paymentDataForm: { fields: getFields(paymentData), network: {} },
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
                const paymentData = {};

                await render(
                    <Component
                        store={{
                            spa: { status: RequestStatus.PENDING },
                            legacyUser: {
                                paymentData,
                            },
                            paymentDataForm: { fields: getFields(paymentData), network: {} },
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
