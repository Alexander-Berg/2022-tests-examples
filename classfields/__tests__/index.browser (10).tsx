import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';
import ModalDisplay from 'view/components/ModalDisplay';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';

import { HouseServicesMeterReadingsFormContainer } from '../container';

import styles from '../styles.module.css';

import * as stub from './stub';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 375, height: 300 } }];

const selectors = {
    inputs: {
        counterValue0: '#COUNTER_VALUE_0',
        counterValue1: '#COUNTER_VALUE_1',
        counterValue2: '#COUNTER_VALUE_2',
    },
    submitButton: `.${styles.button}`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: {
            counterValue0: `.${stepByStepModalStyles.modal} #COUNTER_VALUE_0`,
            counterValue1: `.${stepByStepModalStyles.modal} #COUNTER_VALUE_1`,
            counterValue2: `.${stepByStepModalStyles.modal} #COUNTER_VALUE_2`,
        },
    },
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={rootReducer} Gate={props.Gate} initialState={props.store}>
        <HouseServicesMeterReadingsFormContainer />
        <ModalDisplay />
    </AppProvider>
);

describe('HouseServicesMeterReadingsForm', () => {
    describe('Внешний вид формы передачи показаний', () => {
        describe('однотарифный счётчик', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={stub.storeSingle} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('двухтарифный счётчик', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={stub.storeDouble} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('трёхтарифный счётчик', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={stub.storeTriple} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Показ скелетона', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={stub.skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение формы через попап пошаговости', () => {
        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={stub.mobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.counterValue0);
            await page.type(selectors.stepByStep.inputs.counterValue0, '100');
            await page.click(selectors.stepByStep.right);
            await page.type(selectors.stepByStep.inputs.counterValue1, '200');
            await page.click(selectors.stepByStep.right);
            await page.type(selectors.stepByStep.inputs.counterValue2, '300');

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Сохранение формы', () => {
        it('Форма в процессе сохранения', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.send_house_services_period_meter_readings': {
                            return new Promise(noop);
                        }
                    }
                },
            };

            await render(<Component store={stub.filledStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Не удалось сохранить форму', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.send_house_services_period_meter_readings': {
                            return Promise.reject({ error: { message: '' } });
                        }
                    }
                },
            };

            await render(<Component store={stub.filledStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Форма успешно сохранена', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.send_house_services_period_meter_readings': {
                            return Promise.resolve();
                        }
                    }
                },
            };

            await render(<Component store={stub.filledStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
