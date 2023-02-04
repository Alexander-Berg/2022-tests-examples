import React from 'react';
import { advanceTo } from 'jest-date-mock';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AuthorCategoryTypes, IOfferCard } from 'realty-core/types/offerCard';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferCardAuthorBadge } from '../index';

import { ownerOffer } from './stubs';

advanceTo(new Date('2020-08-14T04:20:00.111Z'));

const renderOptions = { viewport: { width: 350, height: 150 } };

const getStore = () => ({
    user: {
        avatarHost: '/',
        creationDate: '2019-04-01T03:00:00.111Z',
    },
    geo: {
        rgid: 0,
    },
    config: {
        yaArendaUrl: 'https://arenda.test.vertis.yandex.ru/',
    },
});

const Component: React.FC<{ offer: IOfferCard; isOwner?: boolean }> = ({ offer, isOwner = false }) => (
    <AppProvider initialState={getStore()}>
        <OfferCardAuthorBadge offer={offer} isOwner={isOwner} />
    </AppProvider>
);

describe('OfferCardAuthorBadge', () => {
    describe('собственник', () => {
        it('без пессимицации, НЕ проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        trustedOfferInfo: {
                            isFullTrustedOwner: false,
                            isCadastrPersonMatched: false,
                            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('без пессимицации, проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        trustedOfferInfo: {
                            isFullTrustedOwner: true,
                            isCadastrPersonMatched: true,
                            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('с пессимизацией, НЕ проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        socialPessimization: true,
                        trustedOfferInfo: {
                            isFullTrustedOwner: false,
                            isCadastrPersonMatched: false,
                            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('с пессимизацией, проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        socialPessimization: true,
                        trustedOfferInfo: {
                            isFullTrustedOwner: true,
                            isCadastrPersonMatched: true,
                            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка автора, без пессимизации, проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        trustedOfferInfo: {
                            isFullTrustedOwner: true,
                            isCadastrPersonMatched: true,
                            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                        },
                    }}
                    isOwner
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка автора, без пессимизации, НЕ проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        trustedOfferInfo: {
                            isFullTrustedOwner: false,
                            isCadastrPersonMatched: false,
                            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                        },
                    }}
                    isOwner
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка автора, с пессимизацией, проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        socialPessimization: true,
                        trustedOfferInfo: {
                            isFullTrustedOwner: true,
                            isCadastrPersonMatched: true,
                            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                        },
                    }}
                    isOwner
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('карточка автора, с пессимизацией, НЕ проверенный юзер', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        socialPessimization: true,
                        trustedOfferInfo: {
                            isFullTrustedOwner: false,
                            isCadastrPersonMatched: false,
                            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                        },
                    }}
                    isOwner
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('сервис яндекса', () => {
        it('яндекс аренда', async () => {
            await render(
                <Component
                    offer={{
                        ...ownerOffer,
                        yandexRent: true,
                        author: {
                            ...ownerOffer.author,
                            category: AuthorCategoryTypes.AGENCY,
                            agentName: 'Яндекс.Аренда',
                        },
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
