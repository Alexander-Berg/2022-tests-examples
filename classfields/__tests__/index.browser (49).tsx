import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject, Flavor } from 'realty-core/types/utils';

import { FlatInfoFlatRooms, FlatInfoFlatType, FlatStatus, FlatUserRole, IFlat } from 'types/flat';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/user/reducer';

import stepByStepModalStyles from 'view/components/StepByStep/StepByStepModal/styles.module.css';
import { Fields } from 'view/modules/ownerFlatQuestionnaireForm/types';

import ownerFlatQuestionnaireFormStyles from '../styles.module.css';

import { OwnerFlatQuestionnaireFormContainer } from '../container';

import { filledStore, mobileStore, onlyContentFilledStore, skeletonStore, store } from './stub/store';

const renderOptions = [{ viewport: { width: 630, height: 1000 } }, { viewport: { width: 375, height: 1000 } }];

const selectors = {
    sendSubmit: `.${ownerFlatQuestionnaireFormStyles.submit}.Button`,
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
    <AppProvider
        rootReducer={rootReducer}
        initialState={store}
        Gate={Gate}
        bodyBackgroundColor={AppProvider.PageColor.USER_LK}
    >
        <OwnerFlatQuestionnaireFormContainer />
    </AppProvider>
);

describe('OwnerFlatQuestionnaireForm', () => {
    describe(`?????????????? ??????????????????`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={store} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`?????????????????????? ??????????????????`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={filledStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`?????????????????????? ?????????????????? Only content`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={onlyContentFilledStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`????????????????`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width} px`, async () => {
                await render(<Component store={skeletonStore} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('?????????? ???????????? ?????? ???????????? ??????????`', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={store} />, renderOption);

                await page.type(selectors.inputs.FLAT_ENTRANCE, '4');

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

            await render(<Component store={store} Gate={Gate} />, renderOptions[0]);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.type(selectors.inputs.FLAT_ENTRANCE, '4');
            await page.type(selectors.inputs.FLAT_FLOOR, '18');
            await page.type(selectors.inputs.FLAT_INTERCOM_CODE, '300');

            await page.type(selectors.inputs.FLAT_AREA, '184');
            await page.focus(selectors.inputs.FLAT_ROOMS);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');
            await page.focus(selectors.inputs.FLAT_TYPE);
            await page.keyboard.press('ArrowDown');
            await page.keyboard.press('Enter');

            await page.type(selectors.inputs.DESIRED_RENT_PRICE, '500000');

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        describe('???????????????????? ???????? ?????????? ?????????? ?????????????????? ??????????', () => {
            it('???????????? ?? ????????????????', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.inputs.FLAT_ENTRANCE);
                await page.type(selectors.stepByStep.inputs.FLAT_ENTRANCE, '4');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_FLOOR, '18');
                await page.click(selectors.stepByStep.right);
                await page.type(selectors.stepByStep.inputs.FLAT_INTERCOM_CODE, '300');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('?????????? ???????????????????? ?? ????????????????', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.inputs.FLAT_AREA);
                await page.type(selectors.stepByStep.inputs.FLAT_AREA, '184');
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.FLAT_ROOMS);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.stepByStep.right);
                await page.focus(selectors.stepByStep.inputs.FLAT_TYPE);
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });

            it('???????????????? ??????????', async () => {
                await render(<Component store={mobileStore} />, renderOptions[1]);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.inputs.DESIRED_RENT_PRICE);
                await page.type(selectors.stepByStep.inputs.DESIRED_RENT_PRICE, '500000');
                await page.click(selectors.stepByStep.right);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('???????????????? ??????????', () => {
        renderOptions.forEach((renderOption) => {
            it(`?????????????? ?????????????????? ${renderOption.viewport.width} px`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'owner.update_flat_questionnaire': {
                                return Promise.resolve<{ flat: IFlat }>({
                                    flat: {
                                        flatId: 'e2cf82992ff545c99b63b493a0235bc3' as Flavor<string, 'FlatID'>,
                                        address: {
                                            address: '????????????, ??????????-??????????????????, ???????????????????? ????????????????, 22',
                                            flatNumber: '13',
                                            addressFromStreetToFlat: '???????????????????? ????????????????, 22, ????. 13',
                                        },
                                        phone: '+79058311124',
                                        person: {
                                            name: '????????????',
                                            surname: '??????????????',
                                            patronymic: '??????????????????',
                                        },
                                        status: FlatStatus.CONFIRMED,
                                        userRole: FlatUserRole.OWNER,
                                        assignedUsers: [],
                                        email: 'email0000@yandex.ru',
                                        images: [],
                                        code: '81-AAAG',
                                        flatInfo: {
                                            entrance: 3,
                                            floor: 3,
                                            intercom: {
                                                code: '333',
                                            },
                                            flatType: FlatInfoFlatType.FLAT,
                                            rooms: FlatInfoFlatRooms.ONE,
                                            area: 3,
                                            desiredRentPrice: '300000',
                                        },
                                    },
                                });
                            }
                        }
                    },
                };

                await render(<Component store={filledStore} Gate={Gate} />, renderOption);

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`???????????????????? ?? ???????????????? ${renderOption.viewport.width} px`, async () => {
                const Gate = {
                    create: () => new Promise(noop),
                };

                await render(<Component store={filledStore} Gate={Gate} />, renderOption);

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        renderOptions.forEach((renderOption) => {
            it(`???? ?????????????? ?????????????????? ${renderOption.viewport.width} px`, async () => {
                const Gate = {
                    create: () => Promise.reject(),
                };

                await render(<Component store={filledStore} Gate={Gate} />, renderOption);

                await page.click(selectors.sendSubmit);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
