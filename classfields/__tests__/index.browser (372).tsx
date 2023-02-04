import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardCheck } from '../';

import {
    yaArendaState,
    defaultState,
    withRentPledgeAndUtilitiesIncluded,
    withPriceRentPledgeAndUtilitiesNotIncluded,
    withoutRentPledgeAndUtilitiesNotIncluded,
} from './mocks';

describe('OfferCardCheck', () => {
    it('Активный офер', async () => {
        await render(
            <AppProvider initialState={defaultState()}>
                <OfferCardCheck />
            </AppProvider>,
            {
                viewport: { width: 550, height: 800 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('Итоговая цена', () => {
        it('есть залог, но в поле КУ пришло "Включены в стоимость"', async () => {
            await render(
                <AppProvider initialState={defaultState(withRentPledgeAndUtilitiesIncluded)}>
                    <OfferCardCheck />
                </AppProvider>,
                {
                    viewport: { width: 550, height: 800 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('есть залог (и его цена известна) или залога нет и КУ не включены', async () => {
            await render(
                <AppProvider initialState={defaultState(withPriceRentPledgeAndUtilitiesNotIncluded)}>
                    <OfferCardCheck />
                </AppProvider>,
                {
                    viewport: { width: 550, height: 800 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('залога нет и КУ не включены', async () => {
            await render(
                <AppProvider initialState={defaultState(withoutRentPledgeAndUtilitiesNotIncluded)}>
                    <OfferCardCheck />
                </AppProvider>,
                {
                    viewport: { width: 550, height: 800 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    it('Оффер от Яндекс.Аренда', async () => {
        await render(
            <AppProvider initialState={yaArendaState}>
                <OfferCardCheck />
            </AppProvider>,
            {
                viewport: { width: 550, height: 800 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
