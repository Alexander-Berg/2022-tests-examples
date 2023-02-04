import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import t from 'realty-core/view/react/modules/subscription-list/common/components/SubscriptionListController/i18n';

import { SubscriptionListEmpty } from '../';

describe('SubscriptionListEmptyDesktop', () => {
    it('should render empty list component with generic text', async() => {
        await render(
            <SubscriptionListEmpty text='<описание>' />,
            { viewport: { width: 600, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render empty list component for search tab', async() => {
        await render(
            <SubscriptionListEmpty text={t('searchEmptyText')} />,
            { viewport: { width: 600, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render empty list component for newbuilding tab', async() => {
        await render(
            <SubscriptionListEmpty text={t('newbuildingEmptyText')} />,
            { viewport: { width: 600, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render empty list component for price tab', async() => {
        await render(
            <SubscriptionListEmpty text={t('priceEmptyText')} />,
            { viewport: { width: 600, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
