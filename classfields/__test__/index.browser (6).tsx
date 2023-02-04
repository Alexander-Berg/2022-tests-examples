import React from 'react';

import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/manager/reducer';

import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import { Fields } from 'view/modules/managerFlatPublishingForm/types';
import ModalDisplay from 'view/components/ModalDisplay';
import managerFlatPublishingFormStyles from 'view/components/ManagerFlat/ManagerFlatPublishingForm/styles.module.css';

import { ManagerFlatPublishingContainer } from '../container';

import * as stubs from './stub/store';

const renderOptions = [{ viewport: { width: 630, height: 1000 } }, { viewport: { width: 375, height: 1000 } }];

const selectors = {
    sendSubmit: `.${managerFlatPublishingFormStyles.button}.Button`,
    inputs: Object.values(Fields).reduce((acc, field) => {
        acc[field] = `#${field}`;

        return acc;
    }, {} as Record<Fields, string>),
    stepByStep: {
        left: `.Portal:last-of-type .${stepByStepModalStyles.leftButton}`,
        right: `.Portal:last-of-type .${stepByStepModalStyles.rightButton}`,
        close: `.Portal:last-of-type .${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: Object.values(Fields).reduce((acc, field) => {
            acc[field] = `.Portal:last-of-type .${stepByStepModalStyles.modal} #${field}`;

            return acc;
        }, {} as Record<Fields, string>),
    },
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <ManagerFlatPublishingContainer />
        <ModalDisplay />
    </AppProvider>
);

describe('ManagerFlatPublishing', () => {
    describe('Скелетон', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.skeletonStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Не заполнена анкета', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={stubs.store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Некорректный статус квартиры', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={stubs.invalidStatusStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Все данные и корректный статус - не опубликовано', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={stubs.notPublishedStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Все данные и корректный статус - опубликовано', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={stubs.publishedStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Ожидает подтверждения', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={stubs.waitingForApprovingStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение формы', () => {
        it('Заполнение всех полей', async () => {
            const Gate = {
                create: () => Promise.resolve(),
            };

            await render(<Component store={stubs.formNotFilledStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.OFFER_3D_TOUR_URL, 'https://st.yandex-team.ru/REALTYBACK-5792');
            await page.type(selectors.inputs.OFFER_COPYRIGHT, stubs.offerCopyright);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Заполнение всех полей через пошаговость', async () => {
            await render(<Component store={stubs.formNotFilledMobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.OFFER_3D_TOUR_URL);
            await page.type(selectors.stepByStep.inputs.OFFER_3D_TOUR_URL, 'https://st.yandex-team.ru/REALTYBACK-5792');
            await page.click(selectors.stepByStep.right);
            await page.type(selectors.stepByStep.inputs.OFFER_COPYRIGHT, stubs.offerCopyright);
            await page.click(selectors.stepByStep.right);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Показ ошибок', async () => {
            const Gate = {
                create: () => Promise.resolve(),
            };

            await render(<Component store={stubs.formNotFilledStore} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.OFFER_3D_TOUR_URL, '1');
            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Показ ошибок в форме пошаговости', async () => {
            await render(<Component store={stubs.formNotFilledMobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.OFFER_3D_TOUR_URL);
            await page.type(selectors.stepByStep.inputs.OFFER_3D_TOUR_URL, '1');
            await page.click(selectors.stepByStep.right);
            await page.click(selectors.stepByStep.right);
            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.OFFER_3D_TOUR_URL);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.stepByStep.inputs.OFFER_3D_TOUR_URL);
            await page.keyboard.press('Backspace');
            await page.type(selectors.stepByStep.inputs.OFFER_3D_TOUR_URL, 'https://st.yandex-team.ru/REALTYBACK-5792');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.stepByStep.right);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.stepByStep.inputs.OFFER_COPYRIGHT, stubs.offerCopyright);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.stepByStep.right);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Сохранение формы в процессе', () => {
        const Gate = {
            create: () => new Promise(noop),
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.formNotFilledStore} Gate={Gate} />, option);

                await page.type(selectors.inputs.OFFER_3D_TOUR_URL, 'https://st.yandex-team.ru/REALTYBACK-5792');
                await page.type(selectors.inputs.OFFER_COPYRIGHT, stubs.offerCopyright);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Не удалось сохранить форму', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_publishing_form': {
                        return Promise.reject();
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.formNotFilledStore} Gate={Gate} />, option);

                await page.type(selectors.inputs.OFFER_3D_TOUR_URL, 'https://st.yandex-team.ru/REALTYBACK-5792');
                await page.type(selectors.inputs.OFFER_COPYRIGHT, stubs.offerCopyright);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Успешное сохранение формы', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'manager.update_flat_publishing_form': {
                        return Promise.resolve({
                            questionnaire: {
                                ...stubs.flatQuestionnaire,
                                offerCopyright: stubs.offerCopyright,
                            },
                            flat: stubs.store.managerFlat,
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.formNotFilledStore} Gate={Gate} />, option);

                await page.type(selectors.inputs.OFFER_3D_TOUR_URL, 'https://st.yandex-team.ru/REALTYBACK-5792');
                await page.type(selectors.inputs.OFFER_COPYRIGHT, stubs.offerCopyright);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
