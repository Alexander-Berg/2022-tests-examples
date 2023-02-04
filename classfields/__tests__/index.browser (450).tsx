import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { IOfferContact } from 'realty-core/types/phones';
import { IOfferCard } from 'realty-core/types/offerCard';

import { infinitePromise, rejectPromise } from 'view/react/libs/test-helpers';

import { OfferPhoneButton } from '../';

const Component: React.FC<{
    Gate?: Record<string, unknown>;
    offerId: string;
    offerPhones?: Record<number | string, IOfferContact[]>;
}> = ({ Gate, offerId, offerPhones = {} }) => (
    <AppProvider initialState={{ offerPhones }} Gate={Gate}>
        {/* eslint-disable-next-line @typescript-eslint/ban-ts-comment */}
        {/* @ts-ignore */}
        <OfferPhoneButton offer={{ offerId } as IOfferCard} size="xl" showText="+7 (×××) ×××-××-××" />
    </AppProvider>
);

const renderComponent = (element: JSX.Element) => render(element, { viewport: { width: 400, height: 200 } });

describe('OfferCardButton', () => {
    it('рендерится кнопка с текстом', async () => {
        await renderComponent(<Component offerId="123" />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится c одним телефоном', async () => {
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

    it('рендерится c несколькими телефонами', async () => {
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

        await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится c одним телефоном (с биллингом)', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: true,
                    phones: [{ phoneNumber: '+79991150830' }],
                },
            ],
        };

        await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится c несколькими телефонами (с биллингом)', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: true,
                    phones: [
                        { phoneNumber: '+79991150830' },
                        { phoneNumber: '+79991150831' },
                        { phoneNumber: '+79991150832' },
                    ],
                },
            ],
        };

        await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится c одним телефоном (с индикатором)', async () => {
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

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится c несколькими телефонами (с индикатором)', async () => {
        const offerPhones = {
            123: [
                {
                    shouldShowRedirectIndicator: true,
                    withBilling: false,
                    phones: [
                        { phoneNumber: '+79991150830' },
                        { phoneNumber: '+79991150831' },
                        { phoneNumber: '+79991150832' },
                    ],
                },
            ],
        };

        await renderComponent(<Component offerId="123" offerPhones={offerPhones} />);

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

    it('рендерится в состоянии ошибки', async () => {
        const Gate = {
            get: () => rejectPromise(),
        };

        await renderComponent(<Component offerId="1" Gate={Gate} />);

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
