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

import { OwnerHouseServicesMeterReadingsDeclineContainer } from '../container';

import styles from '../styles.module.css';

import { store, skeletonStore, mobileStore } from './stub';

const renderOptions = [{ viewport: { width: 1000, height: 900 } }, { viewport: { width: 375, height: 900 } }];

const selectors = {
    inputs: {
        reasonForDecline: '#REASON_FOR_DECLINE',
    },
    submitButton: `.${styles.button}`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: {
            reasonForDecline: `.${stepByStepModalStyles.modal} #REASON_FOR_DECLINE`,
        },
    },
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider
        rootReducer={rootReducer}
        Gate={props.Gate}
        initialState={props.store}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <OwnerHouseServicesMeterReadingsDeclineContainer />
        <ModalDisplay />
    </AppProvider>
);

describe('OwnerHouseServicesMeterReadingsDecline', () => {
    describe('Внешний вид формы отклонения показаний', () => {
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

                await page.type(
                    selectors.inputs.reasonForDecline,
                    'Почему показания за последний месяц сильно отличаются от средних? Я чую дерзкий обман'
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение формы через попап пошаговости', () => {
        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={mobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.reasonForDecline);
            await page.type(
                selectors.stepByStep.inputs.reasonForDecline,
                'Почему показания за последний месяц сильно отличаются от средних? Я чую дерзкий обман'
            );

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Сохранение формы', () => {
        it('Форма в процессе сохранения', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.decline_house_services_period_meter_readings': {
                            return new Promise(noop);
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

            await page.type(
                selectors.inputs.reasonForDecline,
                'Почему показания за последний месяц сильно отличаются от средних? Я чую дерзкий обман'
            );
            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Не удалось сохранить форму', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.decline_house_services_period_meter_readings': {
                            return Promise.reject({ error: {} });
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

            await page.type(
                selectors.inputs.reasonForDecline,
                'Почему показания за последний месяц сильно отличаются от средних? Я чую дерзкий обман'
            );
            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Форма успешно сохранена', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'user.decline_house_services_period_meter_readings': {
                            return Promise.resolve();
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

            await page.type(
                selectors.inputs.reasonForDecline,
                'Почему показания за последний месяц сильно отличаются от средних? Я чую дерзкий обман'
            );
            await page.click(selectors.submitButton);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
