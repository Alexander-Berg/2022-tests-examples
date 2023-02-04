import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferEGRNReportPaidPromotion } from '../';

const WIDTH_SMALL = 850;

const TEST_CASES = [
    { price: 200, description: 'рендерится без скидки' },
    { price: 200, basePrice: 204, description: 'рендерится с однозначной скидкой' },
    { price: 200, basePrice: 270, description: 'рендерится с двузначной скидкой' },
];

advanceTo(new Date('2020-07-27'));

describe('OfferEGRNReportPaidPromotion', () => {
    TEST_CASES.forEach(({ description, ...props }) => {
        it(description, async () => {
            await render(
                <AppProvider>
                    <OfferEGRNReportPaidPromotion onPurchaseClick={() => undefined} isAuth={false} {...props} />
                </AppProvider>,
                { viewport: { width: WIDTH_SMALL, height: 550 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
