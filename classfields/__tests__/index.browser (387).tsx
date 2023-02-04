import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import OfferCardOwnerBlockContainer from '../container';

import { offer, stateWithoutVas, stateWithVas } from './mocks';

describe('OfferCardOwnerBlock', () => {
    it('Отрисовка без vas-ов', async () => {
        await render(
            <AppProvider initialState={stateWithoutVas}>
                <OfferCardOwnerBlockContainer offer={offer} />
            </AppProvider>,
            {
                viewport: { width: 900, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с vas-ми', async () => {
        await render(
            <AppProvider initialState={stateWithVas}>
                <OfferCardOwnerBlockContainer offer={offer} />
            </AppProvider>,
            {
                viewport: { width: 1000, height: 800 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
