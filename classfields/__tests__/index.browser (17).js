import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import Link from 'vertis-react/components/Link';

import { renderOffersBalloon, renderSiteBalloon } from '../';

import mocks, { defaultSite, siteWithoutRoomsInfo, siteWithOneRoom } from './mocks';

const Component = ({
    offer,
    otherOffersLinkComponent,
    withTail
}) => (
    <div
        dangerouslySetInnerHTML={
            { __html: renderOffersBalloon({
                link: () => {},
                offer,
                otherOffersLinkComponent,
                withTail
            }) }
        }
    />
);

describe('renderOffersBalloon', () => {
    it('correct draw preloader balloon', async() => {
        await render(
            <Component />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with apartment', async() => {
        await render(
            <Component offer={mocks.apartment} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with rooms', async() => {
        await render(
            <Component offer={mocks.room} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with house', async() => {
        await render(
            <Component offer={mocks.house} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with lot', async() => {
        await render(
            <Component offer={mocks.lot} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with garage', async() => {
        await render(
            <Component offer={mocks.garage} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with commercial land', async() => {
        await render(
            <Component offer={mocks.commercialLand} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with commercial business', async() => {
        await render(
            <Component offer={mocks.commercialBusiness} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with rent apartment', async() => {
        await render(
            <Component offer={mocks.rentApartment} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with rent room', async() => {
        await render(
            <Component offer={mocks.rentRoom} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with apartment without floors', async() => {
        await render(
            <Component offer={mocks.withoutFloors} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with apartment without rooms', async() => {
        await render(
            <Component offer={mocks.withoutRooms} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with apartment without area', async() => {
        await render(
            <Component offer={mocks.withoutArea} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with image', async() => {
        await render(
            <Component offer={mocks.withImage} />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with other offers link with room preview', async() => {
        await render(
            <Component
                offer={mocks.manyOffersRoom}
                otherOffersLinkComponent={
                    ({ className }) => (
                        <Link className={className} size="xs" url="123">Показать еще 123 объявлений</Link>
                    )
                }
            />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with other offers link with commercial preview', async() => {
        await render(
            <Component
                offer={mocks.manyOffersCommercialLand}
                otherOffersLinkComponent={
                    ({ className }) => (
                        <Link className={className} size="xs" url="123">Показать еще 123 объявлений</Link>
                    )
                }
            />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with other offers link', async() => {
        await render(
            <Component
                offer={mocks.manyOffers}
                otherOffersLinkComponent={
                    ({ className }) => (
                        <Link className={className} size="xs" url="123">Показать еще 123 объявлений</Link>
                    )
                }
            />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw balloon with tail', async() => {
        await render(
            <Component offer={mocks.apartment} withTail />,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

const SiteComponent = props => (
    <div
        dangerouslySetInnerHTML={
            { __html: renderSiteBalloon({
                link: () => {},
                ...props
            }) }
        }
    />
);

describe('renderSiteBalloon', () => {
    it('correct draw default', async() => {
        await render(<SiteComponent site={defaultSite} />, { viewport: { width: 300, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw with one room allowed', async() => {
        await render(<SiteComponent site={siteWithOneRoom} />, { viewport: { width: 300, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw without rooms info', async() => {
        await render(<SiteComponent site={siteWithoutRoomsInfo} />, { viewport: { width: 300, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw loading', async() => {
        await render(<SiteComponent loading />, { viewport: { width: 300, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw error', async() => {
        await render(<SiteComponent error />, { viewport: { width: 300, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
