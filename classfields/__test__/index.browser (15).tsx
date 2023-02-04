import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';
import noop from 'lodash/noop';

import dayjs from '@realty-front/dayjs';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { rootReducer } from 'view/entries/outstaff/reducer';

import { OutstaffCallCenterFormContainer } from '../container';
import styles from '../styles.module.css';

import {
    store,
    skeletonStore,
    storeWithAddress,
    storeWithFilledForm,
    storeWithNoShowing,
    storeWithALotOfShowings,
    storeWithPreFinalShowings,
    onlyOnlineShowingsStore,
} from './stub/store';

const renderOptions = [{ viewport: { width: 1280, height: 1000 } }, { viewport: { width: 375, height: 1000 } }];

const selectors = {
    findFlats: `.${styles.suggestButton}`,
    chooseFlat: (n: number) => `.RadioGroup .Radio:nth-child(${n})`,
    inputs: {
        address: '#ADDRESS',
        name: '#TENANT_NAME',
        phone: '#TENANT_PHONE',
        numberOfAdults: '#NUMBER_OF_ADULTS',
        numberOfChildren: '#NUMBER_OF_CHILDREN',
        withPets: '#WITH_PETS',
        descriptionOfPets: '#DESCRIPTION_OF_PETS',
        showingType: '#SHOWING_TYPE',
        showingDate: '#SHOWING_DATE',
        onlineShowingDate: '#ONLINE_SHOWING_DATE',
        onlineShowingSlot: '#ONLINE_SHOWING_SLOT',
        tenantQuestion: '#TENANT_QUESTION',
        tenantComment: '#TENANT_COMMENT',
    },
    submitForm: `.${styles.submitButton}`,
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={rootReducer} Gate={props.Gate} initialState={props.store}>
        <OutstaffCallCenterFormContainer />
    </AppProvider>
);

const flatsMock = [
    {
        address: 'Россия, Санкт-Петербург, Пискаревский пр-кт, 1',
        flatId: '1',
        area: 60,
        floor: 5,
        cost: 3000000,
    },
    {
        address: 'Россия, Санкт-Петербург, Пискаревский пр-кт, 2',
        flatId: '2',
        area: 70,
        floor: 10,
        cost: 4000000,
    },
];

const GateStubCommon = {
    create: (path: string) => {
        switch (path) {
            case 'outstaff.get_flats_by_address': {
                return Promise.resolve({
                    flats: flatsMock,
                });
            }
            case 'outstaff.create_flat_showing': {
                return Promise.resolve({});
            }
        }
    },
};

const GateStubOnlineShowing = {
    create: (path: string) => {
        switch (path) {
            case 'outstaff.get_flats_by_address': {
                return Promise.resolve({
                    flats: flatsMock,
                });
            }
            case 'outstaff.get_available_showings_slots': {
                return Promise.resolve({
                    availableSlotsByDay: [
                        {
                            date: dayjs('2021-12-24 12:00').format(),
                            availableSlots: [
                                {
                                    calendarId: '123456',
                                    dateTime: dayjs('2021-12-24 13:30').format(),
                                },
                                {
                                    calendarId: '123456',
                                    dateTime: dayjs('2021-12-24 15:45').format(),
                                },
                            ],
                        },
                        {
                            date: dayjs('2021-12-25 12:00').format(),
                            availableSlots: [
                                {
                                    calendarId: '123456',
                                    dateTime: dayjs('2021-12-25 12:30').format(),
                                },
                            ],
                        },
                    ],
                });
            }
            case 'outstaff.create_flat_showing': {
                return Promise.resolve({});
            }
        }
    },
};

describe('OutstaffCallCenterForm', () => {
    describe('Внешний вид', () => {
        describe(`Базовое состояние`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={store} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`Показ скелетона`, () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={skeletonStore} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`показ длинного адреса в сниппете`, () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'outstaff.get_flats_by_address': {
                            return Promise.resolve({
                                flats: [
                                    {
                                        address: 'Россия, Санкт-Петербург, Большеохтинский проспект, 9',
                                        flatId: '1',
                                        area: 60,
                                        floor: 5,
                                        cost: 3000000,
                                    },
                                    {
                                        address: 'Россия, Санкт-Петербург, Полюстровский проспект, 245',
                                        flatId: '2',
                                        area: 70,
                                        floor: 10,
                                        cost: 4000000,
                                    },
                                ],
                            });
                        }
                    }
                },
            };

            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOption);

                    await page.click(selectors.findFlats);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe(`c бека не пришла цена`, () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'outstaff.get_flats_by_address': {
                            return Promise.resolve({
                                flats: [
                                    {
                                        address: 'Россия, Санкт-Петербург, Большеохтинский проспект, 9',
                                        flatId: '1',
                                        area: 60,
                                        floor: 5,
                                        cost: -1,
                                    },
                                    {
                                        address: 'Россия, Санкт-Петербург, Полюстровский проспект, 245',
                                        flatId: '2',
                                        area: 70,
                                        floor: 10,
                                        cost: -1,
                                    },
                                ],
                            });
                        }
                    }
                },
            };

            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithAddress} Gate={Gate} />, renderOption);

                    await page.click(selectors.findFlats);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Заполнение формы', () => {
        describe('Заполнение всех полей', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithAddress} Gate={GateStubCommon} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.findFlats);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.chooseFlat(2));

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    const select = await page.$(selectors.inputs.showingType);

                    await page.type(selectors.inputs.name, 'Иван');
                    await page.type(selectors.inputs.phone, '9115559922');
                    await page.type(selectors.inputs.numberOfAdults, '3');
                    await select?.focus();
                    await select?.press('Enter');
                    await select?.press('ArrowDown');
                    await select?.press('ArrowDown');
                    await select?.press('Enter');
                    await page.type(selectors.inputs.showingDate, 'в понедельник после восьми');
                    await page.type(selectors.inputs.tenantQuestion, 'А платить точно нужно?');
                    await page.type(selectors.inputs.tenantComment, 'Я бы не хотел платить');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Заполнение полей для онлайн показа', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithAddress} Gate={GateStubOnlineShowing} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.findFlats);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.chooseFlat(2));

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    const select = await page.$(selectors.inputs.showingType);
                    const slotSelect = await page.$(selectors.inputs.onlineShowingSlot);

                    await page.type(selectors.inputs.name, 'Иван');
                    await page.type(selectors.inputs.phone, '9115559922');
                    await page.type(selectors.inputs.numberOfAdults, '3');
                    await select?.focus();
                    await select?.press('Enter');
                    await select?.press('ArrowDown');
                    await select?.press('Enter');

                    await slotSelect?.focus();
                    await slotSelect?.press('Enter');
                    await slotSelect?.press('ArrowDown');
                    await slotSelect?.press('ArrowDown');
                    await slotSelect?.press('Enter');

                    await page.type(selectors.inputs.tenantQuestion, 'А платить точно нужно?');
                    await page.type(selectors.inputs.tenantComment, 'Я бы не хотел платить');

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Сниппеты квартир исчезают при изменении адреса', () => {
            const renderOption = renderOptions[0];

            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={storeWithAddress} Gate={GateStubCommon} />, renderOption);

                await page.click(selectors.findFlats);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.type(selectors.inputs.address, ', 10');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        describe('Показ ошибок при отправке незаполненной формы', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithAddress} Gate={GateStubCommon} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.findFlats);
                    await page.click(selectors.chooseFlat(2));
                    await page.waitFor(500);
                    await page.waitForSelector(selectors.submitForm, { visible: true });
                    await page.click(selectors.submitForm);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Ошибки исчезают при заполнении формы', () => {
            it(`${renderOptions[0].viewport.width}px`, async () => {
                await render(<Component store={storeWithAddress} Gate={GateStubCommon} />, renderOptions[0]);

                await page.click(selectors.findFlats);
                await page.click(selectors.chooseFlat(2));
                await page.waitFor(500);
                await page.waitForSelector(selectors.submitForm, { visible: true });
                await page.click(selectors.submitForm);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                const select = await page.$(selectors.inputs.showingType);

                await page.type(selectors.inputs.name, 'Иван');
                await page.type(selectors.inputs.phone, '9115559922');
                await page.type(selectors.inputs.numberOfAdults, '3');
                await select?.focus();
                await select?.press('Enter');
                await select?.press('ArrowDown');
                await select?.press('ArrowDown');
                await select?.press('Enter');
                await page.type(selectors.inputs.showingDate, 'в понедельник после восьми');

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    // todo: https://st.yandex-team.ru/VSIF-3179#6298aea496098109bfa80073
    describe.skip('Отправка формы', () => {
        const renderOption = renderOptions[0];

        describe('Успешная отправка формы', () => {
            it(`${renderOption.viewport.width}px`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'outstaff.create_flat_showing': {
                                return Promise.resolve({});
                            }
                        }
                    },
                };

                await render(<Component store={storeWithFilledForm} Gate={Gate} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submitForm);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        describe('В процессе отправки', () => {
            it(`${renderOption.viewport.width}px`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'outstaff.create_flat_showing': {
                                return new Promise(noop);
                            }
                        }
                    },
                };

                await render(<Component store={storeWithFilledForm} Gate={Gate} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submitForm);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        describe('Ошибка отправки', () => {
            it(`${renderOption.viewport.width}px`, async () => {
                const Gate = {
                    create: (path: string) => {
                        switch (path) {
                            case 'outstaff.create_flat_showing': {
                                return Promise.reject({});
                            }
                        }
                    },
                };

                await render(<Component store={storeWithFilledForm} Gate={Gate} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.submitForm);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Квартира с просмотрами', () => {
        describe('Нет просмотров', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithNoShowing} Gate={GateStubCommon} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.chooseFlat(1));

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Уже назначены 5 показов с подходящими резолюциями', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithALotOfShowings} Gate={GateStubCommon} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.chooseFlat(1));

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });

        describe('Есть показы в предфинальном статусе', () => {
            renderOptions.forEach((renderOption) => {
                it(`${renderOption.viewport.width}px`, async () => {
                    await render(<Component store={storeWithPreFinalShowings} Gate={GateStubCommon} />, renderOption);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                    await page.click(selectors.chooseFlat(1));

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });

    describe('Только онлайн показы', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={onlyOnlineShowingsStore} Gate={GateStubCommon} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

                await page.click(selectors.chooseFlat(1));

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
