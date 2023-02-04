import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { promotionSeoSelector } from 'realty-core/view/react/deskpad/selectors/seo/pages/promotion';

import { AppProvider } from 'view/react/libs/test-helpers';

import { PromotionPageRouter } from '..';

advanceTo(new Date('2020-12-16T03:00:00.111Z'));

const getInitialState = promoPageType => ({
    page: {
        params: {
            type: promoPageType
        }
    }
});

const Component = ({ promoPageType }) => {
    const initialState = getInitialState(promoPageType);
    const seoTexts = promotionSeoSelector(initialState).seoTexts;

    return (
        <AppProvider initialState={initialState}>
            <PromotionPageRouter seoTexts={seoTexts} />
        </AppProvider>
    );
};

const pageTypes = [
    'entity',
    'newbuildings',
    'secondaryAndCommercial',
    'villages',
    'options',
    'individual',
    'rates',
    'advert',
    'useful',
    'activity',
    'webinars'
];

const widths = [ 1000, 1440 ];

describe('PromotionPageRouter', () => {
    // eslint-disable-next-line no-unused-vars
    for (const width of widths) {
        // eslint-disable-next-line no-unused-vars
        for (const pageType of pageTypes) {
            it(`Контент страницы ${pageType} ${width}px`, async() => {
                await render(
                    <Component promoPageType={pageType} />,
                    { viewport: { width, height: 1080 } }
                );

                await page.addStyleTag({ content: 'body{padding: 0}' });

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        }
    }
});
