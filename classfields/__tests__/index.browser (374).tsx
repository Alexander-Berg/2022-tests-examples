import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IOfferContact } from 'realty-core/types/phones';
import { CommunicationChannels, IOfferCard } from 'realty-core/types/offerCard';

import { infinitePromise, rejectPromise } from 'view/react/libs/test-helpers';

import styles from '../styles.module.css';
import { OfferCardContactsContainer } from '../container';
import { OfferCardContactsDisplayMode } from '../types';

const Component: React.FC<{
    Gate?: Record<string, unknown>;
    offerId: string;
    offerPhones?: Record<number | string, IOfferContact[]>;
    view?: 'light' | 'dark';
    withChat?: boolean;
    displayMode?: OfferCardContactsDisplayMode;
}> = ({
    Gate,
    offerId,
    offerPhones = {},
    view = 'light',
    withChat,
    displayMode = OfferCardContactsDisplayMode.MULTILINE,
}) => (
    <AppProvider initialState={{ offerPhones, user: { isAuth: true, uid: '777' }, offerCard: {} }} Gate={Gate}>
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column' }}>
            <OfferCardContactsContainer
                offer={
                    {
                        offerId,
                        author: {
                            allowedCommunicationChannels: withChat ? [CommunicationChannels.COM_CHATS] : [],
                        },
                    } as IOfferCard
                }
                page="blank"
                pageType="blank"
                view={view}
                displayMode={displayMode}
                placement="somewhere"
            />
        </div>
    </AppProvider>
);

const renderComponent = (element: JSX.Element) => render(element, { viewport: { width: 400, height: 250 } });

describe('OfferCardContacts', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await renderComponent(<Component offerId="1" />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии с кнопкой чата', async () => {
        await renderComponent(<Component offerId="1" withChat />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном компактном состоянии', async () => {
        await renderComponent(<Component offerId="1" displayMode={OfferCardContactsDisplayMode.COMPACT} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном компактном состоянии с кнопкой чата', async () => {
        await renderComponent(<Component offerId="1" withChat displayMode={OfferCardContactsDisplayMode.COMPACT} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в одну строку с кнопкой чата', async () => {
        await renderComponent(<Component offerId="1" withChat displayMode={OfferCardContactsDisplayMode.INLINE} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в одну строку с кнопкой чата и отображённым телефоном', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: false,
                    phones: [{ phoneNumber: '+79991150830' }],
                },
            ],
        };

        await renderComponent(
            <Component
                offerId="123"
                offerPhones={offerPhones}
                withChat
                displayMode={OfferCardContactsDisplayMode.INLINE}
            />
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в одну строку с кнопкой чата и несколькими телефонами', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: false,
                    phones: [
                        { phoneNumber: '+79991150830' },
                        { phoneNumber: '+79991150831' },
                        { phoneNumber: '+79991150832' },
                    ],
                },
            ],
        };

        await renderComponent(
            <Component
                offerId="123"
                offerPhones={offerPhones}
                withChat
                displayMode={OfferCardContactsDisplayMode.INLINE}
            />
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии загрузки', async () => {
        const Gate = {
            get: infinitePromise(),
        };

        await renderComponent(<Component offerId="1" Gate={Gate} />);

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии загрузки с кнопкой чата', async () => {
        const Gate = {
            get: infinitePromise(),
        };

        await renderComponent(<Component offerId="1" Gate={Gate} withChat />);

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии ошибки', async () => {
        const Gate = {
            get: () => rejectPromise(),
        };

        await renderComponent(<Component offerId="1" Gate={Gate} />);

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии ошибки с кнопкой чата', async () => {
        const Gate = {
            get: () => rejectPromise(),
        };

        await renderComponent(<Component offerId="1" Gate={Gate} withChat />);

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('телефон', () => {
        it('один телефон', async () => {
            const offerPhones = {
                123: [
                    {
                        shouldShowRedirectIndicator: false,
                        withBilling: false,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
        it('два телефона', async () => {
            const offerPhones = {
                123: [
                    {
                        shouldShowRedirectIndicator: false,
                        withBilling: false,
                        phones: [{ phoneNumber: '+79992250430' }, { phoneNumber: '+79024869940' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
        it('четыре телефона', async () => {
            const offerPhones = {
                123: [
                    {
                        shouldShowRedirectIndicator: false,
                        withBilling: false,
                        phones: [
                            { phoneNumber: '+79992250430' },
                            { phoneNumber: '+79024869941' },
                            { phoneNumber: '+79111239923' },
                            { phoneNumber: '+79864569996' },
                        ],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
        it('четыре телефона и кнопка чата', async () => {
            const offerPhones = {
                123: [
                    {
                        shouldShowRedirectIndicator: false,
                        withBilling: false,
                        phones: [
                            { phoneNumber: '+79992250430' },
                            { phoneNumber: '+79024869941' },
                            { phoneNumber: '+79111239923' },
                            { phoneNumber: '+79864569996' },
                        ],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} withChat />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
        it('один телефон и индикатор редиректа + ховер по редиректу', async () => {
            const offerPhones = {
                123: [
                    {
                        shouldShowRedirectIndicator: true,
                        withBilling: false,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

            await page.hover(`.${styles.redirectIndicator}`);
            await page.waitFor(200);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
        it('один телефон и иконка с рекламой + ховер по рекламе', async () => {
            const offerPhones = {
                123: [
                    {
                        name: 'Агентство 47',
                        logo: 'https://realty.yandex.ru',
                        shouldShowRedirectIndicator: false,
                        withBilling: true,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

            await page.hover('.CardPhoneHint');

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
        it('несколько телефонов и индикатор редиректа', async () => {
            const offerPhones = {
                123: [
                    {
                        name: 'Агентство 47',
                        logo: 'https://realty.yandex.ru',
                        shouldShowRedirectIndicator: true,
                        withBilling: false,
                        phones: [
                            { phoneNumber: '+79992250430' },
                            { phoneNumber: '+79024869941' },
                            { phoneNumber: '+79111239923' },
                            { phoneNumber: '+79864569996' },
                        ],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
        it('два телефона и кнопка чата', async () => {
            const offerPhones = {
                123: [
                    {
                        shouldShowRedirectIndicator: false,
                        withBilling: false,
                        phones: [{ phoneNumber: '+79992250430' }, { phoneNumber: '+79024869940' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} withChat />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('один телефон и индикатор редиректа в одной линии', async () => {
            const offerPhones = {
                123: [
                    {
                        name: 'Агентство 47',
                        logo: 'https://realty.yandex.ru',
                        shouldShowRedirectIndicator: true,
                        withBilling: false,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
    });

    describe('темная тема', () => {
        it('рендерится в дефолтном состоянии с кнопкой чата', async () => {
            await renderComponent(<Component offerId="1" view="dark" />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рендерится в состоянии загрузки', async () => {
            const Gate = {
                get: infinitePromise(),
            };

            await renderComponent(<Component offerId="1" Gate={Gate} view="dark" />);

            await page.click('button');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рендерится в состоянии загрузки с кнопкой чата', async () => {
            const Gate = {
                get: infinitePromise(),
            };

            await renderComponent(<Component offerId="1" Gate={Gate} view="dark" withChat />);

            await page.click('button');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рендерится с телефоном', async () => {
            const offerPhones = {
                123: [
                    {
                        shouldShowRedirectIndicator: false,
                        withBilling: false,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} view="dark" />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рендерится с телефоном и кнопкой чата', async () => {
            const offerPhones = {
                123: [
                    {
                        shouldShowRedirectIndicator: false,
                        withBilling: false,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} view="dark" withChat />);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('один телефон, индикатор редиректа', async () => {
            const offerPhones = {
                123: [
                    {
                        name: 'Агентство 47',
                        logo: 'https://realty.yandex.ru',
                        shouldShowRedirectIndicator: true,
                        withBilling: false,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} view="dark" />);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('один телефон и иконка с рекламой + кнопка чата', async () => {
            const offerPhones = {
                123: [
                    {
                        name: 'Агентство 47',
                        logo: 'https://realty.yandex.ru',
                        shouldShowRedirectIndicator: false,
                        withBilling: true,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} view="dark" withChat />);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
    });
});
