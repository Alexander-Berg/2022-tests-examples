import React from 'react';
import { render } from 'jest-puppeteer-react';

import noop from 'lodash/noop';
import cloneDeep from 'lodash/cloneDeep';
import set from 'lodash/set';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { FlatStatus } from 'types/flat';

import { OutstaffRoles } from 'types/outstaff';

import { Fields } from 'view/modules/outstaffCopywriterForm/types';
import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/outstaff/reducer';
import { IUniversalStore } from 'view/modules/types';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import styles from 'view/components/OutstaffCopywriterForm/styles.module.css';
import textAreaStyles from 'view/components/TextArea/styles.module.css';
import modalStyles from 'view/components/Modal/ConfirmActionModal/styles.module.css';
import ModalDisplay from 'view/components/ModalDisplay';

import { OutstaffFlatQuestionnaireForms } from '../';

import * as stubs from './stubs/copywriterFormStore';

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
    textArea: `#${Fields.OFFER_COPYRIGHT}`,
    submit: `.${styles.submitWrapper} :nth-child(1)`,
    publishingButton: `.${styles.submitWrapper} :nth-child(2)`,
    confirmButton: `.${modalStyles.buttons} :nth-child(1)`,
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        textArea: `.${stepByStepModalStyles.modal} #${Fields.OFFER_COPYRIGHT}`,
    },
    addArendaDecription: `.${textAreaStyles.descriptionButton}`,
};

const Component: React.FC<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({ store, Gate }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
            <OutstaffFlatQuestionnaireForms role={OutstaffRoles.copywriter} />
            <ModalDisplay />
        </AppProvider>
    );
};

describe('OutstaffCopywriterForm', () => {
    describe('Базовое состояние', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.baseStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Скелетон', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.skeletonStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Анкета заполнена', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatQuesstionnaireStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Форма ввода текста остается в нижней части экрана при скролле страницы', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatQuesstionnaireStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.evaluate(() => {
                    window.scrollTo(0, document.body.scrollHeight);
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Набор текста в форме', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatQuesstionnaireStore} />, option);

                await page.type(selectors.textArea, 'Хорошая квартира, все супер, снимайте не пожалеете!');

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть сохраненное описание квартиры', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.fullFilledStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Попытка отправить пустую форму', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.getClearFormStore()} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Сохранение формы в процессе', () => {
        const Gate = {
            create: () => new Promise(noop),
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatQuesstionnaireStore} Gate={Gate} />, option);

                await page.type(selectors.textArea, 'Хорошая квартира, все супер, снимайте не пожалеете!');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Не удалось сохранить форму', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_copywriter': {
                        return Promise.reject();
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatQuesstionnaireStore} Gate={Gate} />, option);

                await page.type(selectors.textArea, 'Хорошая квартира, все супер, снимайте не пожалеете!');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Успешное сохранение формы', () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_copywriter': {
                        return Promise.resolve({
                            questionnaire: {
                                ...stubs.flatQuestionnaire,
                                offerCopyright: stubs.offerCopyright,
                            },
                            outstaffFlat: stubs.baseStore.outstaffFlat,
                        });
                    }
                }
            },
        };

        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatQuesstionnaireStore} Gate={Gate} />, option);

                await page.type(selectors.textArea, stubs.offerCopyright);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Показ кнопки подтверждения публикации квартиры', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatApprovingPublishingStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Открытие модалки для подтверждения публикации', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatApprovingPublishingStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.publishingButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Сохранение и подтверждение публикации квартиры', () => {
        const store = {
            ...stubs.withFlatQuesstionnaireStore,
            outstaffFlat: stubs.getOutstaffFlat(FlatStatus.WORK_IN_PROGRESS),
            imageUploader: stubs.imageUploader,
        };
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_copywriter': {
                        return Promise.resolve({
                            questionnaire: {
                                ...stubs.flatQuestionnaire,
                                offerCopyright: stubs.offerCopyright,
                            },
                            outstaffFlat: stubs.getOutstaffFlat(FlatStatus.WORK_IN_PROGRESS),
                        });
                    }
                    case 'outstaff.approving_publishing_flat': {
                        return Promise.resolve({
                            outstaffFlat: stubs.getOutstaffFlat(FlatStatus.LOOKING_FOR_TENANT),
                        });
                    }
                }
            },
        };
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                await render(<Component store={store} Gate={Gate} />, option);

                await page.type(selectors.textArea, stubs.offerCopyright);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.confirmButton);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Сохранение квартиры без заполненных обязательных полей`, () => {
        const store = {
            ...stubs.baseStore,
            managerFlatQuestionnaire: {
                questionnaire: {},
            },
            outstaffFlat: stubs.getOutstaffFlat(FlatStatus.WORK_IN_PROGRESS),
            imageUploader: stubs.imageUploader,
        };
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_copywriter': {
                        return Promise.resolve({
                            questionnaire: {
                                offerCopyright: stubs.offerCopyright,
                            },
                            outstaffFlat: stubs.getOutstaffFlat(FlatStatus.WORK_IN_PROGRESS),
                        });
                    }
                }
            },
        };
        [FlatStatus.WORK_IN_PROGRESS, FlatStatus.LOOKING_FOR_TENANT].forEach((status) => {
            Object.values(renderOptions).forEach((option) => {
                it(`статус квартиры: ${status} width:${option.viewport.width}px`, async () => {
                    await render(<Component store={store} Gate={Gate} />, option);

                    await page.type(selectors.textArea, stubs.offerCopyright);
                    await page.click(selectors.submit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Работа с формой пошаговости', () => {
        const store: DeepPartial<IUniversalStore> = {
            ...stubs.withFlatQuesstionnaireStore,
            config: { isMobile: 'iOS' },
        };

        it(`Заполнение формы`, async () => {
            await render(<Component store={store} />, renderOptions.mobile);

            await page.click(selectors.textArea);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.stepByStep.textArea, 'Хорошая квартира в центре Питера');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.stepByStep.close);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Показ ошибок в форме`, async () => {
            await render(<Component store={stubs.getClearFormStore(true)} />, renderOptions.mobile);

            await page.click(selectors.submit);
            await page.click(selectors.textArea);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it(`Успешное сохранение формы`, async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'outstaff.update_flat_questionnaire_copywriter': {
                            return Promise.resolve({
                                questionnaire: {
                                    ...stubs.flatQuestionnaire,
                                    offerCopyright: stubs.offerCopyright,
                                },
                                outstaffFlat: stubs.baseStore.outstaffFlat,
                            });
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />, renderOptions.mobile);

            await page.click(selectors.textArea);

            await page.type(selectors.stepByStep.textArea, stubs.offerCopyright);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.stepByStep.close);

            await page.click(selectors.submit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('Кнопка добавить описание', () => {
        Object.values(renderOptions).forEach((option) => {
            it(`Добавление текста в пустое окно - width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.withFlatQuesstionnaireStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.addArendaDecription);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        Object.values(renderOptions).forEach((option) => {
            it(`Добавление текста в заполненное окно - width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.fullFilledStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.addArendaDecription);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        Object.values(renderOptions).forEach((option) => {
            it(`Описание не добавляется дважды - width:${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.fullFilledStore} />, option);

                await page.click(selectors.addArendaDecription);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.addArendaDecription);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Введено более 2000 символов`, () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'outstaff.update_flat_questionnaire_copywriter': {
                        return Promise.resolve({
                            questionnaire: {
                                offerCopyright: stubs.offerCopyright,
                            },
                            outstaffFlat: stubs.getOutstaffFlat(FlatStatus.WORK_IN_PROGRESS),
                        });
                    }
                }
            },
        };
        Object.values(renderOptions).forEach((option) => {
            it(`width:${option.viewport.width}px`, async () => {
                const store = cloneDeep(stubs.withFlatQuesstionnaireStore);

                set(
                    store,
                    ['outstaffCopywriterForm', 'fields', Fields.OFFER_COPYRIGHT, 'value'],
                    stubs.manySymbolsOfferCopyright
                );

                await render(<Component store={store} Gate={Gate} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
