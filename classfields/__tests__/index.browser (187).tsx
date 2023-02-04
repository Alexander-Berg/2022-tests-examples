import { render } from 'jest-puppeteer-react';

import React from 'react';

import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RECENT_VIEW_OFFERS } from 'realty-core/view/react/modules/visited-offers/mock';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { DeveloperCardSeenRecently, IDeveloperCardSeenRecentlyProps } from '../index';

const getComponent = (props: IDeveloperCardSeenRecentlyProps) => (
    <AppProvider>
        <DeveloperCardSeenRecently {...props} />
    </AppProvider>
);

advanceTo(new Date('2022-05-24T12:20:00.111Z'));

describe('DeveloperCardSeenRecently', () => {
    it('В блок "ранее смотрели" не пришли оферы', async () => {
        await render(getComponent({ visitedOffers: [] }), { viewport: { width: 420, height: 600 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка блока "Ранее смотрели"', async () => {
        await render(getComponent({ visitedOffers: RECENT_VIEW_OFFERS }), {
            viewport: { width: 420, height: 600 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
