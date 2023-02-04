import React from 'react';
import { render } from 'jest-puppeteer-react';

import noop from 'lodash/noop';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { OutstaffRoles } from 'types/outstaff';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/outstaff/reducer';
import { IUniversalStore } from 'view/modules/types';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import styles from 'view/components/OutstaffRetoucherForm/styles.module.css';
import { Fields } from 'view/modules/outstaffRetoucherForm/types';

import { OutstaffFlatQuestionnaireForms } from '../';

import * as stubs from './stubs/retoucherFormStore';

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
    photoRetouchInput: `#${Fields.PHOTO_RETOUCHED_URL}`,
    submit: `.${styles.button}`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        photoRetouchInput: `.${stepByStepModalStyles.modal} #${Fields.PHOTO_RETOUCHED_URL}`,
    },
};

const Component: React.FC<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({ store, Gate }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
            <OutstaffFlatQuestionnaireForms role={OutstaffRoles.retoucher} />
        </AppProvider>
    );
};

describe('OutstaffRetoucherForm', () => {
    describe('Cкелетон', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.skeletonStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Фотограф еще не прикрепил ссылки на исходные фото', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть ссылка на фотографии', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withPhotoRawUrlStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть ссылка на отретушированное фото', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.fullFilledStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Заполнение  формы', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withPhotoRawUrlStore} />, option);

                await page.type(selectors.photoRetouchInput, stubs.photoRetouchedUrl);

                await expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Сохранение формы в процессе', () => {
        const Gate = {
            create: () => new Promise(noop),
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withPhotoRawUrlStore} Gate={Gate} />, option);

                await page.type(selectors.photoRetouchInput, stubs.photoRetouchedUrl);

                await page.click(selectors.submit);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Не удалось сохранить форму', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_retoucher': {
                        return Promise.reject();
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withPhotoRawUrlStore} Gate={Gate} />, option);

                await page.type(selectors.photoRetouchInput, stubs.photoRetouchedUrl);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Успешное сохранение формы', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_retoucher': {
                        return Promise.resolve({
                            questionnaire: {
                                media: {
                                    photoRawUrl: stubs.photoRawUrl,
                                    photoRetouchedUrl: stubs.photoRetouchedUrl,
                                },
                            },
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withPhotoRawUrlStore} Gate={Gate} />, option);

                await page.type(selectors.photoRetouchInput, stubs.photoRetouchedUrl);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Работа с формой пошаговости', () => {
        const store: DeepPartial<IUniversalStore> = {
            ...stubs.withPhotoRawUrlStore,
            config: { isMobile: 'iOS' },
        };

        it(`Заполнение формы`, async () => {
            await render(<Component store={store} />, renderOptions.mobile);

            await page.click(selectors.photoRetouchInput);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(selectors.stepByStep.photoRetouchInput, 'link');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Показ ошибок в форме`, async () => {
            await render(<Component store={store} />, renderOptions.mobile);

            await page.click(selectors.photoRetouchInput);
            await page.type(selectors.stepByStep.photoRetouchInput, 'link');
            await page.click(selectors.stepByStep.close);
            await page.click(selectors.submit);
            await page.click(selectors.photoRetouchInput);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`Сохранение формы`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'outstaff.update_flat_questionnaire_retoucher': {
                            return Promise.resolve({
                                questionnaire: {
                                    media: {
                                        photoRawUrl: stubs.photoRawUrl,
                                        photoRetouchedUrl: stubs.photoRetouchedUrl,
                                    },
                                },
                            });
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />, renderOptions.mobile);

            await page.click(selectors.photoRetouchInput);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.type(selectors.stepByStep.photoRetouchInput, stubs.photoRetouchedUrl);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
