import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';
import { IStore } from 'view/common/reducers';

import { Tariffs } from '..';

import { mocks } from './mocks';

const Component = ({ store }: { store: IStore }) => (
    <AppProvider initialState={store} fakeTimers={{ now: new Date('2022-06-23T09:00:00.111Z').getTime() }}>
        <Tariffs />
    </AppProvider>
);

const SCREEN_WIDTHS = [1280, 1000];

describe('Tariffs', () => {
    SCREEN_WIDTHS.forEach((width) => {
        it(`[${width}px] Рисует активный тариф (Минимальный/Оплата за звонки)`, async () => {
            await render(<Component store={mocks.callsMinimum} />, {
                viewport: { width, height: 900 },
            });
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`[${width}px] Рисует активный тариф (Максимальный/Оплата за размещение)`, async () => {
            await render(<Component store={mocks.listingMaximum} />, {
                viewport: { width, height: 900 },
            });
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    SCREEN_WIDTHS.forEach((width) => {
        [mocks.pending1, mocks.pending2].forEach((mock, i) => {
            it(`[${width}px] Рисует загрузку ${i + 1}`, async () => {
                await render(<Component store={mock} />, { viewport: { width, height: 900 } });
                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    SCREEN_WIDTHS.forEach((width) => {
        it(`[${width}px] Рисует с запланированным временем`, async () => {
            await render(<Component store={mocks.plannedTime} />, { viewport: { width, height: 900 } });
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`[${width}px] Рисует с запланированным временем (кейс с 0)`, async () => {
            await render(<Component store={mocks.zeroPlannedTime} />, { viewport: { width, height: 900 } });
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    it('Рисует ошибку', async () => {
        await render(<Component store={mocks.error} />, { viewport: { width: 1000, height: 900 } });
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
