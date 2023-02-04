import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IOfferContact } from 'realty-core/types/phones';
import { CommunicationChannels, IOfferCard } from 'realty-core/types/offerCard';

import { rejectPromise, infinitePromise } from 'view/react/libs/test-helpers';

import { OfferCardContacts } from '../';

import styles from '../styles.module.css';

const Component: React.FC<{
    Gate?: Record<string, unknown>;
    offerId: string;
    offerPhones?: Record<number | string, IOfferContact[]>;
    view?: 'light' | 'dark';
    withChat?: boolean;
}> = ({ Gate, offerId, offerPhones = {}, view = 'light', withChat }) => (
    <AppProvider initialState={{ offerPhones, user: { isAuth: true, uid: '777' } }} Gate={Gate}>
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column' }}>
            <OfferCardContacts
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
                placement="blank"
                eventPlace="heart"
                view={view}
            />
        </div>
    </AppProvider>
);

const renderComponent = (element: JSX.Element) => render(element, { viewport: { width: 400, height: 200 } });

describe('OldOfferCardContacts', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await renderComponent(<Component offerId="1" />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии с кнопкой чата', async () => {
        await renderComponent(<Component offerId="1" withChat />);

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

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рендерится в состоянии ошибки с кнопкой чата', async () => {
        const Gate = {
            get: () => rejectPromise(),
        };

        await renderComponent(<Component offerId="1" Gate={Gate} withChat />);

        await page.click('button');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
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
        it('один телефон, иконка с рекламой + ховер по рекламе', async () => {
            const offerPhones = {
                123: [
                    {
                        name: 'Агентство 47',
                        logo: 'https://realty.yandex.ru',
                        withBilling: true,
                        shouldShowRedirectIndicator: false,
                        phones: [{ phoneNumber: '+79991150830' }],
                    },
                ],
            };

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

            await page.hover('.CardPhoneHint');

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
        it('несколько телефонов и иконка с рекламой', async () => {
            const offerPhones = {
                123: [
                    {
                        name: 'Агентство 47',
                        logo: 'https://realty.yandex.ru',
                        withBilling: true,
                        shouldShowRedirectIndicator: false,
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
    });

    describe('темная тема', () => {
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

        it('один телефон и иконка с рекламой', async () => {
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

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} view="dark" />);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('один телефон, индикатор редиректа + кнопка чата', async () => {
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

            await renderComponent(<Component offerId="123" offerPhones={offerPhones} view="dark" withChat />);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });
    });
});
