import React from 'react';
import { render } from 'jest-puppeteer-react';

import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { OutstaffRoles } from 'types/outstaff';

import { Fields } from 'view/modules/outstaffPhotographerForm/types';
import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/outstaff/reducer';
import { IUniversalStore } from 'view/modules/types';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import styles from 'view/components/OutstaffPhotographerForm/styles.module.css';

import { OutstaffFlatQuestionnaireForms } from '../';

import * as stubs from './stubs/photographerFormStore';

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
    offerPhotoRawInput: `#${Fields.OFFER_PHOTO_RAW_URL}`,
    offer3dTourInput: `#${Fields.OFFER_3D_TOUR_URL}`,
    submit: `.${styles.button}`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        offerPhotoRawInput: `.${stepByStepModalStyles.modal} #${Fields.OFFER_PHOTO_RAW_URL}`,
        offer3dTourInput: `.${stepByStepModalStyles.modal} #${Fields.OFFER_3D_TOUR_URL}`,
    },
};

const Component: React.FC<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({ store, Gate }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
            <OutstaffFlatQuestionnaireForms role={OutstaffRoles.photographer} />
        </AppProvider>
    );
};

describe('OutstaffPhotographerForm', () => {
    describe('Базовое состояние', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Базовое состояние(нет информации о квартире)', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.noFlatEntranceInfoStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Cкелетон', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.skeletonStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполненное состояние', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.fullFilledStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение  всех полей', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} />, option);

                await page.type(selectors.offerPhotoRawInput, stubs.photoRawUrl);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.type(selectors.offer3dTourInput, stubs.tour3dUrl);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Валидация в браузере', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} />, option);

                await page.type(selectors.offerPhotoRawInput, 'myphoto');
                await page.type(selectors.offer3dTourInput, '3durtur');

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Сохранение формы в процессе', () => {
        const Gate = {
            create: () => new Promise(noop),
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} Gate={Gate} />, option);

                await page.type(selectors.offerPhotoRawInput, stubs.photoRawUrl);
                await page.type(selectors.offer3dTourInput, stubs.tour3dUrl);

                await page.click(selectors.submit);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Не удалось сохранить форму', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_photographer': {
                        return Promise.reject();
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} Gate={Gate} />, option);

                await page.type(selectors.offerPhotoRawInput, stubs.photoRawUrl);
                await page.type(selectors.offer3dTourInput, stubs.tour3dUrl);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Успешное сохранение формы', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_photographer': {
                        return Promise.resolve({
                            questionnaire: {
                                media: {
                                    photoRawUrl: stubs.photoRawUrl,
                                    tour3dUrl: stubs.tour3dUrl,
                                },
                            },
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} Gate={Gate} />, option);

                await page.type(selectors.offerPhotoRawInput, stubs.photoRawUrl);
                await page.type(selectors.offer3dTourInput, stubs.tour3dUrl);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Работа с формой пошаговости', () => {
        const store: DeepPartial<IUniversalStore> = {
            ...stubs.baseStore,
            config: { isMobile: 'iOS' },
        };

        it(`Заполнение всех полей формы`, async () => {
            await render(<Component store={store} />, renderOptions.mobile);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.offerPhotoRawInput);

            await page.type(selectors.stepByStep.offerPhotoRawInput, stubs.photoRawUrl);

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.offer3dTourInput, stubs.tour3dUrl);

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Показ ошибок в форме`, async () => {
            await render(<Component store={store} />, renderOptions.mobile);

            await page.click(selectors.offerPhotoRawInput);
            await page.type(selectors.stepByStep.offerPhotoRawInput, 'link');
            await page.click(selectors.stepByStep.right);
            await page.type(selectors.stepByStep.offer3dTourInput, 'link');
            await page.click(selectors.stepByStep.close);
            await page.click(selectors.submit);

            await page.click(selectors.offerPhotoRawInput);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.stepByStep.right);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Сохранение формы`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'outstaff.update_flat_questionnaire_photographer': {
                            return Promise.resolve({
                                questionnaire: {
                                    media: {
                                        photoRawUrl: stubs.photoRawUrl,
                                        tour3dUrl: stubs.tour3dUrl,
                                    },
                                },
                            });
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />, renderOptions.mobile);

            await page.click(selectors.offerPhotoRawInput);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(selectors.stepByStep.offerPhotoRawInput, stubs.photoRawUrl);

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.offer3dTourInput, stubs.tour3dUrl);

            await page.click(selectors.stepByStep.close);

            await page.click(selectors.submit);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
