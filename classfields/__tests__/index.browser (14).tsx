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
import { UserFlatContentWrapper } from 'view/components/UserFlatContentWrapper';

import { HouseServicesPeriodBillFormContainer } from '../container';

import styles from '../styles.module.css';

import { store, skeletonStore, mobileStore, storeWithImages } from './stub';

const renderOptions = [{ viewport: { width: 900, height: 300 } }, { viewport: { width: 375, height: 300 } }];

const selectors = {
    inputs: {
        amount: '#AMOUNT',
        comment: '#COMMENT',
    },
    submitButton: `.${styles.button}`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: {
            amount: `.${stepByStepModalStyles.modal} #AMOUNT`,
            comment: `.${stepByStepModalStyles.modal} #COMMENT`,
        },
    },
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={rootReducer} Gate={props.Gate} initialState={props.store}>
        <UserFlatContentWrapper>
            <HouseServicesPeriodBillFormContainer />
        </UserFlatContentWrapper>
        <ModalDisplay />
    </AppProvider>
);

describe('HouseServicesPeriodBillForm', () => {
    describe('Внешний вид формы выставления счёта', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Показ скелетона', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение формы', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                await page.type(selectors.inputs.amount, '7000');
                await page.type(selectors.inputs.comment, 'Заплатишь мне за всё');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение формы через попап пошаговости', () => {
        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={mobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.amount);
            await page.type(selectors.stepByStep.inputs.amount, '7000');
            await page.click(selectors.stepByStep.right);
            await page.type(selectors.stepByStep.inputs.comment, 'Заплатишь мне за всё');

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Сохранение формы', () => {
        it('Форма в процессе сохранения', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.create_house_services_period_bill': {
                            return new Promise(noop);
                        }
                    }
                },
            };

            await render(<Component store={storeWithImages} Gate={Gate} />, renderOptions[0]);

            await page.type(selectors.inputs.amount, '7000');
            await page.type(selectors.inputs.comment, 'Заплатишь мне за всё');
            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Не удалось сохранить форму', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.create_house_services_period_bill': {
                            return Promise.reject();
                        }
                    }
                },
            };

            await render(<Component store={storeWithImages} Gate={Gate} />, renderOptions[0]);

            await page.type(selectors.inputs.amount, '7000');
            await page.type(selectors.inputs.comment, 'Заплатишь мне за всё');
            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Форма успешно сохранена', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.create_house_services_period_bill': {
                            return Promise.resolve();
                        }
                    }
                },
            };

            await render(<Component store={storeWithImages} Gate={Gate} />, renderOptions[0]);

            await page.type(selectors.inputs.amount, '7000');
            await page.type(selectors.inputs.comment, 'Заплатишь мне за всё');
            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
