import React from 'react';
import { render } from 'jest-puppeteer-react';

import { TrafficSourceInfo } from '@vertis/schema-registry/ts-types/realty/event/model';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { CommunicationChannels } from 'realty-core/types/offerCard';

import {
    developerFullInfo,
    agencyFullInfo,
    developersOfferMock,
    agencyOfferMock,
    agentOfferMock,
    ownerOfferMock,
} from 'view/react/deskpad/components/cards/OfferCard/__tests__/stubs/author';

import { OfferCardAuthorInfo } from '..';

const baseInitialState = {
    geo: {
        rgid: 426676,
    },
};

describe('OfferCardAuthorInfo', () => {
    it('рендерится для застройщика', async () => {
        await render(
            <AppProvider
                initialState={{
                    ...baseInitialState,
                    offerCard: {
                        authorStats: developerFullInfo,
                    },
                    offerPhones: {
                        1234567890: [{ phones: [] }],
                    },
                }}
            >
                <OfferCardAuthorInfo
                    offer={{
                        ...developersOfferMock,
                        uid: 'someUidMock',
                        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                        // @ts-ignore
                        author: {
                            ...developersOfferMock.author,
                            allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
                        },
                        backCallTrafficInfo: {} as TrafficSourceInfo,
                    }}
                    isOwner={false}
                    placement="offer_card_test"
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится без контролов для застройщика-автора', async () => {
        await render(
            <AppProvider
                initialState={{
                    ...baseInitialState,
                    offerCard: {
                        authorStats: developerFullInfo,
                    },
                    offerPhones: {
                        1234567890: [{ phones: [] }],
                    },
                }}
            >
                <OfferCardAuthorInfo
                    offer={{
                        ...developersOfferMock,
                        uid: 'someUidMock',
                        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                        // @ts-ignore
                        author: {
                            ...developersOfferMock.author,
                            allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
                        },
                        backCallTrafficInfo: {} as TrafficSourceInfo,
                    }}
                    isOwner={true}
                    placement="offer_card_test"
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится для агенства', async () => {
        await render(
            <AppProvider
                initialState={{
                    ...baseInitialState,
                    offerCard: {
                        authorStats: agencyFullInfo,
                    },
                    offerPhones: {
                        1234567890: [{ phones: [] }],
                    },
                }}
            >
                <OfferCardAuthorInfo
                    offer={{
                        ...agencyOfferMock,
                        uid: 'someUidMock',
                        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                        // @ts-ignore
                        author: {
                            ...agencyOfferMock.author,
                            allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
                        },
                        backCallTrafficInfo: {} as TrafficSourceInfo,
                    }}
                    isOwner={false}
                    placement="offer_card_test"
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится без контролов для агенства-автора', async () => {
        await render(
            <AppProvider
                initialState={{
                    ...baseInitialState,
                    offerCard: {
                        authorStats: agencyFullInfo,
                    },
                    offerPhones: {
                        1234567890: [{ phones: [] }],
                    },
                }}
            >
                <OfferCardAuthorInfo
                    offer={{
                        ...agencyOfferMock,
                        uid: 'someUidMock',
                        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                        // @ts-ignore
                        author: {
                            ...agencyOfferMock.author,
                            allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
                        },
                        backCallTrafficInfo: {} as TrafficSourceInfo,
                    }}
                    isOwner={true}
                    placement="offer_card_test"
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится для частного агента', async () => {
        await render(
            <AppProvider
                initialState={{
                    baseInitialState,
                    offerCard: { ...agentOfferMock },
                    offerPhones: {
                        1234567890: [{ phones: [] }],
                    },
                }}
            >
                <OfferCardAuthorInfo
                    offer={{
                        ...agentOfferMock,
                        uid: 'someUidMock',
                        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                        // @ts-ignore
                        author: {
                            ...agentOfferMock.author,
                            allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
                        },
                    }}
                    isOwner={false}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится для частного пользователя (от собственника)', async () => {
        await render(
            <AppProvider
                initialState={{
                    baseInitialState,
                    offerCard: { ...ownerOfferMock },
                    offerPhones: {
                        1234567890: [{ phones: [] }],
                    },
                }}
            >
                <OfferCardAuthorInfo
                    offer={{
                        ...ownerOfferMock,
                        uid: 'someUidMock',
                        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                        // @ts-ignore
                        author: {
                            ...ownerOfferMock.author,
                            allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
                        },
                    }}
                    isOwner={false}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('не рендерится для собственника-автора', async () => {
        await render(
            <AppProvider
                initialState={{
                    baseInitialState,
                    offerCard: { ...ownerOfferMock },
                    offerPhones: {
                        1234567890: [{ phones: [] }],
                    },
                }}
            >
                <OfferCardAuthorInfo
                    offer={{
                        ...ownerOfferMock,
                        uid: 'someUidMock',
                        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                        // @ts-ignore
                        author: {
                            ...ownerOfferMock.author,
                            allowedCommunicationChannels: [CommunicationChannels.COM_CHATS],
                        },
                    }}
                    isOwner={true}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
