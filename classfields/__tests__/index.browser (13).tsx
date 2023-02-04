import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { HouseServicesRole } from 'types/houseServices';

import { AppProvider } from 'view/libs/test-helpers';
import { userReducer } from 'view/entries/user/reducer';

import { HouseServicesPeriod, IHouseServicesPeriodProps } from '../';

import * as stub from './stub';

const mobileViewports = [
    { width: 345, height: 200 },
    { width: 360, height: 200 },
] as const;

const desktopViewports = [
    { width: 1000, height: 200 },
    { width: 1200, height: 200 },
] as const;

const viewports = [...mobileViewports, ...desktopViewports] as const;

const Component = (props: IHouseServicesPeriodProps) => {
    return (
        <AppProvider rootReducer={userReducer}>
            <HouseServicesPeriod {...props} />
        </AppProvider>
    );
};

describe('HouseServicesPeriod', () => {
    describe('Дата передачи не наступила', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.notSentState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Дата передачи наступила', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.shouldBeSentState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Отправлено менее 15 мин назад', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.toSentState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Отправлено более 15 мин назад', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.sentState} />, { viewport });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Показания отклонены', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.declinedState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Дата передачи просрочена', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.expiredState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Показания с документами', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.withDocsState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Можно отправить документы', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.docsCanBeSentState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Пора отправить документы', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.docsShouldBeSentState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Документы отправлены', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.docsSentState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
    describe('Документы отклонены', () => {
        viewports.forEach((viewport) => {
            it(`${viewport.width} px`, async () => {
                await render(<Component role={HouseServicesRole.TENANT} {...stub.docsDeclinedState} />, {
                    viewport,
                });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
