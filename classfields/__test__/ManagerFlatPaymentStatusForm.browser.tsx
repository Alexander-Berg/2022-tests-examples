import React from 'react';
import { render } from 'jest-puppeteer-react';

import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { PaymentStatus } from 'types/payment';

import { Fields } from 'view/modules/managerFlatPaymentStatusForm/types';
import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';
import { IUniversalStore } from 'view/modules/types';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import modalStyles from 'view/components/Modal/ConfirmActionModal/styles.module.css';
import styles from 'view/components/ManagerFlat/ManagerFlatPayment/ManagerFlatPaymentStatus/styles.module.css';
import ModalDisplay from 'view/components/ModalDisplay';

import { ManagerFlatPaymentContainer } from '../container';
import { ManagerFlatPaymentTab } from '../types';

import * as stubs from './stubs/paymentStatus';

const renderOptions = {
    desktop: {
        viewport: {
            width: 1400,
            height: 1000,
        },
    },
    mobile: {
        viewport: {
            width: 375,
            height: 900,
        },
    },
};

const selectors = {
    button: `.${styles.button}`,
    updateStatus: `#${Fields.NEW_STATUS}`,
    paymentDate: `#${Fields.PAYMENT_DATE}`,
    confirmButton: `.${modalStyles.buttons} :nth-child(1)`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        updateStatus: `.${stepByStepModalStyles.modal} #${Fields.NEW_STATUS}`,
        paymentDate: `.${stepByStepModalStyles.modal} #${Fields.PAYMENT_DATE}`,
    },
};

const Component: React.FC<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({ store, Gate }) => {
    return (
        <AppProvider
            fakeTimers={{
                now: new Date('2021-11-12T03:00:00.111Z').getTime(),
            }}
            rootReducer={rootReducer}
            initialState={store}
            Gate={Gate}
        >
            <ManagerFlatPaymentContainer tab={ManagerFlatPaymentTab.PAYMENT_STATUS} />
            <ModalDisplay />
        </AppProvider>
    );
};

describe('ManagerFlatPaymentStatusForm', () => {
    describe('Скелетон', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.storeWithSkeleton} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    Object.values(PaymentStatus).forEach((status) => {
        describe(`Квартира в статусе ${status}`, () => {
            Object.values(renderOptions).forEach((option) => {
                it(`width:${option.viewport.width}px`, async () => {
                    await render(<Component store={stubs.getStore(status)} />, option);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Не выбран новый статус платежа', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(PaymentStatus.NEW)} />, option);

                await page.type(selectors.paymentDate, '11.11.2021');
                await page.click(selectors.button);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Нельзя указать дату в будущем', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(PaymentStatus.NEW)} />, option);

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.type(selectors.paymentDate, '12.11.2026');

                await page.click(selectors.button);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Не указана дата', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(PaymentStatus.NEW)} />, option);

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.click(selectors.button);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение всех полей и успешная отправка формы', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_payment_status': {
                        return Promise.resolve({
                            payment: stubs.getStore(PaymentStatus.PAID_BY_TENANT).managerFlatPayment
                                ?.paymentWithContract?.payment,
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(PaymentStatus.NEW)} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.type(selectors.paymentDate, '12.11.2021');

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.button);
                await page.click(selectors.confirmButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Перевод платежа собственнику', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_payment_status': {
                        return Promise.resolve({
                            payment: stubs.getStore(PaymentStatus.PAID_TO_OWNER).managerFlatPayment?.paymentWithContract
                                ?.payment,
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(PaymentStatus.NEW)} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.type(selectors.paymentDate, '12.11.2021');

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.button);
                await page.click(selectors.confirmButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Отпавка формы в процессе', () => {
        const Gate = {
            create: () => {
                return new Promise(noop);
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(PaymentStatus.NEW)} Gate={Gate} />, option);

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.type(selectors.paymentDate, '12.11.2021');

                await page.click(selectors.button);
                await page.click(selectors.confirmButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Бэкенд вернул ошибку', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_payment_status': {
                        return Promise.reject();
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(PaymentStatus.NEW)} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.type(selectors.paymentDate, '12.11.2021');

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.button);
                await page.click(selectors.confirmButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Работа формы пошаговости', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_payment_status': {
                        return Promise.resolve({
                            payment: stubs.getStore(PaymentStatus.PAID_BY_TENANT).managerFlatPayment
                                ?.paymentWithContract?.payment,
                        });
                    }
                }
            },
        };

        const option = renderOptions.mobile;

        it(`Заполнение всех полей и отправка формы`, async () => {
            await render(<Component store={stubs.getStore(PaymentStatus.NEW, 'mobile')} Gate={Gate} />, option);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.updateStatus);

            await page.focus(selectors.stepByStep.updateStatus);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.paymentDate, '12.11.2021');
            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.button);
            await page.click(selectors.confirmButton);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
