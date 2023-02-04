import React from 'react';
import { render } from 'jest-puppeteer-react';

import { advanceTo } from 'jest-date-mock';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IManagerFlat } from 'types/flat';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';
import { IUniversalStore } from 'view/modules/types';

import { ManagerSearchFlatsItem } from '../index';

import * as stubs from './stubs/flats';

advanceTo(new Date('2021-02-4 12:00'));

const renderOptions = [{ viewport: { width: 1200, height: 400 } }, { viewport: { width: 460, height: 600 } }];

const Component: React.FunctionComponent<{ store?: DeepPartial<IUniversalStore>; item: IManagerFlat }> = ({
    store = {},
    item,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <ManagerSearchFlatsItem item={item} />
    </AppProvider>
);

describe('ManagerSearchFlatsItem', () => {
    describe('есть данные только из заявки', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.applicationOwner} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть данные только по арендатору из привязанных', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.assignedTenant} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть данные из привязанных пользователей', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.assignedUsers} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Аренда окончена, показ предыдущего жильца', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.assignedUsers} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть данные из заявки и из привязанных о собственнике - данные совпадают', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.applicationOwnerMatchAssignedOwner} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть данные из заявки и из привязанных о собственнике - данные не совпадают', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.applicationOwnerNotMatchAssignedOwner} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть данные из контракта', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.contractData} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть данные из контракта и привязанных пользователей', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.contractAndAssigned} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('В квартире вообще нет данных(только адрес)', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.emptyData} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть ссылка на амо', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.hasAmoLink} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть дата расторжения', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.hasTerminationDate} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть задолженность', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.hasPaymentOverdueDays} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Есть информация о платеже', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.hasPaymentInfo} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Выплата 31 числа (текущий месяц февраль)', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.hasLastFebruaryLastMonthDay} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Следущая выплата попадает на следующий год', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.hasPaymentNextYear} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Квартира создана менеджером', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.createByModeration} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Квартира создана собственником', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component item={stubs.createByOwner} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    stubs.flatStatus().map((item: IManagerFlat) => {
        describe(`Статус квартиры: ${item.flat.status}`, () => {
            renderOptions.forEach((option) => {
                it(`${option.viewport.width}px`, async () => {
                    await render(<Component item={item} />, option);

                    expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                });
            });
        });
    });
});
