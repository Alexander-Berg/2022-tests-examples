import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { ILegacyRentUser } from 'types/user';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { UserPersonalDataPhoneFieldType } from 'view/modules/userPersonalDataPhone/types';
import { userReducer } from 'view/entries/user/reducer';

import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import userPersonalDataPhoneFormStyles from 'view/components/Modal/UserPersonalDataPhoneModal/styles.module.css';
import { Fields } from 'view/modules/personalDataForm/types';

import personalDataFormStyles from '../styles.module.css';
import { PersonalDataFormContainer } from '../container';

import { skeletonStore, store, mobileStore, filledStore } from './stub/store';

const renderOptions = [{ viewport: { width: 630, height: 1000 } }, { viewport: { width: 375, height: 1000 } }];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
    Gate?: AnyObject;
    isManagerForm?: boolean;
}> = (props) => (
    <AppProvider rootReducer={userReducer} Gate={props.Gate} initialState={props.store}>
        <PersonalDataFormContainer
            user={(props.store.legacyUser as unknown) as ILegacyRentUser}
            isManagerForm={props.isManagerForm}
        />
    </AppProvider>
);

const selectors = {
    sendSubmit: `.${personalDataFormStyles.button}.Button`,
    phone: {
        editBtn: `.${personalDataFormStyles.phoneEditButton}`,
        submitBtn: `.${userPersonalDataPhoneFormStyles.button}`,
        phoneInput: '#PHONE',
        confirmInput: '#CONFIRMATION_CODE',
    },
    inputs: Object.values(Fields).reduce((acc, field) => {
        acc[field] = `#${field}`;

        return acc;
    }, {} as Record<Fields, string>),
    stepByStep: {
        left: `.Portal:last-of-type  .${stepByStepModalStyles.leftButton}`,
        right: `.Portal:last-of-type .${stepByStepModalStyles.rightButton}`,
        close: `.Portal:last-of-type  .${stepByStepModalStyles.modal} .IconSvg_close-24`,
        inputs: Object.values(Fields).reduce((acc, field) => {
            acc[field] = `.Portal:last-of-type .${stepByStepModalStyles.modal} #${field}`;

            return acc;
        }, {} as Record<Fields, string>),
    },
};

describe('PersonalDataForm', () => {
    describe(`?????????????? ??????????????????`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`?????????????????????? ??????????????????`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`????????????????`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('?????????? ????????????', () => {
        describe(`???????????? ?????? ???????????? ??????????`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('???????????????????????? ????????????', () => {
            it(`${renderOptions[0].viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.PASSPORT_SERIES_AND_NUMBER, '34');
                await page.type(selectors.inputs.PASSPORT_ISSUE_DATE, '10');
                await page.type(selectors.inputs.BIRTHDAY, '20');
                await page.type(selectors.inputs.DEPARTMENT_CODE, '234');
                await page.type(selectors.inputs.EMAIL, 'fff');

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('???????????????????? ??????????', () => {
        it('???????????????????? ???????? ??????????', async () => {
            const Gate = {
                create: () => Promise.resolve(),
            };
            const customStore: DeepPartial<IUniversalStore> = {
                ...store,
                userPersonalDataPhone: {
                    fields: {
                        [UserPersonalDataPhoneFieldType.PHONE]: {
                            id: UserPersonalDataPhoneFieldType.PHONE,
                            value: '+79876543210',
                        },
                    },
                },
                legacyUser: {
                    phone: '+79876543210',
                },
            };

            await render(<Component store={customStore} Gate={Gate} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.SURNAME, '????????????');
            await page.type(selectors.inputs.NAME, '????????');
            await page.type(selectors.inputs.PATRONYMIC, '????????????????');
            await page.type(selectors.inputs.EMAIL, '123@list.ru');

            await page.type(selectors.inputs.PASSPORT_SERIES_AND_NUMBER, '1111222222');
            await page.type(selectors.inputs.PASSPORT_ISSUE_BY, '??????');
            await page.type(selectors.inputs.PASSPORT_ISSUE_DATE, '11.12.2010');
            await page.type(selectors.inputs.DEPARTMENT_CODE, '380567');
            await page.type(selectors.inputs.BIRTHDAY, '20.11.1996');
            await page.type(selectors.inputs.BIRTH_PLACE, '?????????????????? ??????????????, ?????????? ??????????????');
            await page.type(selectors.inputs.REGISTRATION, '??????????-??????????????????, ????-???? ????????????, ?? 58, ???? 332');

            const dataProcessingAgreement = await page.$(selectors.inputs.DATA_PROCESSING_AGREEMENT);
            await dataProcessingAgreement?.focus();
            await dataProcessingAgreement?.press('Space');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('???????????????????? ???????? ?????????? ?????????? ?????????????????? ??????????', async () => {
            await render(<Component store={mobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.SURNAME);

            await page.type(selectors.stepByStep.inputs.SURNAME, '????????????');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.NAME, '????????');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.PATRONYMIC, '????????????????');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.PASSPORT_SERIES_AND_NUMBER, '1111222222');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.PASSPORT_ISSUE_BY, '??????');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.PASSPORT_ISSUE_DATE, '11.12.2010');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.DEPARTMENT_CODE, '380567');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.BIRTHDAY, '20.11.1996');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.BIRTH_PLACE, '??????????-??????????????????');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.REGISTRATION, '??????????-??????????????????, ????-???? ????????????, ?? 58, ???? 332');

            await page.click(selectors.stepByStep.right);

            await page.click(selectors.inputs.EMAIL);

            await page.type(selectors.stepByStep.inputs.EMAIL, '123@list.ru');

            await page.click(selectors.stepByStep.right);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('?????????????????? ?????????????? ?? ?????????? ????????????', () => {
        it('Desktop', async () => {
            const Gate = {
                create: () => Promise.resolve(),
            };
            const customStore: DeepPartial<IUniversalStore> = {
                ...store,
                userPersonalDataPhone: {
                    fields: {
                        [UserPersonalDataPhoneFieldType.PHONE]: {
                            id: UserPersonalDataPhoneFieldType.PHONE,
                            value: '+79876543210',
                        },
                    },
                },
                legacyUser: {
                    phone: '+79876543210',
                },
            };

            await render(<Component store={customStore} Gate={Gate} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.SURNAME, '????????????     ');

            await page.focus(selectors.inputs.NAME);

            await page.keyboard.type('????????');

            await page.focus(selectors.inputs.SURNAME);

            await page.keyboard.type('22');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('Touch', async () => {
            await render(<Component store={mobileStore} />, renderOptions[1]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.inputs.SURNAME);

            await page.type(selectors.stepByStep.inputs.SURNAME, '????????????    ');

            await page.click(selectors.stepByStep.right);

            await page.type(selectors.stepByStep.inputs.NAME, '????????');

            await page.click(selectors.stepByStep.left);

            await page.focus(selectors.stepByStep.inputs.SURNAME);

            await page.keyboard.type('22');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('?????????????????? ???????????? ?? ?????????????????? ?? ????????????????????????', () => {
        it('?????????????? ???????????? ???? ???? ??????????????????????', async () => {
            const customStore: DeepPartial<IUniversalStore> = {
                ...filledStore,
                userPersonalDataPhone: {
                    fields: {
                        [UserPersonalDataPhoneFieldType.PHONE]: {
                            id: UserPersonalDataPhoneFieldType.PHONE,
                            value: '+79992134913',
                        },
                    },
                },
                legacyUser: {
                    ...filledStore.legacyUser,
                    phone: undefined,
                },
            };

            await render(<Component store={customStore} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('?????????????? ??????????????????????', async () => {
            const customStore: DeepPartial<IUniversalStore> = {
                ...filledStore,
                userPersonalDataPhone: {
                    fields: {
                        [UserPersonalDataPhoneFieldType.PHONE]: {
                            id: UserPersonalDataPhoneFieldType.PHONE,
                            value: '+79992134916',
                        },
                    },
                },
            };

            await render(<Component store={customStore} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('???????????? ?????????????? ???? ??????????????????????', async () => {
            const customStore: DeepPartial<IUniversalStore> = {
                ...filledStore,
                userPersonalDataPhone: {
                    fields: {
                        [UserPersonalDataPhoneFieldType.PHONE]: {
                            id: UserPersonalDataPhoneFieldType.PHONE,
                            value: '+79876543210',
                        },
                    },
                },
            };

            await render(<Component store={customStore} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('???????????? ?? ??????????', () => {
        it('???????????? ???????????? ?? ???????????????? ????????????????????', async () => {
            const Gate = {
                create: () => new Promise(noop),
            };

            const customStore: DeepPartial<IUniversalStore> = {
                ...filledStore,
                userPersonalDataPhone: {
                    fields: {
                        [UserPersonalDataPhoneFieldType.PHONE]: {
                            id: UserPersonalDataPhoneFieldType.PHONE,
                            value: '+79992134916',
                        },
                    },
                },
            };

            await render(<Component store={customStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('???? ?????????????? ?????????????????? ???????????? ????????????', async () => {
            const Gate = {
                create: () => Promise.reject(),
            };

            const customStore: DeepPartial<IUniversalStore> = {
                ...filledStore,
                userPersonalDataPhone: {
                    fields: {
                        [UserPersonalDataPhoneFieldType.PHONE]: {
                            id: UserPersonalDataPhoneFieldType.PHONE,
                            value: '+79992134916',
                        },
                    },
                },
            };

            await render(<Component store={customStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('???????????? ???????????? ?????????????? ??????????????????', async () => {
            const Gate = {
                create: () => {
                    return Promise.resolve();
                },
            };

            const customStore: DeepPartial<IUniversalStore> = {
                ...filledStore,
                userPersonalDataPhone: {
                    fields: {
                        [UserPersonalDataPhoneFieldType.PHONE]: {
                            id: UserPersonalDataPhoneFieldType.PHONE,
                            value: '+79992134916',
                        },
                    },
                },
            };

            await render(<Component store={customStore} Gate={Gate} />, renderOptions[0]);

            await page.click(selectors.sendSubmit);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    describe('?????????? ??????????????????', () => {
        it('?????????????? ???????????????? ??????????????', async () => {
            await render(<Component store={store} isManagerForm={true} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
