import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { ChooseApartmentPromo } from '../';

import { siteCard, offerCard } from './mocks';

const EXPERIMENTS = ['REALTYFRONT-12096_popup_phone_show'];
const INITIAL_STATE = { offerPhones: [] };

describe('ChooseApartmentPromo', () => {
    it('Рендерится для новостройки', async () => {
        await render(
            <AppProvider disableSetTimeoutDelay experiments={EXPERIMENTS} initialState={INITIAL_STATE}>
                <ChooseApartmentPromo siteCard={siteCard} page="newbuilding" />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится для оффера', async () => {
        await render(
            <AppProvider disableSetTimeoutDelay experiments={EXPERIMENTS} initialState={INITIAL_STATE}>
                <ChooseApartmentPromo siteCard={siteCard} offerCard={offerCard} page="offer" />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
