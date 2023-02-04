import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import {
    developerFullInfo,
    developerInfoWithoutSomeObjects,
    agencyFullInfo,
} from 'view/react/deskpad/components/cards/OfferCard/__tests__/stubs/author';

import { OfferCardAuthorStats } from '..';

const rgid = 426676;

describe('OfferCardAuthorStats', () => {
    it('рендерится для застройщика', async () => {
        await render(
            <AppProvider>
                <OfferCardAuthorStats
                    authorCategory="DEVELOPER"
                    uid="someUidMock"
                    authorStats={developerFullInfo}
                    rgid={rgid}
                    yaArendaUrl={'https://arenda.yandex.ru/'}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ренерится застройщика с частью бейджей', async () => {
        await render(
            <AppProvider>
                <OfferCardAuthorStats
                    authorCategory="DEVELOPER"
                    uid="someUidMock"
                    authorStats={developerInfoWithoutSomeObjects}
                    rgid={rgid}
                    yaArendaUrl={'https://arenda.yandex.ru/'}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится для агенства', async () => {
        await render(
            <AppProvider>
                <OfferCardAuthorStats
                    authorCategory="AGENCY"
                    uid="someUidMock"
                    authorStats={agencyFullInfo}
                    rgid={rgid}
                    yaArendaUrl={'https://arenda.yandex.ru/'}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
