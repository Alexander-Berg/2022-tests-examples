import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionList } from '../';

const subscriptionsMock = [
    {
        url: 'https://realty.yandex.ru/offer/123',
        id: '33333333',
        title: 'Изменение цены в 2-комнатной квартире',
        description: 'по адресу Москва, Ленина 22',
        email: 'someone@mail.ru',
        isActive: false,
        isConfirmed: true,
        autoconfirmed: true,
        deleted: false
    },
    {
        url: 'https://realty.yandex.ru/offer/456',
        id: '44444444',
        title: 'Изменение цены в доме',
        description: 'по адресу Москва, Банковский пер. 1',
        email: 'someone@mail.ru',
        isActive: true,
        isConfirmed: false,
        autoconfirmed: false,
        deleted: false
    },
    {
        url: 'https://realty.yandex.ru/offer/789',
        id: '55555555',
        title: 'Изменение цены для участка 100 соток',
        description: 'по адресу Брянск, деревня Михайлово, 35',
        email: 'someone@mail.ru',
        isActive: true,
        isConfirmed: true,
        autoconfirmed: false,
        deleted: true
    }
];

describe('SubscriptionsListDesktop', () => {
    it('should render list with 3 subscriptions', async() => {
        await render(
            <SubscriptionList
                subscriptions={subscriptionsMock}
            />,
            { viewport: { width: 600, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render spinner overlay above subscription tiles when loading', async() => {
        await render(
            <SubscriptionList
                subscriptions={subscriptionsMock}
                subscriptionsLoading
            />,
            { viewport: { width: 600, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render empty list component when given empty array of subscriptions', async() => {
        await render(
            <SubscriptionList
                subscriptions={[]}
                emptyListText='<описание>'
            />,
            { viewport: { width: 600, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render empty list component when not given array of subscriptions', async() => {
        await render(
            <SubscriptionList emptyListText='<описание>' />,
            { viewport: { width: 600, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render loader when given no subscriptions and loading', async() => {
        await render(
            <SubscriptionList
                subscriptionsLoading
            />,
            { viewport: { width: 600, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render list in error state', async() => {
        await render(
            <SubscriptionList
                subscriptionsLoadingError
            />,
            { viewport: { width: 600, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
