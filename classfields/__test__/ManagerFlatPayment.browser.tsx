import React, { ComponentProps } from 'react';
import { render } from 'jest-puppeteer-react';

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

describe('ManagerFlatPayment', () => {
    describe('Если нет ФИО юзера показывается его ID', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.noUserFioPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Данные юзера в договоре отличаются от личных данных', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.differentUserDataPayment} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Скелетон', () => {
        viewports.forEach((option) => {
            it(`${option.viewport.width}px`, async () => {
                await render(<Component store={stubs.skeletonStore} />, option);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
