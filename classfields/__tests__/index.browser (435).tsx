import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { LastApartmentsPromo, SHOW_TIMEOUT } from '../';

import { siteCard } from './mocks';

describe('LastApartmentsPromo', () => {
    it('Рендерится корректно (модалка)', async () => {
        await render(
            <AppProvider disableSetTimeoutDelay experiments={['REALTYFRONT-12098_popup_last_apartments_phone_show']}>
                <LastApartmentsPromo card={siteCard} page="newbuilding" />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится корректно (попап)', async () => {
        await render(
            <AppProvider fakeTimers={{}} experiments={['REALTYFRONT-12098_popup_last_apartments']}>
                <LastApartmentsPromo card={siteCard} page="newbuilding" />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );
        await customPage.tick(SHOW_TIMEOUT);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
