import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IOfferContact } from 'realty-core/types/phones';
import { CommunicationChannels, IOfferCard } from 'realty-core/types/offerCard';

import { infinitePromise, rejectPromise } from 'view/react/libs/test-helpers';

import styles from '../styles.module.css';
import { OfferCardAuthorInfoContacts } from '..';

const Component: React.FC<{
    Gate?: Record<string, unknown>;
    offerId: string;
    offerPhones?: Record<number | string, IOfferContact[]>;
    view?: 'light' | 'dark';
    withChat?: boolean;
    withCallback?: boolean;
    backCallData?: Record<string, string | boolean>;
}> = ({ Gate, offerId, offerPhones = {}, view = 'light', withChat, withCallback }) => (
    <AppProvider
        initialState={{
            offerPhones,
            user: { isAuth: true, uid: '777', defaultPhone: '+79991234567' },
        }}
        Gate={Gate}
    >
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column' }}>
            <OfferCardAuthorInfoContacts
                offer={
                    {
                        offerId,
                        author: {
                            allowedCommunicationChannels: withChat ? [CommunicationChannels.COM_CHATS] : [],
                        },
                        backCallTrafficInfo: withCallback ? {} : undefined,
                    } as IOfferCard
                }
                page="blank"
                pageType="blank"
                view={view}
                placement="somewhere"
            />
        </div>
    </AppProvider>
);

const renderComponent = (element: JSX.Element) => render(element, { viewport: { width: 1000, height: 250 } });

describe('OfferCardAuthorInfoContacts', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await renderComponent(<Component offerId="1" />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии с кнопкой чата', async () => {
        await renderComponent(<Component offerId="1" withChat />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии с кнопками чата и обратного звонка', async () => {
        await renderComponent(<Component offerId="1" withChat withCallback />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с кнопкой чата и отображённым телефоном', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: false,
                    phones: [{ phoneNumber: '+79991150830' }],
                },
            ],
        };

        await renderComponent(<Component offerId="123" offerPhones={offerPhones} withChat />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с кнопкой чата и несколькими телефонами', async () => {
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

        await renderComponent(<Component offerId="123" offerPhones={offerPhones} withChat />);

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

    it('рендерит один телефон и индикатор редиректа + ховер по редиректу', async () => {
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

    it('рендерит один телефон и иконку с рекламой + ховер по рекламе', async () => {
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
});
