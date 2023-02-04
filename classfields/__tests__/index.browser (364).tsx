import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import {
    developerFullInfo,
    agencyFullInfo,
    developersOfferMock,
    agencyOfferMock,
    agentOfferMock,
    ownerOfferMock,
} from 'view/react/deskpad/components/cards/OfferCard/__tests__/stubs/author';

import { OfferCardAuthorInfoData } from '..';

const baseInitialState = {
    geo: {
        rgid: 426676,
    },
};

describe('OfferCardAuthorInfoData', () => {
    it('рендерится для застройщика', async () => {
        await render(
            <AppProvider initialState={{ ...baseInitialState, offerCard: { authorStats: developerFullInfo } }}>
                <OfferCardAuthorInfoData
                    authorCategory="DEVELOPER"
                    uid="someUidMock"
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-ignore
                    offer={developersOfferMock}
                    isOwner={false}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ренерится для агенства', async () => {
        await render(
            <AppProvider initialState={{ ...baseInitialState, offerCard: { authorStats: agencyFullInfo } }}>
                <OfferCardAuthorInfoData
                    authorCategory="AGENCY"
                    uid="someUidMock"
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-ignore
                    offer={agencyOfferMock}
                    isOwner={false}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится для частного агента', async () => {
        await render(
            <AppProvider initialState={{ baseInitialState, offerCard: { ...agentOfferMock } }}>
                <OfferCardAuthorInfoData
                    authorCategory="AGENT"
                    uid="someUidMock"
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-ignore
                    offer={agentOfferMock}
                    isOwner={false}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится для частного пользователя (от собственника)', async () => {
        await render(
            <AppProvider initialState={{ baseInitialState, offerCard: { ...ownerOfferMock } }}>
                <OfferCardAuthorInfoData
                    authorCategory="OWNER"
                    uid="someUidMock"
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-ignore
                    offer={ownerOfferMock}
                    isOwner={false}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
