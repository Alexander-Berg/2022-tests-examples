import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionsTabSelectorComponent } from '../';

const mockTabs = [
    { label: 'Мои поиски', value: 'search' },
    { label: 'Новостройки', value: 'newbuilding' },
    { label: 'Изменение цены', value: 'price' }
];

describe('SubscriptionsTabSelectorMobile', () => {
    it('should render tab selector', async() => {
        await render(
            (
                <SubscriptionsTabSelectorComponent
                    tabs={mockTabs}
                    linkBuilder={() => 'https://realty.yandex.ru'}
                />
            ),
            { viewport: { width: 350, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
