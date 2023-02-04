import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import { daysByInterval } from 'view/lib/weekdays';

import { JuridicalVasPanel } from '../index';

advanceTo(new Date('2020-06-01T03:00:00.111Z'));

const initialStore = {
    user: {
        crc: '',
        vosUserData: {
            userType: 'AGENCY'
        }
    }
};

const Component = (props = {}) => (
    <AppProvider initialState={initialStore}>
        <JuridicalVasPanel {...props} />
    </AppProvider>
);

const getProps = ({ schedules = { status: 'DISABLED' }, mapProducts = noop } = {}) => {
    const products = [
        'raising',
        'promotion',
        'premium'
    ].map(type => ({
        type,
        isChangingNotCancelable: false,
        isChangingStatus: false,
        priceContext: { isAvailable: true, effective: 2, base: 2 },
        renewal: { status: 'UNAVAILABLE' },
        status: 'inactive',
        schedules: type === 'raising' ? schedules : { status: 'DISABLED' },
        ...mapProducts(type)
    }));

    return {
        balance: 100,
        offer: {
            photo: { orig: '' },
            restrictions: [],
            isEditable: true,
            services: products.reduce((acc, { type, ...service }) => {
                acc[type] = service;
                return acc;
            }, {})
        },
        products,
        onButtonClick: () => noop,
        onCheckRenewal: () => noop
    };
};

const schedules = [
    {
        daysOfWeek: daysByInterval.everyday,
        hasBoughtProduct: true,
        startTime: '2020-05-22T07:00:00Z'
    },
    {
        daysOfWeek: daysByInterval.weekday,
        hasBoughtProduct: true,
        startTime: '2020-05-22T17:00:00Z'
    },
    {
        daysOfWeek: daysByInterval.weekends,
        hasBoughtProduct: true,
        startTime: '2020-05-22T21:00:00Z'
    }
];

describe('JuridicalVasPanel', () => {
    it('render by default', async() => {
        await render(
            <Component {...getProps()} />,
            { viewport: { width: 1100, height: 180 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render by default for small screen', async() => {
        await render(
            <Component {...getProps()} />,
            { viewport: { width: 900, height: 150 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render with active schedules', async() => {
        await render(
            <Component
                {...getProps({
                    schedules: {
                        status: 'ENABLED',
                        scheduleOncePolicy: {
                            manual: {
                                items: schedules
                            }
                        }
                    },
                    mapProducts: type => ({ status: type === 'rasing' ? 'active' : 'inactive' })
                })}
            />,
            { viewport: { width: 1100, height: 180 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render with turned off schedules', async() => {
        await render(
            <Component
                {...getProps({
                    schedules: {
                        status: 'DISABLED',
                        scheduleOncePolicy: {
                            manual: {
                                items: schedules
                            }
                        }
                    }
                })}
            />,
            { viewport: { width: 1100, height: 180 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render with active feed schedules', async() => {
        await render(
            <Component
                {...getProps({
                    schedules: {
                        status: 'ENABLED',
                        scheduleOncePolicy: {
                            feed: {
                                items: schedules
                            }
                        }
                    },
                    mapProducts: type => ({ status: type === 'rasing' ? 'active' : 'inactive' })
                })}
            />,
            { viewport: { width: 1100, height: 180 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render with active premium', async() => {
        await render(
            <Component
                {...getProps({
                    mapProducts: type => ({ status: type === 'premium' ? 'active' : 'inactive' })
                })}
            />,
            { viewport: { width: 1100, height: 180 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render products in pending status', async() => {
        await render(
            <Component
                {...getProps({
                    schedules: {
                        status: 'ENABLED',
                        scheduleOncePolicy: {
                            manual: {
                                items: schedules.map(schedule =>
                                    ({ ...schedule, hasBoughtProduct: false })
                                )
                            }
                        }
                    },
                    mapProducts: () => ({
                        isChangingStatus: true
                    })
                })}
            />,
            { viewport: { width: 1100, height: 180 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render when all products are active', async() => {
        await render(
            <Component
                {...getProps({
                    schedules: {
                        status: 'ENABLED',
                        scheduleOncePolicy: {
                            manual: {
                                items: [ {
                                    daysOfWeek: daysByInterval.weekends,
                                    hasBoughtProduct: true,
                                    startTime: '2020-05-22T21:00:00Z'
                                } ]
                            }
                        }
                    },
                    mapProducts: () => ({ status: 'active' })
                })}
            />,
            { viewport: { width: 1100, height: 180 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render when all products are from feed', async() => {
        await render(
            <Component
                {...getProps({
                    mapProducts: () => ({ status: 'active', isAppliedFromFeed: true })
                })}
            />,
            { viewport: { width: 1100, height: 180 } }
        );

        await page.click('.InfoServiceTooltip');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('render when rasing will active in the future', async() => {
        await render(
            <Component
                {...getProps({
                    schedules: {
                        status: 'ENABLED',
                        scheduleOncePolicy: {
                            manual: {
                                items: [ {
                                    daysOfWeek: daysByInterval.weekends,
                                    hasBoughtProduct: true,
                                    startTime: '2020-05-22T21:00:00Z'
                                } ]
                            }
                        }
                    },
                    mapProducts: type => ({ status: type === 'rasing' ? 'active' : 'inactive ' })
                })}
            />,
            { viewport: { width: 1100, height: 180 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
