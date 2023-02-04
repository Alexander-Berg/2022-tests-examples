import React from 'react';
import { render as _render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import CardPhone from '../';

import { state, salesDepartment } from './mocks';

advanceTo(new Date('2021-04-10 23:59:59'));

const render = ({ initialState = state, props }: { initialState?: AnyObject; props?: AnyObject } = {}) =>
    _render(
        <AppProvider initialState={initialState}>
            <CardPhone {...(props ?? {})} />
        </AppProvider>,
        { viewport: { width: 320, height: 150 } }
    );

describe('CardPhone', () => {
    it('рисует корректно', async () => {
        await render({ props: { salesDepartment, withBilling: true, withTimeToCall: true } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует корректно без режима работы', async () => {
        await render({});

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
