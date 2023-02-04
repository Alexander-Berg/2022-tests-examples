import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansModal } from '../index';

import { getInitialState, getCard, getPlan } from './mocks';

describe('SitePlansModal', () => {
    it('рисует модалку с кнопкой позвонить', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansModal
                    isOpened
                    plan={getPlan()}
                    card={getCard({ withPhone: true })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модалку без изображения (квартира)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansModal
                    isOpened
                    plan={getPlan()}
                    card={getCard()}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модалку без площади кухни (апартаменты)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansModal
                    isOpened
                    plan={getPlan({ withKitchenArea: false })}
                    card={getCard({ isApartment: true })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модалку без площади кухни (квартиры и апартаменты)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansModal
                    isOpened
                    plan={getPlan({ withKitchenArea: false })}
                    card={getCard({ isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модалку студии', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansModal
                    isOpened
                    plan={getPlan({ isStudio: true, withKitchenArea: false })}
                    card={getCard()}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модалку студии (апартаменты)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansModal
                    isOpened
                    plan={getPlan({ isStudio: true, withKitchenArea: false })}
                    card={getCard({ isApartment: true })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модалку студии (квартиры и апартаменты)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansModal
                    isOpened
                    plan={getPlan({ isStudio: true, withKitchenArea: false })}
                    card={getCard({ isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
