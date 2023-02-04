import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { HouseServicesResponsibleForPayment, HouseServicesSettingsStatus } from 'app/libs/house-services/settings-form';

import { Fields } from 'view/modules/houseServicesSettingsForm/types';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';

import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
// eslint-disable-next-line max-len
import formStyles from 'view/components/HouseServicesSettings/HouseServicesSettingsForm/styles.module.css';

import { HouseServicesSettingsFormContainer } from '../container';

import { mobileStore, skeletonStore, store } from './stub/store';

const renderOptions = [{ viewport: { width: 625, height: 1000 } }, { viewport: { width: 415, height: 1000 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={rootReducer} Gate={props.Gate} initialState={props.store}>
        <HouseServicesSettingsFormContainer />
    </AppProvider>
);

const chooseRadio = (radioInput: string, n: number) => `${radioInput} .Radio:nth-child(${n})`;

const selectors = {
    submitBtn: `.${formStyles.button}.Button`,
    inputs: {
        tenantRefundPaymentsDescription: `#${Fields.TENANT_REFUND_PAYMENTS_DESCRIPTION}`,
        tenantRefundPaymentAmount: `#${Fields.TENANT_REFUND_PAYMENT_AMOUNT}`,
        paidByTenantHouseServices: `#${Fields.PAID_BY_TENANT_HOUSE_SERVICES}`,
        paidByTenantAmount: `#${Fields.PAID_BY_TENANT_AMOUNT}`,
        paymentDetails: `#${Fields.PAYMENT_DETAILS}`,
        paymentAmount: `#${Fields.PAYMENT_AMOUNT}`,
    },
    radio: {
        responsibleForPayment: `#${Fields.RESPONSIBLE_FOR_PAYMENT}`,
        shouldTenantRefund: `#${Fields.SHOULD_TENANT_REFUND}`,
        hasServicesPaidByTenant: `#${Fields.HAS_SERVICES_PAID_BY_TENANT}`,
    },
    checkboxes: {
        paymentConfirmation: `#${Fields.PAYMENT_CONFIRMATION}`,
        shouldSendReadings: `#${Fields.SHOULD_SEND_READINGS}`,
        shouldSendReceiptPhotos: `#${Fields.SHOULD_SEND_RECEIPT_PHOTOS}`,
    },
    stepByStep: {
        left: `.Portal:last-of-type .${stepByStepModalStyles.leftButton}`,
        right: `.Portal:last-of-type .${stepByStepModalStyles.rightButton}`,
        close: `.Portal:last-of-type .${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: {
            responsibleForPayment: `.${stepByStepModalStyles.modal} #${Fields.RESPONSIBLE_FOR_PAYMENT}`,
            shouldTenantRefund: `.${stepByStepModalStyles.modal} #${Fields.SHOULD_TENANT_REFUND}`,
            hasServicesPaidByTenant: `.${stepByStepModalStyles.modal} #${Fields.HAS_SERVICES_PAID_BY_TENANT}`,
            // eslint-disable-next-line max-len
            tenantRefundPaymentsDescription: `.${stepByStepModalStyles.modal} #${Fields.TENANT_REFUND_PAYMENTS_DESCRIPTION}`,
            // eslint-disable-next-line max-len
            tenantRefundPaymentAmount: `.${stepByStepModalStyles.modal} #${Fields.TENANT_REFUND_PAYMENT_AMOUNT}`,
            paidByTenantHouseServices: `.${stepByStepModalStyles.modal} #${Fields.PAID_BY_TENANT_HOUSE_SERVICES}`,
            paidByTenantAmount: `.${stepByStepModalStyles.modal} #${Fields.PAID_BY_TENANT_AMOUNT}`,

            paymentDetails: `.${stepByStepModalStyles.modal} #${Fields.PAYMENT_DETAILS}`,
            paymentAmount: `.${stepByStepModalStyles.modal} #${Fields.PAYMENT_AMOUNT}`,
        },
        checkboxes: {
            paymentConfirmation: `.${stepByStepModalStyles.modal} #${Fields.PAYMENT_CONFIRMATION}`,
            shouldSendReadings: `.${stepByStepModalStyles.modal} #${Fields.SHOULD_SEND_READINGS}`,
            shouldSendReceiptPhotos: `.${stepByStepModalStyles.modal} #${Fields.SHOULD_SEND_RECEIPT_PHOTOS}`,
        },
    },
};

describe('HouseServicesSettingsForm', () => {
    describe('Внешний вид', () => {
        describe(`Базовое состояние`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Скелетон`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={skeletonStore} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Заполнение', () => {
        describe(`${renderOptions[1].viewport.width}px`, () => {
            it('Все поля и отправка', async () => {
                await render(<Component store={store} />, renderOptions[1]);

                await page.click(chooseRadio(selectors.radio.responsibleForPayment, 2));

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(chooseRadio(selectors.radio.shouldTenantRefund, 1));

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.tenantRefundPaymentsDescription, 'Вода, электричество');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.tenantRefundPaymentAmount, '3670');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(chooseRadio(selectors.radio.hasServicesPaidByTenant, 1));

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.paidByTenantHouseServices, 'Интернет, ТВ');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.paidByTenantAmount, '1560');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const paymentConfirmation = await page.$(selectors.checkboxes.paymentConfirmation);
                await paymentConfirmation?.focus();
                await paymentConfirmation?.press('Space');

                const shouldSendReadings = await page.$(selectors.checkboxes.shouldSendReadings);
                await shouldSendReadings?.focus();
                await shouldSendReadings?.press('Space');

                const shouldSendReceiptPhotos = await page.$(selectors.checkboxes.shouldSendReceiptPhotos);
                await shouldSendReceiptPhotos?.focus();
                await shouldSendReceiptPhotos?.press('Space');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submitBtn);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Всех поля при оплате жильцом', async () => {
                await render(<Component store={store} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(chooseRadio(selectors.radio.responsibleForPayment, 2));

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.paymentDetails, '4323 2342 2342 2342');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.paymentAmount, '4400');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const paymentConfirmation = await page.$(selectors.checkboxes.paymentConfirmation);
                await paymentConfirmation?.focus();
                await paymentConfirmation?.press('Space');

                const shouldSendReadings = await page.$(selectors.checkboxes.shouldSendReadings);
                await shouldSendReadings?.focus();
                await shouldSendReadings?.press('Space');

                const shouldSendReceiptPhotos = await page.$(selectors.checkboxes.shouldSendReceiptPhotos);
                await shouldSendReceiptPhotos?.focus();
                await shouldSendReceiptPhotos?.press('Space');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submitBtn);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            describe('показ ошибок валидации бэка responsibleForPayment', () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'user.update_house_services_settings': {
                                return Promise.resolve({
                                    validationErrors: [
                                        {
                                            parameter: 'responsibleForPayment',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                    ],
                                });
                            }
                        }
                    },
                };

                renderOptions.forEach((renderOption) => {
                    it(`${renderOption.viewport.width}px`, async () => {
                        await render(<Component store={store} Gate={Gate} />, renderOption);

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                        await page.click(selectors.submitBtn);

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                    });
                });
            });

            describe('показ ошибок валидации бэка hasServicesPaidByTenant', () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'user.update_house_services_settings': {
                                return Promise.resolve({
                                    validationErrors: [
                                        {
                                            parameter: 'hasServicesPaidByTenant',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                        {
                                            parameter: 'shouldTenantRefund',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                    ],
                                });
                            }
                        }
                    },
                };

                renderOptions.forEach((renderOption) => {
                    it(`${renderOption.viewport.width}px`, async () => {
                        await render(<Component store={store} Gate={Gate} />, renderOption);

                        await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                        await page.click(selectors.submitBtn);

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                    });
                });
            });

            describe('показ ошибок валидации бэка tenantRefundPaymentsDescription', () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'user.update_house_services_settings': {
                                return Promise.resolve({
                                    validationErrors: [
                                        {
                                            parameter: 'tenantRefundPaymentsDescription',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                        {
                                            parameter: 'tenantRefundPaymentAmount',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                        {
                                            parameter: 'paidByTenantHouseServices',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                        {
                                            parameter: 'paidByTenantAmount',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                    ],
                                });
                            }
                        }
                    },
                };

                renderOptions.forEach((renderOption) => {
                    it(`${renderOption.viewport.width}px`, async () => {
                        await render(<Component store={store} Gate={Gate} />, renderOption);

                        await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));
                        await page.click(chooseRadio(selectors.radio.shouldTenantRefund, 1));
                        await page.click(chooseRadio(selectors.radio.hasServicesPaidByTenant, 1));

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                        await page.click(selectors.submitBtn);

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                    });
                });
            });

            describe('показ ошибок валидации бэка paymentDetails', () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'user.update_house_services_settings': {
                                return Promise.resolve({
                                    validationErrors: [
                                        {
                                            parameter: 'paymentDetails',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                        {
                                            parameter: 'paymentAmount',
                                            localizedDescription: 'Обязательное поле',
                                        },
                                    ],
                                });
                            }
                        }
                    },
                };

                renderOptions.forEach((renderOption) => {
                    it(`${renderOption.viewport.width}px`, async () => {
                        await render(<Component store={store} Gate={Gate} />, renderOption);

                        await page.click(chooseRadio(selectors.radio.responsibleForPayment, 2));

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                        await page.click(selectors.submitBtn);

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                    });
                });
            });

            describe('Попап пошаговости и клик на инпуты', () => {
                Object.keys(selectors.inputs).forEach((key) => {
                    it(key, async () => {
                        await render(<Component store={mobileStore} />, renderOptions[1]);

                        await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));
                        await page.click(chooseRadio(selectors.radio.shouldTenantRefund, 1));
                        await page.click(chooseRadio(selectors.radio.hasServicesPaidByTenant, 1));

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                        if (['paymentDetails', 'paymentAmount'].includes(key)) {
                            await page.click(chooseRadio(selectors.radio.responsibleForPayment, 2));
                        }

                        await page.click(selectors.inputs[key]);

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                    });
                });
            });

            describe('Пошаговая формой', () => {
                it('Заполнение всех полей', async () => {
                    await render(<Component store={mobileStore} />, renderOptions[1]);

                    await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));
                    await page.click(chooseRadio(selectors.radio.shouldTenantRefund, 1));
                    await page.click(chooseRadio(selectors.radio.hasServicesPaidByTenant, 1));

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.inputs.tenantRefundPaymentsDescription);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.stepByStep.inputs.tenantRefundPaymentsDescription, 'Вода, электричество');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.right);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.stepByStep.inputs.tenantRefundPaymentAmount, '3670');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.close);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.inputs.paidByTenantHouseServices);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.stepByStep.inputs.paidByTenantHouseServices, 'Интернет, ТВ');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.right);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.stepByStep.inputs.paidByTenantAmount, '1560');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.close);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it('Корректно переключается вперед и назад в пошаговой форме', async () => {
                    await render(<Component store={mobileStore} />, renderOptions[1]);

                    await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));
                    await page.click(chooseRadio(selectors.radio.shouldTenantRefund, 1));
                    await page.click(chooseRadio(selectors.radio.hasServicesPaidByTenant, 1));

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.inputs.tenantRefundPaymentsDescription);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.stepByStep.inputs.tenantRefundPaymentsDescription, 'Вода, электричество');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.left);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.right);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.right);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.stepByStep.inputs.tenantRefundPaymentAmount, '3670');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.close);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.inputs.paidByTenantHouseServices);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.stepByStep.inputs.paidByTenantHouseServices, 'Интернет, ТВ');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.left);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.right);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.right);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.stepByStep.inputs.paidByTenantAmount, '1560');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.left);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.right);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.stepByStep.close);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it('Анкета в процессе сохранения', async () => {
                    const Gate = {
                        create: () => new Promise(noop),
                    };

                    await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));
                    await page.click(chooseRadio(selectors.radio.shouldTenantRefund, 1));
                    await page.type(selectors.inputs.tenantRefundPaymentsDescription, 'Вода, электричество');
                    await page.type(selectors.inputs.tenantRefundPaymentAmount, '3670');
                    await page.click(chooseRadio(selectors.radio.hasServicesPaidByTenant, 1));
                    await page.type(selectors.inputs.paidByTenantHouseServices, 'Интернет, ТВ');
                    await page.type(selectors.inputs.paidByTenantAmount, '1560');

                    const paymentConfirmation = await page.$(selectors.checkboxes.paymentConfirmation);
                    await paymentConfirmation?.focus();
                    await paymentConfirmation?.press('Space');

                    const shouldSendReadings = await page.$(selectors.checkboxes.shouldSendReadings);
                    await shouldSendReadings?.focus();
                    await shouldSendReadings?.press('Space');

                    const shouldSendReceiptPhotos = await page.$(selectors.checkboxes.shouldSendReceiptPhotos);
                    await shouldSendReceiptPhotos?.focus();
                    await shouldSendReceiptPhotos?.press('Space');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.submitBtn);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it('Не удалось сохранить анкету', async () => {
                    const Gate = {
                        create: () => Promise.reject(),
                    };

                    await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

                    await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));
                    await page.click(chooseRadio(selectors.radio.shouldTenantRefund, 1));
                    await page.type(selectors.inputs.tenantRefundPaymentsDescription, 'Вода, электричество');
                    await page.type(selectors.inputs.tenantRefundPaymentAmount, '3670');
                    await page.click(chooseRadio(selectors.radio.hasServicesPaidByTenant, 1));
                    await page.type(selectors.inputs.paidByTenantHouseServices, 'Интернет, ТВ');
                    await page.type(selectors.inputs.paidByTenantAmount, '1560');

                    const paymentConfirmation = await page.$(selectors.checkboxes.paymentConfirmation);
                    await paymentConfirmation?.focus();
                    await paymentConfirmation?.press('Space');

                    const shouldSendReadings = await page.$(selectors.checkboxes.shouldSendReadings);
                    await shouldSendReadings?.focus();
                    await shouldSendReadings?.press('Space');

                    const shouldSendReceiptPhotos = await page.$(selectors.checkboxes.shouldSendReceiptPhotos);
                    await shouldSendReceiptPhotos?.focus();
                    await shouldSendReceiptPhotos?.press('Space');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.submitBtn);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it('Анкета успешно сохранена', async () => {
                    const Gate = {
                        create: () => {
                            return Promise.resolve({
                                settings: {
                                    responsibleForPayment: HouseServicesResponsibleForPayment.OWNER,
                                    shouldSendReceiptPhotos: true,
                                    shouldSendReadings: true,
                                    shouldTenantRefund: true,
                                    tenantRefundPaymentsDescription: 'Вода, электричество',
                                    tenantRefundPaymentAmount: 3670,
                                    hasServicesPaidByTenant: true,
                                    paidByTenantHouseServices: 'Интернет, ТВ',
                                    paidByTenantAmount: 1560,
                                    paymentConfirmation: true,
                                    settingsStatus: HouseServicesSettingsStatus.DRAFT,
                                },
                            });
                        },
                    };

                    await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(chooseRadio(selectors.radio.responsibleForPayment, 1));
                    await page.click(chooseRadio(selectors.radio.shouldTenantRefund, 1));
                    await page.type(selectors.inputs.tenantRefundPaymentsDescription, 'Вода, электричество');
                    await page.type(selectors.inputs.tenantRefundPaymentAmount, '3670');
                    await page.click(chooseRadio(selectors.radio.hasServicesPaidByTenant, 1));
                    await page.type(selectors.inputs.paidByTenantHouseServices, 'Интернет, ТВ');
                    await page.type(selectors.inputs.paidByTenantAmount, '1560');

                    const paymentConfirmation = await page.$(selectors.checkboxes.paymentConfirmation);
                    await paymentConfirmation?.focus();
                    await paymentConfirmation?.press('Space');

                    const shouldSendReadings = await page.$(selectors.checkboxes.shouldSendReadings);
                    await shouldSendReadings?.focus();
                    await shouldSendReadings?.press('Space');

                    const shouldSendReceiptPhotos = await page.$(selectors.checkboxes.shouldSendReceiptPhotos);
                    await shouldSendReceiptPhotos?.focus();
                    await shouldSendReceiptPhotos?.press('Space');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.submitBtn);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });
});
