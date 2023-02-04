import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IOfferSnippet, IRelatedFastlink } from 'realty-core/types/offerSnippet';

import { OfferCardSlider } from '../';

import {
    getOffers,
    getInitialState,
    onIntersection,
    getOffersWithSmallAddress,
    getOffersWithLongAddress,
    getRentOffers,
    getOffersWithDeactivated,
    fastlinksMock,
} from './mocks';
interface IComponentProps {
    withPhone?: boolean;
    offers?: IOfferSnippet[];
    fastlinks?: IRelatedFastlink[];
}

const renderComponent = ({ withPhone = false, offers, fastlinks = [] }: IComponentProps = {}) =>
    render(
        <AppProvider initialState={getInitialState()}>
            <OfferCardSlider
                title="Похожие квартиры"
                offers={offers || getOffers()}
                withPhone={withPhone}
                onIntersection={onIntersection}
                fastlinks={fastlinks}
            />
        </AppProvider>,
        { viewport: { width: 900, height: 400 } }
    );

describe('OfferCardSlider', () => {
    it('рисует блок', async () => {
        await renderComponent();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с телефонами', async () => {
        await renderComponent({ withPhone: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с телефонами + ховер по 1 сниппету', async () => {
        await renderComponent({ withPhone: true });

        await page.hover('[class*="snippet"]');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует блок + скролл', async () => {
        await renderComponent();

        await page.click('[class*="nextButton"]');

        await page.waitFor(500);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок + последний скролл', async () => {
        await renderComponent();

        await page.click('[class*="nextButton"]');
        await page.click('[class*="nextButton"]');

        await page.waitFor(1000);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с адресами, занимающими две и одну строку', async () => {
        await renderComponent({ offers: getOffersWithSmallAddress() });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с адресами, занимающими две и одну строку с телефоном', async () => {
        await renderComponent({ offers: getOffersWithSmallAddress(), withPhone: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с оффером с длинным адресом', async () => {
        await renderComponent({ offers: getOffersWithLongAddress() });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с оффером с длинным адресом с телефоном', async () => {
        await renderComponent({ offers: getOffersWithLongAddress(), withPhone: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с арендой', async () => {
        await renderComponent({ offers: getRentOffers() });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с арендой и телефоном', async () => {
        await renderComponent({ offers: getRentOffers(), withPhone: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('у неактивного оффера рисуется предупреждение о том, что он не активен', async () => {
        await renderComponent({ offers: getOffersWithDeactivated(), withPhone: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок с фастлинками, когда есть похожие объявления', async () => {
        await renderComponent({ offers: getOffersWithDeactivated(), withPhone: true, fastlinks: fastlinksMock });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
