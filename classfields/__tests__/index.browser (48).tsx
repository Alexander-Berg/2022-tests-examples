import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import suggestListStyles from 'view/components/SuggestList/styles.module.css';
import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';

import flatDraftFormStyles from '../OwnerFlatDraftForm/styles.module.css';
import flatDraftConfirmationStyles from '../OwnerFlatDraftConfirmation/styles.module.css';

import { OwnerFlatDraftContainer } from '../container';

import {
    confirmationStore,
    storeWithAddress,
    confirmationStoreWithoutSms,
    mobileStore,
    skeletonStore,
    store,
} from './stub/store';

const renderOptions = [{ viewport: { width: 625, height: 1000 } }, { viewport: { width: 415, height: 1000 } }];

const Component: React.FunctionComponent<{
    store: DeepPartial<IUniversalStore>;
    Gate?: AnyObject;
    isConfirmationStage?: boolean;
}> = (props) => (
    <AppProvider
        rootReducer={userReducer}
        Gate={props.Gate}
        initialState={props.store}
        fakeTimers={{
            now: new Date('2020-06-01T03:00:00.111Z').getTime(),
        }}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <OwnerFlatDraftContainer isConfirmationStage={props.isConfirmationStage} />
    </AppProvider>
);

const selectors = {
    sendSubmit: `.${flatDraftFormStyles.buttons} .Button`,
    saveSubmit: `.${flatDraftFormStyles.buttons} .Link`,
    signSubmit: `.${flatDraftConfirmationStyles.button}`,
    inputs: {
        phone: '#PHONE',
        address: '#ADDRESS',
        flatNumber: '#FLAT_NUMBER',
        name: '#NAME',
        surname: '#SURNAME',
        patronymic: '#PATRONYMIC',
        confirmationCode: '#CONFIRMATION_CODE',
        email: '#EMAIL',
    },
    stepByStep: {
        left: `.${stepByStepModalStyles.leftButton}`,
        right: `.${stepByStepModalStyles.rightButton}`,
        close: `.${stepByStepModalStyles.modal} .IconSvg_close-24`,
        suggest: {
            itemN: (n: number) =>
                `.${stepByStepModalStyles.modal} .${suggestListStyles.list} .${suggestListStyles.item}:nth-child(${n})`,
        },
        inputs: {
            phone: `.${stepByStepModalStyles.modal} #PHONE`,
            address: `.${stepByStepModalStyles.modal} #ADDRESS`,
            flatNumber: `.${stepByStepModalStyles.modal} #FLAT_NUMBER`,
            name: `.${stepByStepModalStyles.modal} #NAME`,
            surname: `.${stepByStepModalStyles.modal} #SURNAME`,
            patronymic: `.${stepByStepModalStyles.modal} #PATRONYMIC`,
            email: `.${stepByStepModalStyles.modal} #EMAIL`,
        },
    },
    repeatConfirmation: `.${flatDraftConfirmationStyles.repeat} .Link`,
    suggest: {
        itemN: (n: number) => `.${suggestListStyles.list} .${suggestListStyles.item}:nth-child(${n})`,
    },
};

describe('OwnerFlatDraft', () => {
    describe('?????????????? ??????', () => {
        describe(`?????????????? ??????????????????`, () => {
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
    });

    describe('?????????? ????????????', () => {
        describe(`???????????? ?????????????????????????? ?????????? ?????? ?????????? ???? ???????????? "??????????????????"`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`???????????? ?????????????????????????? ?????????? ?????? ?????????? ???? ???????????? "?????????????????? ????????????????"`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    await page.click(selectors.saveSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`???????????????????????? ???????????? ????????????????`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    await page.type(selectors.inputs.phone, '123');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`???????????? ?????????? ?????? ????????????????`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithAddress} />, renderOption);

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('???????????? ?????????????????? ?????? ?????????? ?? ????????', () => {
            it(`${renderOptions[0].viewport.width}px`, async () => {
                await render(<Component store={storeWithAddress} />, renderOptions[0]);

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.flatNumber, '78');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.name, '????????');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.surname, '????????????');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.phone, '79500000000');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.email, 'a@a.ru');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('???????????????????? ??????????', () => {
        describe(`${renderOptions[0].viewport.width}px`, () => {
            describe('?????????? ????????????', () => {
                it(`???????????????????? ?????????? ?? ?????????????????? ?? ?????????????????? ????????????????`, async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    return Promise.resolve({
                                        flat: {
                                            flatId: '53fa34e56b044674b81c2d3c713a5d47',
                                            address: {
                                                address: '????????????, ??????????-??????????????????, ?????????????????????? ????????????, 8',
                                                flatNumber: '342',
                                            },
                                            person: {},
                                            status: 'DRAFT',
                                            userRole: 'OWNER',
                                        },
                                    });
                                }
                            }
                        },
                    };

                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOptions[0]);

                    await page.type(selectors.inputs.flatNumber, '342');

                    await page.click(selectors.saveSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it(`???????????????????? ?????????? ?? ?????????????????? ?? ???????? ???????????? ???????????????? ??????????????????`, async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    return new Promise(noop);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOptions[0]);

                    await page.type(selectors.inputs.flatNumber, '342');

                    await page.click(selectors.saveSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it(`???????????????????? ?????????? ?? ?????????????????? ?? ???? ?????????????? ?????????????? ????????????????`, async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    return Promise.reject();
                                }
                            }
                        },
                    };

                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOptions[0]);

                    await page.type(selectors.inputs.flatNumber, '342');

                    await page.click(selectors.saveSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });

            describe('???????????? ????????', () => {
                it.skip('???????????????????? ?? ????????????????', async () => {
                    const Gate = {
                        create: (path: string, params: AnyObject) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    if (params?.fields?.NAME) {
                                        return new Promise(noop);
                                    }

                                    return Promise.resolve({
                                        flat: {
                                            flatId: '53fa34e56b044674b81c2d3c713a5d47',
                                            address: {
                                                address: '????????????, ??????????-??????????????????, ?????????????????????? ????????????, 8',
                                                flatNumber: '342',
                                            },
                                            person: {},
                                            status: 'DRAFT',
                                            userRole: 'OWNER',
                                        },
                                    });
                                }
                            }
                        },
                    };

                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOptions[0]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.inputs.flatNumber, '342');

                    await page.type(selectors.inputs.name, '????????');

                    await page.type(selectors.inputs.surname, '????????????');

                    await page.type(selectors.inputs.patronymic, '????????????????');

                    await page.type(selectors.inputs.phone, '79500000000');

                    await page.type(selectors.inputs.email, 'a@a.ru');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it.skip('???? ?????????????? ?????????????????? ????????????????', async () => {
                    const Gate = {
                        create: (path: string, params: AnyObject) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    if (params?.fields?.NAME) {
                                        return Promise.reject();
                                    }

                                    return Promise.resolve({
                                        flat: {
                                            flatId: '53fa34e56b044674b81c2d3c713a5d47',
                                            address: {
                                                address: '????????????, ??????????-??????????????????, ?????????????????????? ????????????, 8',
                                                flatNumber: '342',
                                            },
                                            person: {},
                                            status: 'DRAFT',
                                            userRole: 'OWNER',
                                        },
                                    });
                                }
                            }
                        },
                    };

                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOptions[0]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.inputs.flatNumber, '342');

                    await page.type(selectors.inputs.name, '????????');

                    await page.type(selectors.inputs.surname, '????????????');

                    await page.type(selectors.inputs.patronymic, '????????????????');

                    await page.type(selectors.inputs.phone, '79500000000');

                    await page.type(selectors.inputs.email, 'a@a.ru');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it.skip('???????????????? ???????????????? ??????', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    return Promise.resolve({
                                        flat: {
                                            flatId: '53fa34e56b044674b81c2d3c713a5d47',
                                            address: {
                                                address: '????????????, ??????????-??????????????????, ?????????????????????? ????????????, 8',
                                                flatNumber: '33',
                                            },
                                            person: {
                                                name: '????????',
                                                surname: '????????????',
                                                patronymic: '????????????????',
                                            },
                                            phone: '+79500000000',
                                            status: 'DRAFT',
                                            userRole: 'OWNER',
                                        },
                                    });
                                }
                                case 'owner-flat-draft.send_flat_draft_confirmation_code': {
                                    return Promise.resolve({ sentSmsInfo: { codeLength: 5 } });
                                }
                            }
                        },
                    };

                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOptions[0]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.inputs.flatNumber, '33');

                    await page.type(selectors.inputs.name, '????????');

                    await page.type(selectors.inputs.surname, '????????????');

                    await page.type(selectors.inputs.patronymic, '????????????????');

                    await page.type(selectors.inputs.phone, '79500000000');

                    await page.type(selectors.inputs.email, 'a@a.ru');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it.skip('???????????????? ?????? ?? ????????????????', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    return Promise.resolve({
                                        flat: {
                                            flatId: '53fa34e56b044674b81c2d3c713a5d47',
                                            address: {
                                                address: '????????????, ??????????-??????????????????, ?????????????????????? ????????????, 8',
                                                flatNumber: '342',
                                            },
                                            person: {},
                                            status: 'DRAFT',
                                            userRole: 'OWNER',
                                        },
                                    });
                                }
                                case 'owner-flat-draft.send_flat_draft_confirmation_code': {
                                    return new Promise(noop);
                                }
                            }
                        },
                    };

                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOptions[0]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.inputs.flatNumber, '342');

                    await page.type(selectors.inputs.name, '????????');

                    await page.type(selectors.inputs.surname, '????????????');

                    await page.type(selectors.inputs.patronymic, '????????????????');

                    await page.type(selectors.inputs.phone, '79500000000');

                    await page.type(selectors.inputs.email, 'a@a.ru');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it.skip('???? ?????????????? ?????????????????? ??????', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    return Promise.resolve({
                                        flat: {
                                            flatId: '53fa34e56b044674b81c2d3c713a5d47',
                                            address: {
                                                address: '????????????, ??????????-??????????????????, ?????????????????????? ????????????, 8',
                                                flatNumber: '342',
                                            },
                                            person: {},
                                            status: 'DRAFT',
                                            userRole: 'OWNER',
                                        },
                                    });
                                }
                                case 'owner-flat-draft.send_flat_draft_confirmation_code': {
                                    return Promise.reject();
                                }
                            }
                        },
                    };

                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOptions[0]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.type(selectors.inputs.flatNumber, '342');

                    await page.type(selectors.inputs.name, '????????');

                    await page.type(selectors.inputs.surname, '????????????');

                    await page.type(selectors.inputs.patronymic, '????????????????');

                    await page.type(selectors.inputs.phone, '79500000000');

                    await page.type(selectors.inputs.email, 'a@a.ru');

                    await page.click(selectors.sendSubmit);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`${renderOptions[1].viewport.width}px`, () => {
            describe('???????????????? ???????????? ?????????????????????? ?????? ?????????? ???? ????????????', () => {
                Object.keys(selectors.inputs)
                    .filter((key) => key !== 'confirmationCode')
                    .forEach((key) => {
                        it(key, async () => {
                            await render(<Component store={mobileStore} />, renderOptions[1]);

                            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                            await page.click(selectors.inputs[key]);

                            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                        });
                    });
            });

            describe('???????????? ?? ?????????????????? ????????????', () => {
                it('???????????? ?? ???????????? ???????????????????????????????? ?? ????????????', async () => {
                    await render(<Component store={mobileStore} />, renderOptions[1]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.sendSubmit);

                    await page.click(selectors.inputs.phone);

                    await page.type(selectors.stepByStep.inputs.phone, '123');

                    await page.click(selectors.stepByStep.close);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });

                it('???????????????????? ?????????? ?? ???????????????? ?? ???? ????????????????????????', async () => {
                    const Gate = {
                        create: (path: string) => {
                            switch (path) {
                                case 'owner-flat-draft.update_flat_draft': {
                                    return Promise.resolve({
                                        flat: {
                                            flatId: '53fa34e56b044674b81c2d3c713a5d47',
                                            address: {
                                                address: '????????????, ??????????-??????????????????, ?????????????????????? ????????????, 8',
                                                flatNumber: '33',
                                            },
                                            person: {},
                                            status: 'DRAFT',
                                            userRole: 'OWNER',
                                        },
                                    });
                                }
                            }
                        },
                    };

                    await render(<Component store={mobileStore} Gate={Gate} />, renderOptions[1]);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.inputs.address);

                    await page.type(
                        selectors.stepByStep.inputs.address,
                        '????????????, ??????????-??????????????????, ?????????????????????? ????????????, 8'
                    );

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.flatNumber, '33');

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.surname, '????????????');

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.name, '????????');

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.patronymic, '????????????????');

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.phone, '79500000000');

                    await page.click(selectors.stepByStep.right);

                    await page.type(selectors.stepByStep.inputs.email, 'a@a.ru');

                    await page.click(selectors.stepByStep.right);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('???????????????????? ????????????', () => {
        describe('?????????????? ??????', () => {
            renderOptions.forEach((renderOption) => {
                it(`???? ?????????????? ?????? ${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={confirmationStore} isConfirmationStage />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('?????? ???????? ????????????????????', () => {
            it(`???? ?????????????? ??????`, async () => {
                await render(<Component store={confirmationStore} isConfirmationStage />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.signSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`?????????????????? ?????????????????? ????????`, async () => {
                await render(<Component store={confirmationStore} isConfirmationStage />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.confirmationCode, '123');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.signSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`?????????? ?????????????? ????????????????, ?????? ?????? ????????????????????????`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'owner-flat-draft.confirm_flat_draft_confirmation_code': {
                                return Promise.resolve({
                                    error: {
                                        code: 'VALIDATION_ERROR',
                                        data: {
                                            validationErrors: [
                                                {
                                                    code: 'INVALID_CODE',
                                                },
                                            ],
                                        },
                                    },
                                });
                            }
                        }
                    },
                };

                await render(<Component store={confirmationStore} Gate={Gate} isConfirmationStage />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.confirmationCode, '12345');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.signSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
            it(`?????????? ?????????????? ?????????????????????????? ??????????`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'owner-flat-draft.confirm_flat_draft_confirmation_code': {
                                return Promise.reject();
                            }
                        }
                    },
                };

                await render(<Component store={confirmationStore} Gate={Gate} isConfirmationStage />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.confirmationCode, '12345');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.signSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`?????????? ?????????????? ???????????????? ??????????????`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'owner-flat-draft.confirm_flat_draft_confirmation_code': {
                                return Promise.resolve({});
                            }
                        }
                    },
                };

                await render(<Component store={confirmationStore} Gate={Gate} isConfirmationStage />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.confirmationCode, '12345');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.signSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`?????????? ?????????????? ?? ???????????????? ????????????`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'owner-flat-draft.confirm_flat_draft_confirmation_code': {
                                return new Promise(noop);
                            }
                        }
                    },
                };

                await render(<Component store={confirmationStore} Gate={Gate} isConfirmationStage />, renderOptions[0]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.confirmationCode, '12345');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.signSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        describe('?????????????????? ???????????????? ??????', () => {
            it(`?????????????????? ???????????????? ?????? ?? ????????????????`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'owner-flat-draft.send_flat_draft_confirmation_code': {
                                return new Promise(noop);
                            }
                        }
                    },
                };

                await render(
                    <Component store={confirmationStoreWithoutSms} Gate={Gate} isConfirmationStage />,
                    renderOptions[0]
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.repeatConfirmation);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`?????????????????? ???????????????? ?????? ??????????????`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'owner-flat-draft.send_flat_draft_confirmation_code': {
                                return Promise.resolve({ sentSmsInfo: { codeLength: 5, requestId: '123' } });
                            }
                        }
                    },
                };

                await render(
                    <Component store={confirmationStoreWithoutSms} Gate={Gate} isConfirmationStage />,
                    renderOptions[0]
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.repeatConfirmation);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it(`?????????????????? ???????????????? ?????? ????????????`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'owner-flat-draft.send_flat_draft_confirmation_code': {
                                return Promise.reject();
                            }
                        }
                    },
                };

                await render(
                    <Component store={confirmationStoreWithoutSms} Gate={Gate} isConfirmationStage />,
                    renderOptions[0]
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.repeatConfirmation);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
