import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RequestStatus } from 'realty-core/types/network';

import { IUserPaymentData } from 'types/user';
import { FlatId } from 'types/flat';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { PaymentDataPreview } from '../';

const renderOptions = [{ viewport: { width: 625, height: 1000 } }, { viewport: { width: 415, height: 1000 } }];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
    paymentData: Partial<IUserPaymentData>;
}> = (props) => (
    <AppProvider initialState={props.store}>
        <PaymentDataPreview paymentData={props.paymentData} flatId={'3b3f8741ac5c4f618e7db30055535ca7' as FlatId} />
    </AppProvider>
);

describe('PaymentDataPreview', () => {
    describe(`Базовое состояние`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component
                        store={{ spa: { status: RequestStatus.LOADED } }}
                        paymentData={{
                            person: { name: 'Роман', surname: 'Полегуев', patronymic: '' },
                            bik: '436123125',
                            accountNumber: '123215676532421316',
                            inn: '76999322146',
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
                        store={{ spa: { status: RequestStatus.LOADED } }}
                        paymentData={{
                            person: { name: 'Роман', surname: 'Полегуев', patronymic: '' },
                            inn: '76999322146',
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Не заполнена платежная информация`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component store={{ spa: { status: RequestStatus.LOADED } }} paymentData={{}} />,
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
                        store={{ spa: { status: RequestStatus.PENDING } }}
                        paymentData={{
                            person: { name: 'Роман', surname: 'Полегуев', patronymic: '' },
                            inn: '76999322146',
                        }}
                    />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
