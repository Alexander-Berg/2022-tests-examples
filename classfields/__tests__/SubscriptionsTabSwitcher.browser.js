import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionsTabSwitcherComponent } from '../';

const mockTabs = [
    { label: 'Мои поиски', value: 'search' },
    { label: 'Новостройки', value: 'newbuilding' },
    { label: 'Изменение цены', value: 'price' }
];

describe('SubscriptionsTabSwitcher', () => {
    it('should render tab switcher', async() => {
        await render(
            (
                <SubscriptionsTabSwitcherComponent
                    tabs={mockTabs}
                    selectedTab='newbuilding'
                    linkBuilder={() => 'https://realty.yandex.ru'}
                />
            ),
            { viewport: { width: 450, height: 230 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
