import { render } from 'jest-puppeteer-react';
import React, { ComponentProps } from 'react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';

import * as stubs from './stubs/payment';

import { viewports, FlatPayment } from './common';

const Component: React.ComponentType<ComponentProps<typeof FlatPayment>> = (props) => {
    return (
        <AppProvider
            fakeTimers={{
                now: new Date('2021-11-12T03:00:00.111Z').getTime(),
            }}
        >
            <FlatPayment {...props} />
        </AppProvider>
    );
};

describe('ManagerFlatPaymentStatus', () => {
    describe('Unknown', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.unknownPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Future payment', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.futurePaymentPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Ready to pay', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.readyToPayPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Today', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.todayPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Outdated', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.outdatedPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Paid by tenant', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.paidByTenantPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Paid by tenant holding', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.paidByTenantHoldingPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Paid by tenant by phone', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.paidByTenantByPhonePayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Payout retries', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.payoutRetriesPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Retries limit reached', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.retriesLimitReachedPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Bound owner card is absent', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.boundOwnerCardIsAbsentPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Bound owner card is inactive', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.boundOwnerCardIsInactivePayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Bound owner card is not the only', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.boundOwnerCardIsNotTheOnlyPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Paid to owner on card', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.paidToOwnerOnCardPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Paid to owner on requisites', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.paidToOwnerOnRequisitesPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
