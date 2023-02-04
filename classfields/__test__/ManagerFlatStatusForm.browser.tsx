import React from 'react';
import { render } from 'jest-puppeteer-react';

import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { FlatStatus } from 'types/flat';

import { Fields } from 'view/modules/managerFlatStatusForm/types';
import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';
import { IUniversalStore } from 'view/modules/types';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import modalStyles from 'view/components/Modal/ConfirmActionModal/styles.module.css';
import styles from 'view/components/ManagerFlat/ManagerFlatStatusForm/styles.module.css';
import ModalDisplay from 'view/components/ModalDisplay';

import { ManagerFlatContainer } from '../container';
import { ManagerFlatTab } from '../types';

import * as stubs from './stubs/flatStatus';

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
    amoStatus: `#${Fields.CHECK_AMO_STATUS}`,
    confirmButton: `.${modalStyles.buttons} :nth-child(1)`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        updateStatus: `.${stepByStepModalStyles.modal} #${Fields.NEW_STATUS}`,
        responsibleUser: `.${stepByStepModalStyles.modal} #${Fields.RESPONSIBLE_MANAGER}`,
        amoStatus: `.${stepByStepModalStyles.modal} #${Fields.CHECK_AMO_STATUS}`,
    },
};

const Component: React.FC<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({ store, Gate }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
            <ManagerFlatContainer tab={ManagerFlatTab.CHANGE_FLAT_STATUS} />
            <ModalDisplay />
        </AppProvider>
    );
};

describe('ManagerFlatStatusForm', () => {
    describe('Скелетон', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.storeWithSkeleton} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    Object.values(FlatStatus).forEach((status) => {
        describe(`Квартира в статусе ${status}`, () => {
            Object.values(renderOptions).forEach((option) => {
                it(`width:${option.viewport.width}px`, async () => {
                    await render(<Component store={stubs.getStore(status)} />, option);

                    expect(await takeScreenshot()).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Заполнение всех полей и успешная отправка формы', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_status_form': {
                        return Promise.resolve({
                            flat: stubs.getStore(FlatStatus.WORK_IN_PROGRESS).managerFlat,
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(FlatStatus.CONFIRMED)} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.focus(selectors.responsibleUser);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                const checkbox = await page.$(selectors.amoStatus);
                await checkbox?.focus();
                await checkbox?.press('Space');

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
                await render(<Component store={stubs.getStore(FlatStatus.CONFIRMED)} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                const checkbox = await page.$(selectors.amoStatus);
                await checkbox?.focus();
                await checkbox?.press('Space');

                await page.click(selectors.button);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Бекенд вернул ошибку', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_status_form': {
                        return Promise.reject();
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getStore(FlatStatus.CONFIRMED)} Gate={Gate} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.focus(selectors.responsibleUser);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                const checkbox = await page.$(selectors.amoStatus);
                await checkbox?.focus();
                await checkbox?.press('Space');

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
                await render(<Component store={stubs.getStore(FlatStatus.CONFIRMED)} Gate={Gate} />, option);

                await page.focus(selectors.updateStatus);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                await page.focus(selectors.responsibleUser);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');

                const checkbox = await page.$(selectors.amoStatus);
                await checkbox?.focus();
                await checkbox?.press('Space');

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
                            flat: stubs.getStore(FlatStatus.WORK_IN_PROGRESS).managerFlat,
                        });
                    }
                }
            },
        };

        const store = {
            ...stubs.getStore(FlatStatus.CONFIRMED),
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

            await page.focus(selectors.stepByStep.responsibleUser);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            await page.click(selectors.stepByStep.right);

            const checkbox = await page.$(selectors.stepByStep.amoStatus);
            await checkbox?.focus();
            await checkbox?.press('Space');
            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.button);
            await page.click(selectors.confirmButton);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
