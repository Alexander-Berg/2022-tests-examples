import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { UserFlatContentWrapper } from 'view/components/UserFlatContentWrapper';
import { rootReducer } from 'view/entries/user/reducer';
import ModalDisplay from 'view/components/ModalDisplay';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';

import { HouseServiceFormContainer } from '../container';

import styles from '../styles.module.css';

import * as stub from './stub';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 375, height: 300 } }];

const selectors = {
    inputs: {
        counterNumber: '#COUNTER_NUMBER',
        installedPlace: '#INSTALLED_PLACE',
        deliverFromDay: '#DELIVER_FROM_DAY',
        deliverToDay: '#DELIVER_TO_DAY',
        currentValue1: '#CURRENT_VALUE_1',
        currentPhoto1: '#COUNTER_PHOTO_1 ~ div',
        tariff: '#TARIFF',
    },
    submitButton: `.${styles.submitButton}`,
    deleteButton: `.${styles.deleteButton}`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: {
            counterNumber: `.${stepByStepModalStyles.modal} #COUNTER_NUMBER`,
            installedPlace: `.${stepByStepModalStyles.modal} #INSTALLED_PLACE`,
            deliverFromDay: `.${stepByStepModalStyles.modal} #DELIVER_FROM_DAY`,
            deliverToDay: `.${stepByStepModalStyles.modal} #DELIVER_TO_DAY`,
            currentValue1: `.${stepByStepModalStyles.modal} #CURRENT_VALUE_1`,
            tariff: `.${stepByStepModalStyles.modal} #TARIFF`,
        },
    },
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={rootReducer} Gate={props.Gate} initialState={props.store}>
        <UserFlatContentWrapper>
            <HouseServiceFormContainer />
        </UserFlatContentWrapper>
        <ModalDisplay />
    </AppProvider>
);

describe('HouseServiceForm', () => {
    describe('Внешний вид формы создания', () => {
        describe('однотарифный счётчик', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={stub.store} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('двухтарифный счётчик', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={stub.storeDoubleTariff} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('трёхтарифный счётчик', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={stub.storeTripleTariff} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Внешний вид формы редактирования', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={stub.filledStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
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

    describe('при переключении тарифности счётчика меняется количество полей', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={stub.store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const select = await page.$(selectors.inputs.tariff);

                await select?.focus();
                await select?.press('Enter');
                await select?.press('ArrowDown');
                await select?.press('Enter');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await select?.focus();
                await select?.press('Enter');
                await select?.press('ArrowDown');
                await select?.press('Enter');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('заполнение всех полей', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={stub.store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.counterNumber, '12321');
                await page.type(selectors.inputs.currentValue1, '350');
                await page.type(selectors.inputs.deliverFromDay, '20');
                await page.type(selectors.inputs.deliverToDay, '25');
                await page.type(selectors.inputs.installedPlace, 'В ванной');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('показ ошибок после валидации на бекенде', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'user.create_house_service': {
                        return Promise.resolve({
                            validationErrors: [
                                {
                                    parameter: 'number',
                                    localizedDescription: 'Обязательное поле',
                                },
                                {
                                    parameter: 'installedPlace',
                                    localizedDescription: 'Обязательное поле',
                                },
                                {
                                    parameter: 'deliverFromDay',
                                    localizedDescription: 'Некорректное значение для дня месяца',
                                },
                                {
                                    parameter: 'deliverToDay',
                                    localizedDescription: 'Некорректное значение для дня месяца',
                                },
                            ],
                        });
                    }
                }
            },
        };

        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={stub.store} Gate={Gate} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submitButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('показ модалки с подтверждением перед удалением счётчика', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={stub.filledStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.hover(selectors.inputs.currentPhoto1);
                await page.click(selectors.deleteButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('сохранение формы', () => {
        it('Форма в процессе сохранения', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.update_house_service': {
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
                        case 'user.update_house_service': {
                            return Promise.reject();
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
                        case 'user.update_house_service': {
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

    describe('Заполнение формы через пошаговость', () => {
        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={stub.storeMobile} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.counterNumber);

            await page.type(selectors.stepByStep.inputs.counterNumber, '123321');

            await page.click(selectors.stepByStep.right);

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.currentValue1, '340');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.deliverFromDay, '20');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.deliverToDay, '25');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.installedPlace, 'В ванной');

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
