import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { TenantQuestionnaireModerationStatus, UserPersonalActivity } from 'types/user';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import tenantQuestionnaireStyles from 'view/components/Questionnaire/QuestionnaireForm/styles.module.css';
import formCounterFieldStyles from 'view/components/Form/FormCounterField/styles.module.css';

import { TenantQuestionnaireFormContainer } from '../container';

import { mobileStore, skeletonStore, store, storeWrongAdditionalTenant } from './stub/store';

const renderOptions = [{ viewport: { width: 625, height: 1000 } }, { viewport: { width: 415, height: 1000 } }];

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider
        rootReducer={userReducer}
        Gate={props.Gate}
        initialState={props.store}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <TenantQuestionnaireFormContainer />
    </AppProvider>
);

const selectors = {
    sendSubmit: `.${tenantQuestionnaireStyles.submit}.Button`,
    inputs: {
        personalActivityType: '#PERSONAL_ACTIVITY_TYPE',
        educationalInstitution: '#EDUCATIONAL_INSTITUTION',
        reasonForRelocation: '#REASON_FOR_RELOCATION',
        additionalTenant: '#ADDITIONAL_TENANT',
        petsInfo: '#PETS_INFO',
        selfDescription: '#TELL_ABOUT_YOURSELF',
    },
    checkboxes: {
        hasChildren: '#HAS_CHILDREN',
        hasPets: '#HAS_PETS',
    },
    counters: {
        teenagersPlus: `#NUMBER_OF_TEENAGERS .${formCounterFieldStyles.plus}`,
        schoolchildrenPlus: `#NUMBER_OF_SCHOOLCHILDREN .${formCounterFieldStyles.plus}`,
        preschoolersPlus: `#NUMBER_OF_PRESCHOOLERS .${formCounterFieldStyles.plus}`,
        babiesPlus: `#NUMBER_OF_BABIES .${formCounterFieldStyles.plus}`,
    },
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: {
            personalActivityType: `.${stepByStepModalStyles.modal} #PERSONAL_ACTIVITY_TYPE`,
            educationalInstitution: `.${stepByStepModalStyles.modal} #EDUCATIONAL_INSTITUTION`,
            reasonForRelocation: `.${stepByStepModalStyles.modal} #REASON_FOR_RELOCATION`,
            additionalTenant: `.${stepByStepModalStyles.modal} #ADDITIONAL_TENANT`,
            petsInfo: `.${stepByStepModalStyles.modal} #PETS_INFO`,
            selfDescription: `.${stepByStepModalStyles.modal} #TELL_ABOUT_YOURSELF`,
        },
        checkboxes: {
            hasChildren: `.${stepByStepModalStyles.modal} #HAS_CHILDREN`,
            hasPets: `.${stepByStepModalStyles.modal} #HAS_PETS`,
        },
        counters: {
            teenagersPlus: `.${stepByStepModalStyles.modal} #NUMBER_OF_TEENAGERS .${formCounterFieldStyles.plus}`,
            // eslint-disable-next-line max-len
            schoolchildrenPlus: `.${stepByStepModalStyles.modal} #NUMBER_OF_SCHOOLCHILDREN .${formCounterFieldStyles.plus}`,
            preschoolersPlus: `.${stepByStepModalStyles.modal} #NUMBER_OF_PRESCHOOLERS .${formCounterFieldStyles.plus}`,
            babiesPlus: `.${stepByStepModalStyles.modal} #NUMBER_OF_BABIES .${formCounterFieldStyles.plus}`,
        },
    },
};

describe('TenantQuestionnaireForm', () => {
    describe('Внешний вид', () => {
        describe(`Базовое состояние`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Скелетон`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={skeletonStore} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Показ ошибок', () => {
        describe(`Не заполнены поля "место работы", "с кем планируете проживать" и "Информация о себе"`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Не заполнен возраст детей при отметке есть дети`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    const select = await page.$(selectors.inputs.additionalTenant);

                    await select?.focus();
                    await select?.press('Enter');
                    await select?.press('ArrowDown');
                    await select?.press('Enter');

                    const hasChildren = await page.$(selectors.checkboxes.hasChildren);
                    await hasChildren?.focus();
                    await hasChildren?.press('Space');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Ошибки пропадают при вводе в поле', () => {
            it(`${renderOptions[0].viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const select = await page.$(selectors.inputs.additionalTenant);

                await select?.focus();
                await select?.press('Enter');
                await select?.press('ArrowDown');
                await select?.press('Enter');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        describe('Ошибка модерации', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    const storeWithModerationError = {
                        ...store,
                        legacyUser: {
                            tenantQuestionnaireModerationStatus: TenantQuestionnaireModerationStatus.INVALID,
                        },
                    };
                    await render(<Component store={storeWithModerationError} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Заполнение формы', () => {
        describe(`${renderOptions[1].viewport.width}px`, () => {
            it('Заполнение всех полей и отправка формы', async () => {
                await render(<Component store={store} />, renderOptions[1]);
                const personalActivityTypeSelect = await page.$(selectors.inputs.personalActivityType);
                const additionalTenantSelect = await page.$(selectors.inputs.additionalTenant);
                const hasChildren = await page.$(selectors.checkboxes.hasChildren);
                const hasPets = await page.$(selectors.checkboxes.hasPets);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await personalActivityTypeSelect?.focus();
                await personalActivityTypeSelect?.press('Enter');
                await personalActivityTypeSelect?.press('ArrowDown');
                await personalActivityTypeSelect?.press('Enter');

                await page.type(selectors.inputs.educationalInstitution, 'СПБГЭТУ "ЛЭТИ"');

                await page.type(selectors.inputs.reasonForRelocation, 'Сгорел дом');
                await additionalTenantSelect?.focus();
                await additionalTenantSelect?.press('Enter');
                await additionalTenantSelect?.press('ArrowDown');
                await additionalTenantSelect?.press('Enter');
                await hasChildren?.focus();
                await hasChildren?.press('Space');
                await page.click(selectors.counters.teenagersPlus);
                await page.click(selectors.counters.schoolchildrenPlus);
                await page.click(selectors.counters.preschoolersPlus);
                await page.click(selectors.counters.babiesPlus);
                await page.click(selectors.counters.babiesPlus);
                await hasPets?.focus();
                await hasPets?.press('Space');
                await page.type(selectors.inputs.petsInfo, 'Беспредельщики');
                await page.type(selectors.inputs.selfDescription, 'Беспредельщик');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('Форма не сохранится, если additionalTenant не соответствует фронтовому селекту', async () => {
                await render(<Component store={storeWrongAdditionalTenant} />, renderOptions[1]);

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            describe('Работа с пошаговой формой', () => {
                it('Заполнение всех полей', async () => {
                    await render(<Component store={mobileStore} />, renderOptions[1]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.inputs.personalActivityType);

                    await page.select(selectors.stepByStep.inputs.personalActivityType, 'STUDY');

                    await page.click(selectors.stepByStep.right);
                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.educationalInstitution, 'СПБГЭТУ "ЛЭТИ"');

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.reasonForRelocation, 'Сгорел дом');

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.selfDescription, 'Беспредельщик');

                    await page.click(selectors.stepByStep.right);

                    const select = await page.$(selectors.stepByStep.inputs.additionalTenant);
                    await select?.focus();
                    await select?.press('Enter');
                    await select?.press('ArrowDown');
                    await select?.press('Enter');

                    await page.click(selectors.stepByStep.right);

                    const hasChildren = await page.$(selectors.stepByStep.checkboxes.hasChildren);
                    await hasChildren?.focus();
                    await hasChildren?.press('Space');

                    await page.click(selectors.stepByStep.right);

                    await page.click(selectors.stepByStep.counters.teenagersPlus);
                    await page.click(selectors.stepByStep.counters.schoolchildrenPlus);
                    await page.click(selectors.stepByStep.counters.preschoolersPlus);
                    await page.click(selectors.stepByStep.counters.babiesPlus);
                    await page.click(selectors.stepByStep.counters.babiesPlus);

                    await page.click(selectors.stepByStep.right);

                    const hasPets = await page.$(selectors.stepByStep.checkboxes.hasPets);
                    await hasPets?.focus();
                    await hasPets?.press('Space');

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.petsInfo, 'Беспредельщики');

                    await page.click(selectors.stepByStep.close);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it('Анкета в процессе сохранения', async () => {
                    const Gate = {
                        create: () => new Promise(noop),
                    };

                    await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

                    const personalActivityTypeSelect = await page.$(selectors.inputs.personalActivityType);
                    await personalActivityTypeSelect?.focus();
                    await personalActivityTypeSelect?.press('Enter');
                    await personalActivityTypeSelect?.press('ArrowDown');
                    await personalActivityTypeSelect?.press('Enter');

                    await page.type(selectors.inputs.educationalInstitution, 'СПБГЭТУ "ЛЭТИ"');

                    const select = await page.$(selectors.inputs.additionalTenant);
                    await select?.focus();
                    await select?.press('Enter');
                    await select?.press('ArrowDown');
                    await select?.press('Enter');
                    await page.type(selectors.inputs.selfDescription, 'Прекрасный человек');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it('Не удалось сохранить анкету', async () => {
                    const Gate = {
                        create: () => Promise.reject(),
                    };

                    await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

                    const personalActivityTypeSelect = await page.$(selectors.inputs.personalActivityType);
                    await personalActivityTypeSelect?.focus();
                    await personalActivityTypeSelect?.press('Enter');
                    await personalActivityTypeSelect?.press('ArrowDown');
                    await personalActivityTypeSelect?.press('Enter');

                    await page.type(selectors.inputs.educationalInstitution, 'СПБГЭТУ "ЛЭТИ"');

                    const select = await page.$(selectors.inputs.additionalTenant);
                    await select?.focus();
                    await select?.press('Enter');
                    await select?.press('ArrowDown');
                    await select?.press('Enter');
                    await page.type(selectors.inputs.selfDescription, 'Прекрасный человек');
                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it('Анкета успешно сохранена', async () => {
                    const Gate = {
                        create: () => {
                            return Promise.resolve({
                                user: {
                                    personalActivity: {
                                        activity: UserPersonalActivity.STUDY,
                                        educationalInstitution: 'СПБГЭТУ "ЛЭТИ"',
                                        aboutWorkAndPosition: '',
                                        aboutBusiness: '',
                                    },
                                    reasonForRelocation: '',
                                    additionalTenant: 'Один',
                                    hasChildren: false,
                                    hasPets: false,
                                    petsInfo: '',
                                    selfDescription: 'Прекрасный человек',
                                },
                            });
                        },
                    };

                    await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

                    const personalActivityTypeSelect = await page.$(selectors.inputs.personalActivityType);
                    await personalActivityTypeSelect?.focus();
                    await personalActivityTypeSelect?.press('Enter');
                    await personalActivityTypeSelect?.press('ArrowDown');
                    await personalActivityTypeSelect?.press('Enter');

                    await page.type(selectors.inputs.educationalInstitution, 'СПБГЭТУ "ЛЭТИ"');

                    const select = await page.$(selectors.inputs.additionalTenant);
                    await select?.focus();
                    await select?.press('Enter');
                    await select?.press('ArrowDown');
                    await select?.press('Enter');
                    await page.type(selectors.inputs.selfDescription, 'Прекрасный человек');
                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });
});
