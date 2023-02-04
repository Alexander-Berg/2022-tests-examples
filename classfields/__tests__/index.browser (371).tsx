import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardChatPresetsContainer } from '../container';

import { stateWithManyPresets, disabledOffer, newbuildingOffer, rentOffer, stateWithCouplePresets } from './mocks';

describe('OfferCardChatPresets', () => {
    it('Не рисуем пресеты чатов у неактивного оффера', async () => {
        await render(
            <AppProvider initialState={stateWithCouplePresets}>
                <OfferCardChatPresetsContainer offer={disabledOffer} />
            </AppProvider>,
            {
                viewport: { width: 400, height: 100 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Немного пресетов у новостроечного оффера', async () => {
        await render(
            <AppProvider initialState={stateWithCouplePresets}>
                <OfferCardChatPresetsContainer offer={newbuildingOffer} />
            </AppProvider>,
            {
                viewport: { width: 400, height: 200 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Много пресетов у оффера аренды', async () => {
        await render(
            <AppProvider initialState={stateWithManyPresets}>
                <OfferCardChatPresetsContainer offer={rentOffer} />
            </AppProvider>,
            {
                viewport: { width: 400, height: 200 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
