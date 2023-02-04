import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { IUserPaymentData, UserId } from 'types/user';

import { getFields } from 'app/libs/payment-data-form';

import { Fields } from 'view/modules/paymentDataForm/types';
import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';
import ModalDisplay from 'view/components/ModalDisplay';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import confirmActionModalStyles from 'view/components/Modal/ConfirmActionModal/styles.module.css';

import paymentDataStyles from '../styles.module.css';

import { PaymentDataFormContainer } from '../container';

import { mobileStore, skeletonStore, store, filledStore } from './stub/store';

const renderOptions = [{ viewport: { width: 625, height: 1000 } }, { viewport: { width: 415, height: 1000 } }];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
    Gate?: AnyObject;
    paymentData?: Partial<IUserPaymentData>;
    isManagerForm?: boolean;
}> = (props) => (
    <AppProvider rootReducer={userReducer} Gate={props.Gate} initialState={props.store}>
        <PaymentDataFormContainer
            paymentData={props.paymentData}
            isManagerForm={props.isManagerForm}
            userId={'123' as UserId}
        />
        <ModalDisplay />
    </AppProvider>
);

const selectors = {
    sendSubmit: `.${paymentDataStyles.button}.Button`,
    inputs: {
        name: `#${Fields.NAME}`,
        surname: `#${Fields.SURNAME}`,
        patronymic: `#${Fields.PATRONYMIC}`,
        inn: `#${Fields.INN}`,
        accountNumber: `#${Fields.ACCOUNT_NUMBER}`,
        bik: `#${Fields.BIK}`,
    },
    stepByStep: {
        left: `.Portal:last-of-type .${stepByStepModalStyles.leftButton}`,
        right: `.Portal:last-of-type .${stepByStepModalStyles.rightButton}`,
        close: `.Portal:last-of-type .${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: {
            name: `.${stepByStepModalStyles.modal} #${Fields.NAME}`,
            surname: `.${stepByStepModalStyles.modal} #${Fields.SURNAME}`,
            patronymic: `.${stepByStepModalStyles.modal} #${Fields.PATRONYMIC}`,
            inn: `.${stepByStepModalStyles.modal} #${Fields.INN}`,
            accountNumber: `.${stepByStepModalStyles.modal} #${Fields.ACCOUNT_NUMBER}`,
            bik: `.${stepByStepModalStyles.modal} #${Fields.BIK}`,
        },
    },
    confirmationModal: {
        confirm: `.${confirmActionModalStyles.buttons} .${confirmActionModalStyles.button}:first-of-type`,
        cancel: `.${confirmActionModalStyles.buttons} .${confirmActionModalStyles.button}:last-of-type`,
    },
};

const pressDelNTimes = async (n: number) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    for (const i of new Array(n).fill(1)) {
        await page.keyboard.press('Backspace');
    }
};

describe('PaymentDataForm', () => {
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

        describe(`У юзера сохраненный ИНН заблокирован`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    const paymentData = { inn: '380114872773' };

                    await render(
                        <Component
                            store={{ ...store, paymentDataForm: { fields: getFields(paymentData) } }}
                            paymentData={paymentData}
                            isManagerForm={false}
                        />,
                        renderOption
                    );

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`У юзера сохраненный ИНН заблокирован, но ФИО нет`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    const paymentData = {
                        inn: '380114872773',
                        person: { name: 'Имя', surname: 'Фамилия', patronymic: '' },
                    };

                    await render(
                        <Component
                            store={{ ...store, paymentDataForm: { fields: getFields(paymentData) } }}
                            paymentData={paymentData}
                            isManagerForm={false}
                        />,
                        renderOption
                    );

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Показ ошибок', () => {
        describe(`Не заполнены поля при клике на кнопку "Отправить"`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Некорректный ИНН БИК и Банковский счет`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    await page.type(selectors.inputs.name, 'Иван');
                    await page.type(selectors.inputs.surname, 'Иванов');
                    await page.type(selectors.inputs.patronymic, 'Иванович');
                    await page.type(selectors.inputs.inn, '3801148');
                    await page.type(selectors.inputs.accountNumber, '123');
                    await page.type(selectors.inputs.bik, '041112');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Заполнение формы', () => {
        it('Заполнение всех полей', async () => {
            await render(<Component store={store} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.name, 'Иван');
            await page.type(selectors.inputs.surname, 'Иванов');
            await page.type(selectors.inputs.patronymic, 'Иванович');
            await page.type(selectors.inputs.inn, '380114872773');
            await page.type(selectors.inputs.accountNumber, '12334124321312312122');
            await page.type(selectors.inputs.bik, '041231112');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Заполнение всех полей через пошаговость', async () => {
            await render(<Component store={mobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.inn);

            await page.type(selectors.stepByStep.inputs.inn, '380114872773');

            await page.click(selectors.stepByStep.right);

            await page.click(selectors.inputs.surname);

            await page.type(selectors.stepByStep.inputs.surname, 'Иванов');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.name, 'Иван');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.patronymic, 'Иванович');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.accountNumber, '12334124321312312122');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.bik, '041231112');

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Сохранение формы', () => {
        it('Анкета в процессе сохранения', async () => {
            const Gate = {
                create: () => new Promise(noop),
            };

            await render(<Component store={filledStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.sendSubmit);
            await page.click(selectors.confirmationModal.confirm);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Не удалось сохранить анкету', async () => {
            const Gate = {
                create: () => Promise.reject(),
            };

            await render(<Component store={filledStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.sendSubmit);
            await page.click(selectors.confirmationModal.confirm);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Анкета успешно сохранена', async () => {
            const Gate = {
                create: () => {
                    return Promise.resolve({
                        paymentData: {
                            person: {
                                name: 'Иван',
                                surname: 'Иванов',
                                patronymic: 'Иванович',
                            },
                            inn: '380114872773',
                            bik: '041231112',
                            accountNumber: '12334124321312312122',
                        },
                    });
                },
            };

            await render(<Component store={filledStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.sendSubmit);
            await page.click(selectors.confirmationModal.confirm);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Если пытаемся сохранить в форме ФИО при сохраненном ИНН, то нет модалки', async () => {
            const Gate = {
                create: () => {
                    return Promise.resolve({
                        paymentData: {
                            person: {
                                name: 'Иван',
                                surname: 'Иванов',
                                patronymic: 'Иванович',
                            },
                            inn: '380114872773',
                        },
                    });
                },
            };

            const paymentData = { inn: '380114872773' };

            await render(
                <Component
                    store={{ ...store, paymentDataForm: { fields: getFields(paymentData) } }}
                    Gate={Gate}
                    paymentData={paymentData}
                />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.name, 'Иван');
            await page.type(selectors.inputs.surname, 'Иванов');
            await page.type(selectors.inputs.patronymic, 'Иванович');

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Появляется валдиация ФИО при вводе БИК или Номера банковского счета', async () => {
            const Gate = {
                create: () => {
                    return Promise.resolve({
                        paymentData: {
                            person: {
                                name: 'Иван',
                                surname: 'Иванов',
                                patronymic: 'Иванович',
                            },
                            inn: '380114872773',
                        },
                    });
                },
            };

            const paymentData = { inn: '380114872773' };

            await render(
                <Component
                    store={{ ...store, paymentDataForm: { fields: getFields(paymentData) } }}
                    Gate={Gate}
                    paymentData={paymentData}
                />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.accountNumber, '1');

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.focus(selectors.inputs.accountNumber);
            await page.keyboard.press('Backspace');

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.bik, '2');

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.focus(selectors.inputs.bik);
            await page.keyboard.press('Backspace');

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Менеджер может удалить все поля, кроме ИНН, модалки нет', async () => {
            const Gate = {
                create: () => {
                    return Promise.resolve({
                        paymentData: { inn: '380114872773' },
                    });
                },
            };

            const paymentData = {
                person: {
                    name: 'Иван',
                    surname: 'Иванов',
                    patronymic: '',
                },
                inn: '380114872773',
                bik: '041231112',
                accountNumber: '12334124321312312122',
            };

            await render(
                <Component
                    store={{ ...store, paymentDataForm: { fields: getFields(paymentData) } }}
                    Gate={Gate}
                    isManagerForm={true}
                    paymentData={paymentData}
                />,
                renderOptions[0]
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.focus(selectors.inputs.name);

            await pressDelNTimes(4);

            await page.focus(selectors.inputs.surname);

            await pressDelNTimes(6);

            await page.focus(selectors.inputs.bik);

            await pressDelNTimes(9);

            await page.focus(selectors.inputs.accountNumber);

            await pressDelNTimes(20);

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
