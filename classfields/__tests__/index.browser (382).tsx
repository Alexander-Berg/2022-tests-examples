import React from 'react';
import { render } from 'jest-puppeteer-react';

import { TrafficSourceInfo } from '@vertis/schema-registry/ts-types/realty/event/model';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { CommunicationChannels, IOfferCard } from 'realty-core/types/offerCard';

import { reducer as rootReducer } from 'view/react/deskpad/reducers/roots/offer';
import metroListStyles from 'view/react/deskpad/components/CardMetroList/styles.module.css';
import offerCardContactsStyles from 'view/react/deskpad/components/OldOfferCardContacts/styles.module.css';

import { OfferGallerySnippet, IOfferGallerySnippetProps } from '../index';

import {
    offer,
    defaultState,
    stateWithPhone,
    stateWithFavorite,
    GatePending,
    PhoneGateSuccess,
    TwoPhonesGateSuccess,
    yandexRentOffer,
    emptyMetroListOffer,
    backCallSuccessState,
    PhoneGateWithHintInfo,
} from './mocks';

const containers = {
    1200: { width: 360 },
};

const renderComponent = ({
    width,
    props = {},
    state = defaultState,
    Gate,
    withChat,
    customAddress,
}: {
    width: number;
    state?: Record<string, unknown>;
    Gate?: {
        get?(path?: string, args?: Record<string, unknown>): Promise<unknown>;
        create?(path?: string, args?: Record<string, unknown>): Promise<unknown>;
    };
    withChat?: boolean;
    customAddress?: string;
    props?: Partial<IOfferGallerySnippetProps>;
}) => {
    const offerCard = {
        ...offer,
        author: {
            ...offer.author,
            allowedCommunicationChannels: withChat ? [CommunicationChannels.COM_CHATS] : [],
        },
        location: {
            ...offer.location,
            address: customAddress ?? offer.location?.address,
        },
        building: ({ siteId: '123' } as unknown) as IOfferCard['building'],
        backCallTrafficInfo: {} as TrafficSourceInfo,
    } as IOfferCard;

    return render(
        <AppProvider initialState={state} Gate={Gate} rootReducer={rootReducer} context={{ link: () => 'testLink' }}>
            <div style={containers[width]}>
                <OfferGallerySnippet withActions item={offerCard} {...props} />
            </div>
        </AppProvider>,
        { viewport: { width, height: 500 } }
    );
};

const screens = [1200, 1100];

describe('OfferGallerySnippet', () => {
    it.each(screens)('???????????????????? ?? ?????????????????? ?????????????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????????? ?? ?????????????????? ?????????????????? ?? ?????????????? ???????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, withChat: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????????? c ?????????????????????? ?????????????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, state: stateWithPhone });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????????? ?? ?????????????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, state: stateWithFavorite });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????? ?????????????????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, Gate: GatePending });

        await page.click(`.${offerCardContactsStyles.defaultPhoneButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????? ?????????????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, Gate: PhoneGateSuccess });

        await page.click(`.${offerCardContactsStyles.defaultPhoneButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????? ?? ?????????????? ???????? ?????????????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, Gate: PhoneGateSuccess, withChat: true });

        await page.click(`.${offerCardContactsStyles.defaultPhoneButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????? ?????????????????? (2 ????????????????) ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, Gate: TwoPhonesGateSuccess });

        await page.click(`.${offerCardContactsStyles.defaultPhoneButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????? ?????????????????? (2 ????????????????) ?? ?????????????? ???????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, Gate: TwoPhonesGateSuccess, withChat: true });

        await page.click(`.${offerCardContactsStyles.defaultPhoneButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????? ?????????????????? c ???????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, Gate: PhoneGateWithHintInfo });

        await page.click(`.${offerCardContactsStyles.defaultPhoneButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????????? ?????????? ???????????? ???????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({
            width: key,
            Gate: TwoPhonesGateSuccess,
            props: { item: yandexRentOffer as IOfferCard },
        });

        await page.click(`.${offerCardContactsStyles.defaultPhoneButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('?????????????????? ???????????? ??????????', async () => {
        await renderComponent({
            width: 1200,
        });

        await page.click(`.${metroListStyles.showBtn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????????? ?? ???????????? ?????????????? ?????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({
            width: key,
            Gate: TwoPhonesGateSuccess,
            props: { item: emptyMetroListOffer as IOfferCard },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????? ???????????????? ?????????? ?????????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({ width: key, state: backCallSuccessState });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(screens)('???????????????????? ?????????????????? ?? ?????????????????? ???????????????? ???????????? ?? ???????????????????? %d', async (key) => {
        await renderComponent({
            width: key,
            customAddress: '??????????-??????????????????, ???????????????????? ??????????, ?????????????????????????? ?????????? ????????????????, ?????????? ???????????????? ?????? ????????',
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
