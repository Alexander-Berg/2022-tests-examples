import React from 'react';
import { render } from 'jest-puppeteer-react';
import omit from 'lodash/omit';

import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { RentContractStatus } from 'types/contract';

import { Fields } from 'view/modules/managerFlatContractStatusForm/types';
import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';
import { IUniversalStore } from 'view/modules/types';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import modalStyles from 'view/components/Modal/ConfirmActionModal/styles.module.css';
import styles from 'view/components/ManagerFlatContract/ManagerFlatContractStatusForm/styles.module.css';
import ModalDisplay from 'view/components/ModalDisplay';

import { ManagerFlatContractTab } from '../types';
import { ManagerFlatContractContainer } from '../container';

import * as stubs from './stubs/contractStatus';

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
    responsibleUser: `#${Fields.RESPONSIBLE_MANAGER}`,
    terminateDate: `#${Fields.TERMINATE_DATE}`,
    terminatePrevetionDate: `#${Fields.TERMINATE_PREVENTION_DATE}`,
    terminationReasonCode: `#${Fields.TERMINATION_REASON_CODE}`,
    terminationSubReasonCode: `#${Fields.TERMINATION_SUB_REASON_CODE}`,
    checkOutWithoutAdditionalPayments: `#${Fields.CHECK_OUT_WITHOUT_ADDITIONAL_PAYMENTS}`,
    ownerContinuesWorkWithUs: `#${Fields.OWNER_CONTINUES_WORK_WITH_US}`,
    tenantRefusedPayFor30Days: `#${Fields.TENANT_REFUSED_PAY_FOR_30_DAYS}`,
    confirmButton: `.${modalStyles.buttons} :nth-child(1)`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        updateStatus: `.${stepByStepModalStyles.modal} #${Fields.NEW_STATUS}`,
        responsibleUser: `.${stepByStepModalStyles.modal} #${Fields.RESPONSIBLE_MANAGER}`,
        terminateDate: `.${stepByStepModalStyles.modal} #${Fields.TERMINATE_DATE}`,
        terminatePrevetionDate: `.${stepByStepModalStyles.modal} #${Fields.TERMINATE_PREVENTION_DATE}`,
        terminationReasonCode: `.${stepByStepModalStyles.modal} #${Fields.TERMINATION_REASON_CODE}`,
        terminationSubReasonCode: `.${stepByStepModalStyles.modal} #${Fields.TERMINATION_SUB_REASON_CODE}`,
        // eslint-disable-next-line max-len
        checkOutWithoutAdditionalPayments: `.${stepByStepModalStyles.modal} #${Fields.CHECK_OUT_WITHOUT_ADDITIONAL_PAYMENTS}`,
        ownerContinuesWorkWithUs: `.${stepByStepModalStyles.modal} #${Fields.OWNER_CONTINUES_WORK_WITH_US}`,
        isCheckInDateSameAsCheckOut: `.${stepByStepModalStyles.modal} #${Fields.IS_CHECK_IN_DATE_SAME_AS_CHECK_OUT}`,
        tenantRefusedPayFor30Days: `.${stepByStepModalStyles.modal} #${Fields.TENANT_REFUSED_PAY_FOR_30_DAYS}`,
    },
};

const Component: React.FC<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({ store, Gate }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
            <ManagerFlatContractContainer tab={ManagerFlatContractTab.CHANGE_STATUS} />
            <ModalDisplay />
        </AppProvider>
    );
};

describe('ManagerFlatContractStatusForm', () => {
    describe('Скелетон', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.storeWithSkeleton} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    Object.values(omit(RentContractStatus, [RentContractStatus.UNKNOWN, RentContractStatus.UNRECOGNIZED])).forEach(
        (status) => {
            describe(`Договор в статусе ${status}`, () => {
                Object.values(renderOptions).forEach((option) => {
                    it(`width:${option.viewport.width}px`, async () => {
                        await render(<Component store={stubs.getStore(status)} />, option);

                        expect(await takeScreenshot()).toMatchImageSnapshot();
                    });
                });
            });
        }
    );

    describe('Заполнение всех полей и успешная отправка формы', () => {
        const Gate = {
            create: (path: string) => {
                const { managerFlat, managerFlatContract } = stubs.getStore(RentContractStatus.ACTIVE);

                switch (path) {
                    case 'manager.update_flat_contract_status_form': {
                        return Promise.resolve({
                            flat: managerFlat,
                            contract: managerFlatContract,
                            contractId: managerFlat?.actualContract?.contractId,
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(RentContractStatus.SIGNING)} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.focus(selectors.responsibleUser);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.button);
                await page.click(selectors.confirmButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Валидация полей', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(RentContractStatus.ACTIVE)} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.responsibleUser);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.button);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Бекенд вернул ошибку', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_contract_status_form': {
                        return Promise.reject();
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(RentContractStatus.SIGNING)} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.focus(selectors.responsibleUser);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.button);
                await page.click(selectors.confirmButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Отправка формы в процессе', () => {
        const Gate = {
            create: () => new Promise(noop),
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(RentContractStatus.SIGNING)} Gate={Gate} />, option);

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.focus(selectors.responsibleUser);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.click(selectors.button);
                await page.click(selectors.confirmButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Расторжение договора', () => {
        const Gate = {
            create: (path: string) => {
                const { managerFlat, managerFlatContract } = stubs.getStore(RentContractStatus.FIXED_TERM);

                switch (path) {
                    case 'manager.update_flat_contract_status_form': {
                        return Promise.resolve({
                            flat: managerFlat,
                            contract: managerFlatContract,
                            contractId: managerFlat?.actualContract?.contractId,
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(RentContractStatus.ACTIVE)} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.focus(selectors.terminationReasonCode);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.focus(selectors.terminationSubReasonCode);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.type(selectors.terminateDate, '12.11.2021');
                await page.type(selectors.terminatePrevetionDate, '11.11.2021');

                await page.focus(selectors.ownerContinuesWorkWithUs);
                await page.keyboard.press('ArrowUp');
                await page.keyboard.press('Enter');

                const checkboxPayments = await page.$(selectors.checkOutWithoutAdditionalPayments);
                await checkboxPayments?.focus();
                await checkboxPayments?.press('Space');
                const checkbox30Days = await page.$(selectors.tenantRefusedPayFor30Days);
                await checkbox30Days?.focus();
                await checkbox30Days?.press('Space');

                await page.focus(selectors.responsibleUser);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.button);
                await page.click(selectors.confirmButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Работа с формой пошаговости', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_status_form': {
                        return Promise.resolve({
                            flat: stubs.getStore(RentContractStatus.FIXED_TERM).managerFlat,
                        });
                    }
                }
            },
        };

        const store = {
            ...stubs.getStore(RentContractStatus.ACTIVE),
            config: {
                isMobile: 'mobile',
            },
        };

        const option = renderOptions.mobile;

        it(`Заполнение всех полей и отправка формы`, async () => {
            await render(<Component store={store} Gate={Gate} />, option);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.updateStatus);

            await page.focus(selectors.stepByStep.updateStatus);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            await page.click(selectors.stepByStep.right);

            await page.focus(selectors.stepByStep.terminationReasonCode);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            await page.click(selectors.stepByStep.right);

            await page.focus(selectors.stepByStep.terminationSubReasonCode);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.terminateDate, '12.11.2021');
            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.terminatePrevetionDate, '11.11.2021');
            await page.click(selectors.stepByStep.right);

            await page.focus(selectors.stepByStep.ownerContinuesWorkWithUs);
            await page.keyboard.press('ArrowUp');
            await page.keyboard.press('Enter');
            await page.click(selectors.stepByStep.right);

            await page.$(selectors.stepByStep.isCheckInDateSameAsCheckOut);
            await page.click(selectors.stepByStep.right);

            const checkboxPayments = await page.$(selectors.stepByStep.checkOutWithoutAdditionalPayments);
            await checkboxPayments?.focus();
            await checkboxPayments?.press('Space');
            await page.click(selectors.stepByStep.right);

            const checkbox30Days = await page.$(selectors.stepByStep.tenantRefusedPayFor30Days);
            await checkbox30Days?.focus();
            await checkbox30Days?.press('Space');
            await page.click(selectors.stepByStep.right);

            await page.focus(selectors.stepByStep.responsibleUser);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.button);
            await page.click(selectors.confirmButton);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
