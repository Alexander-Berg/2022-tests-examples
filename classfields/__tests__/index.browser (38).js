import React from 'react';

import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { daysByInterval } from 'view/lib/weekdays';
import { AppProvider } from 'view/lib/test-helpers';

import { ProductSchedulesModalContainer } from '../container';
import styles from '../components/ScheduleContent/styles.module.css';

advanceTo(new Date('2020-06-16T16:08:00Z'));

const getStore = ({ items, payload } = {}) => ({
    popups: {
        productSchedules: {
            visible: true,
            payload: {
                schedules: {
                    status: 'ENABLED',
                    scheduleOncePolicy: {
                        manual: {
                            items
                        }
                    }
                },
                offerIds: [ '0' ],
                productType: 'raising',
                isSchedulesProcessing: false,
                ...payload
            }
        }
    }
});

const Component = ({ store }) => (
    <AppProvider initialState={store}>
        <ProductSchedulesModalContainer />
    </AppProvider>
);

describe('ProductSchedulesModal', () => {
    it('стандартный вид', async() => {
        await render(
            <Component
                store={getStore({
                    payload: {
                        schedules: {
                            status: 'DISABLED'
                        }
                    },
                    items: [ {
                        startTime: '2020-06-16T16:08:00Z',
                        daysOfWeek: daysByInterval.everyday
                    },
                    {
                        startTime: '2020-06-16T08:41:00Z',
                        daysOfWeek: daysByInterval.weekends
                    } ]
                })}
            />,
            { viewport: { width: 1100, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('заблокировано сохранение', async() => {
        await render(
            <Component
                store={getStore({
                    payload: {
                        isSchedulesProcessing: true
                    }
                })}
            />,
            { viewport: { width: 1100, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('расписания не вмещаются в окно', async() => {
        await render(
            <Component
                store={
                    getStore({
                        items: [
                            {
                                startTime: '2020-06-16T16:08:00Z',
                                daysOfWeek: daysByInterval.everyday
                            },
                            {
                                startTime: '2020-06-16T04:20:00Z',
                                daysOfWeek: daysByInterval.weekday
                            },
                            {
                                startTime: '2020-06-16T03:55:00Z',
                                daysOfWeek: daysByInterval.weekends
                            },
                            {
                                startTime: '2020-06-16T17:00:00Z',
                                daysOfWeek: daysByInterval.everyday
                            },
                            {
                                startTime: '2020-06-16T16:08:00Z',
                                daysOfWeek: daysByInterval.everyday
                            },
                            {
                                startTime: '2020-06-16T08:41:00Z',
                                daysOfWeek: daysByInterval.weekends
                            }
                        ]
                    })
                }
            />,
            { viewport: { width: 1100, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('все элементы удалили', async() => {
        await render(
            <Component store={getStore({ items: [] })} />,
            { viewport: { width: 1100, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('показываются ошибки, когда расписание планируется в одно время', () => {
        it('каждый день и будни', async() => {
            await render(
                <Component
                    store={getStore({
                        items: [
                            {
                                startTime: '2020-06-16T12:30:00Z',
                                daysOfWeek: daysByInterval.everyday
                            },
                            {
                                startTime: '2020-06-16T12:30:00Z',
                                daysOfWeek: daysByInterval.weekday
                            }
                        ]
                    })}
                />,
                { viewport: { width: 1100, height: 900 } }
            );

            await page.click(`.${styles.save}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('каждый день и выходные', async() => {
            await render(
                <Component
                    store={getStore({
                        items: [
                            {
                                startTime: '2020-06-16T12:30:00Z',
                                daysOfWeek: daysByInterval.everyday
                            },
                            {
                                startTime: '2020-06-16T12:30:00Z',
                                daysOfWeek: daysByInterval.weekends
                            }
                        ]
                    })}
                />,
                { viewport: { width: 1100, height: 900 } }
            );

            await page.click(`.${styles.save}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('в будни', async() => {
            await render(
                <Component
                    store={getStore({
                        items: [
                            {
                                startTime: '2020-06-16T12:30:00Z',
                                daysOfWeek: daysByInterval.weekday
                            },
                            {
                                startTime: '2020-06-16T12:30:00Z',
                                daysOfWeek: daysByInterval.weekday
                            }
                        ]
                    })}
                />,
                { viewport: { width: 1100, height: 900 } }
            );

            await page.click(`.${styles.save}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
