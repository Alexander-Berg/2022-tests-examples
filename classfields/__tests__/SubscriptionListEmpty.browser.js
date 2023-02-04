import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import t from 'realty-core/view/react/modules/subscription-list/common/components/SubscriptionListController/i18n';
import searchEmptyImage from
    'realty-core/view/react/modules/subscription-list/common/components/SubscriptionListController/assets/emptySearch.svg'; // eslint-disable-line
import newbuildingPriceEmptyImage from
    'realty-core/view/react/modules/subscription-list/common/components/SubscriptionListController/assets/emptyPriceNewbuilding.svg'; // eslint-disable-line

import { SubscriptionListEmpty } from '../';

describe('SubscriptionListEmptyMobile', () => {
    it('should render empty list component with generic text', async() => {
        await render(
            <SubscriptionListEmpty text='<описание>' image={searchEmptyImage} />,
            { viewport: { width: 350, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render empty list component for search tab', async() => {
        await render(
            <SubscriptionListEmpty text={t('searchEmptyText')} image={searchEmptyImage} />,
            { viewport: { width: 350, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render empty list component for newbuilding tab', async() => {
        await render(
            <SubscriptionListEmpty text={t('newbuildingEmptyText')} image={newbuildingPriceEmptyImage} />,
            { viewport: { width: 350, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render empty list component for price tab', async() => {
        await render(
            <SubscriptionListEmpty text={t('priceEmptyText')} image={newbuildingPriceEmptyImage} />,
            { viewport: { width: 350, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
