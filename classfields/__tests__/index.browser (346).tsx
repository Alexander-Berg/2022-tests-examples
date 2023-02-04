import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';

import { DevelopersListBreadcrumbs } from '../index';

const geo = {
    rgid: 417899,
    populatedRgid: 417899,
    name: 'Санкт-Петербург',
    locative: 'в Санкт-Петербурге',
};

describe('DevelopersListBreadcrumbs', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider>
                <DevelopersListBreadcrumbs type="developers-list" geo={geo as IGeoStore} />
            </AppProvider>,
            { viewport: { width: 700, height: 60 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
