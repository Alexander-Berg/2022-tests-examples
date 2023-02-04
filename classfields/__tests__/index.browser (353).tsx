import { render } from 'jest-puppeteer-react';

import React from 'react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import DeveloperCard from '../index';

import { DEVELOPER_CARD, DEVELOPER_CARD_WITH_FLATS_IN_SALE } from './mock';

const getComponent = (props: AnyObject) => (
    <AppProvider initialState={props}>
        <DeveloperCard />
    </AppProvider>
);

advanceTo(new Date('2022-05-24T12:20:00.111Z'));

describe('DeveloperCard', () => {
    it('Карточка застройщика с существующими квартирами на продажу', async () => {
        await render(getComponent({ ...DEVELOPER_CARD }), { viewport: { width: 1440, height: 900 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Карточка застройщика без квартир на продажу', async () => {
        await render(getComponent({ ...DEVELOPER_CARD_WITH_FLATS_IN_SALE }), {
            viewport: { width: 1440, height: 900 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
