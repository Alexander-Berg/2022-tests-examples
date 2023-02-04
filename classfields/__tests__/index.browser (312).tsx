import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansSerpModal, FiltersState } from '..';

import { getProps } from './mocks';

const render = async (filtersState: FiltersState) => {
    await _render(
        <AppProvider fakeTimers={{ now: new Date('2020-06-02T09:00:00.111Z').getTime() }}>
            <SitePlansSerpModal {...getProps(filtersState)}>
                <div>Любой контент</div>
            </SitePlansSerpModal>
        </AppProvider>,
        { viewport: { width: 320, height: 500 } }
    );
};

describe('SitePlansSerpModal', () => {
    it('рисует модалку', async () => {
        await render(FiltersState.EMPTY);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модалку с активными фильтрами', async () => {
        await render(FiltersState.ACTIVE);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
