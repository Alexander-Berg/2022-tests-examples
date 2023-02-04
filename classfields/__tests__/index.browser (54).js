import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';
import cloneDeep from 'lodash/cloneDeep';

import dayjs from '@realty-front/dayjs';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import { getPlacementMessages } from 'view/lib/offer-helpers/messages';

import MessagePanel from '../index';

const WIDTH = 1000;
const HEIGHT = 150;

const offer = {
    status: 'active',
    services: {
        placement: {
            end: 0,
            renewal: {},
            isPaymentPending: false
        }
    },
    placement: {
        paymentRequired: {
            paid: true
        }
    }
};

const params = {
    offer,
    timeDelta: 0,
    userType: 'OWNER',
    isNotEnoughFunds: false
};

const getParams = customData => {
    return merge(cloneDeep(params), customData);
};

const Component = props => (
    <AppProvider>
        <MessagePanel banReasons={[]} {...props} />
    </AppProvider>
);

describe('Custom messages. Placement.', () => {
    it('2 days remaning', async() => {
        const customMessages = getPlacementMessages(getParams({
            offer: {
                services: {
                    placement: {
                        end: Number(dayjs().add(49, 'hour'))
                    }
                }
            }
        }));

        await render(
            <Component customMessages={customMessages} />,
            { viewport: { width: WIDTH, height: HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('1 days remaning', async() => {
        const customMessages = getPlacementMessages(getParams({
            offer: {
                services: {
                    placement: {
                        end: Number(dayjs().add(25, 'hour'))
                    }
                }
            }
        }));

        await render(
            <Component customMessages={customMessages} />,
            { viewport: { width: WIDTH, height: HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('0 days remaning', async() => {
        const customMessages = getPlacementMessages(getParams({
            offer: {
                services: {
                    placement: {
                        end: Number(dayjs().add(23, 'hour'))
                    }
                }
            }
        }));

        await render(
            <Component customMessages={customMessages} />,
            { viewport: { width: WIDTH, height: HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('2 days remaning isNotEnoughFunds', async() => {
        const customMessages = getPlacementMessages(getParams({
            offer: {
                services: {
                    placement: {
                        end: Number(dayjs().add(49, 'hour'))
                    }
                }
            },
            isNotEnoughFunds: true
        }));

        await render(
            <Component customMessages={customMessages} />,
            { viewport: { width: WIDTH, height: HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('renewal error', async() => {
        const customMessages = getPlacementMessages(getParams({
            offer: {
                services: {
                    placement: {
                        end: Number(dayjs().add(49, 'hour')),
                        renewal: {
                            status: 'INACTIVE',
                            error: 'INSUFFICIENT_FUNDS'
                        }
                    }
                }
            }
        }));

        await render(
            <Component customMessages={customMessages} />,
            { viewport: { width: WIDTH, height: HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Agent payment in process', async() => {
        const customMessages = getPlacementMessages(getParams({
            offer: {
                services: {
                    placement: {
                        end: Number(dayjs().add(49, 'hour')),
                        isPaymentPending: true
                    }
                }
            },
            userType: 'AGENT'
        }));

        await render(
            <Component customMessages={customMessages} />,
            { viewport: { width: WIDTH, height: HEIGHT } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
