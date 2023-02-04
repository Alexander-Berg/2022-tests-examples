import React from 'react';
import { render } from 'jest-puppeteer-react';

// eslint-disable-next-line no-restricted-imports
import { rejectPromise, AppProvider } from 'realty-www/view/react/libs/test-helpers';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import Subscriptions from '../';

const mockTabs = [
    { label: 'Мои поиски', value: 'search' },
    { label: 'Новостройки', value: 'newbuilding' },
    { label: 'Изменение цены', value: 'price' }
];

const singleSubscriptionMock = {
    url: 'https://realty.yandex.ru/offer/890',
    id: '22222222',
    title: 'Изменение цены в 2-комнатной квартире',
    description: 'по адресу Москва, Ленина 22',
    email: 'someone@mail.ru',
    frequency: 60,
    isActive: false,
    isConfirmed: true,
    autoconfirmed: false,
    deleted: false
};

const subscriptionsMock = [
    {
        url: 'https://realty.yandex.ru/offer/123',
        id: '33333333',
        title: 'Изменение цены в 2-комнатной квартире',
        description: 'по адресу Москва, Ленина 22',
        email: 'someone@mail.ru',
        frequency: 60,
        isActive: false,
        isConfirmed: true,
        autoconfirmed: false,
        deleted: false
    },
    {
        url: 'https://realty.yandex.ru/offer/456',
        id: '44444444',
        title: 'Изменение цены в доме',
        description: 'по адресу Москва, Банковский пер. 1',
        email: 'someone@mail.ru',
        frequency: 60,
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
        frequency: 60,
        isActive: true,
        isConfirmed: true,
        autoconfirmed: false,
        deleted: true
    }
];

const initialState = {
    routing: { locationBeforeTransitions: { query: {}, pathname: '', search: '' } },
    config: { origin: 'https://yandex.ru/' },
    user: {}
};

const SubscriptionsComponent = props => (
    <AppProvider initialState={initialState}>
        <Subscriptions {...props} linkBuilder={() => 'https://realty.yandex.ru'} />
    </AppProvider>
);

describe('SubscriptionsComponentM', () => {
    it('subscriptions component with 3 subscriptions and 3 tabs for non-authorized user', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
                selectedTab='price'
                subscriptions={subscriptionsMock}
            />,
            { viewport: { width: 350, height: 1200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('tab selector instead of list when no tab is selected', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
            />,
            { viewport: { width: 350, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should show promo-toggler when given shouldShowPromoToggler prop', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
                shouldShowPromoToggler
            />,
            { viewport: { width: 350, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('subscriptions component with 3 subscriptions and 3 tabs for authorized user', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
                selectedTab='price'
                subscriptions={subscriptionsMock}
                isAuth
            />,
            { viewport: { width: 350, height: 1200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should open delete confirmation modal when "delete" is clicked on a subscription tile', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
                selectedTab='price'
                subscriptions={subscriptionsMock}
                isAuth
            />,
            { viewport: { width: 350, height: 1200 } }
        );

        await page.click('[data-test=subscription-tile-options-button]');
        await page.waitFor(500);
        await page.click('[data-test=subscription-options-delete]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should open options modal when clicked subscription tile options', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
                selectedTab='price'
                subscriptions={subscriptionsMock}
                isAuth
            />,
            { viewport: { width: 350, height: 1200 } }
        );

        await page.click('[data-test=subscription-tile-options-button]');
        await page.waitFor(500);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('modified list after successfully deleting a subscription', async() => {
        class DeleteScenarioComponent extends React.Component {
            state = { subscription: singleSubscriptionMock };

            handleDeleteId = id => this.setState(prevState => {
                if (id === prevState.subscription.id) {
                    return {
                        subscription: {
                            ...prevState.subscription,
                            deleted: true
                        }
                    };
                }

                return prevState;
            });

            render() {
                return (
                    <SubscriptionsComponent
                        tabs={mockTabs}
                        selectedTab='price'
                        onDeleteSubscription={this.handleDeleteId}
                        isAuth
                        subscriptions={[ this.state.subscription ]}
                    />
                );
            }
        }

        await render(
            <DeleteScenarioComponent />,
            { viewport: { width: 350, height: 1000 } }
        );

        await page.click('[data-test=subscription-tile-options-button]');
        await page.waitFor(500);
        await page.click('[data-test=subscription-options-delete]');
        await page.click('[data-test=subscription-delete-submit-button]');
        await page.waitFor(200);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('delete confirmation modal in error state after unsuccessfully deleting a subscription',
        async() => {
            await render(
                <SubscriptionsComponent
                    tabs={mockTabs}
                    selectedTab='price'
                    onDeleteSubscription={rejectPromise()}
                    isAuth
                    subscriptions={[ singleSubscriptionMock ]}
                />,
                { viewport: { width: 350, height: 1000 } }
            );

            await page.click('[data-test=subscription-tile-options-button]');
            await page.waitFor(500);
            await page.click('[data-test=subscription-options-delete]');
            await page.click('[data-test=subscription-delete-submit-button]');
            await page.waitFor(200);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

    it('should open delete confirmation modal when mounted with "unsubscribeUrlId" prop', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
                selectedTab='price'
                subscriptions={[ singleSubscriptionMock ]}
                unsubscribeUrlId={singleSubscriptionMock.id}
                isAuth
            />,
            { viewport: { width: 350, height: 1000 } }
        );

        await page.waitFor(500);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should open settings modal when "settings" is clicked on a subscription tile', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
                selectedTab='price'
                subscriptions={subscriptionsMock}
                isAuth
            />,
            { viewport: { width: 350, height: 1000 } }
        );

        await page.click('[data-test=subscription-tile-options-button]');
        await page.waitFor(500);
        await page.click('[data-test=subscription-options-settings]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('settings modal in error state when rejected on save', async() => {
        await render(
            <SubscriptionsComponent
                tabs={mockTabs}
                selectedTab='price'
                subscriptions={subscriptionsMock}
                onUpdateSubscription={rejectPromise()}
                isAuth
            />,
            { viewport: { width: 350, height: 1200 } }
        );

        await page.click('[data-test=subscription-tile-options-button]');
        await page.waitFor(500);
        await page.click('[data-test=subscription-options-settings]');
        await page.click('[data-test=subscription-settings-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should successfully edit subscription', async() => {
        class SettingsScenarioComponent extends React.Component {
            state = { editableSubscription: singleSubscriptionMock };

            handleEdit = subscription => this.setState({ editableSubscription: subscription });

            render() {
                return (
                    <SubscriptionsComponent
                        tabs={mockTabs}
                        selectedTab='price'
                        onUpdateSubscription={this.handleEdit}
                        isAuth
                        subscriptions={[ this.state.editableSubscription ]}
                    />
                );
            }
        }

        await render(
            <SettingsScenarioComponent />,
            { viewport: { width: 350, height: 800 } }
        );

        // открываем окно настроек, вносим изменения в имейл, сохраняем, открываем его снова
        // чтобы посмотреть, что всё изменилось как нужно

        await page.click('[data-test=subscription-tile-options-button]');
        await page.waitFor(500);
        await page.click('[data-test=subscription-options-settings]');
        await page.click('[data-test=subscription-settings-email-input]', { clickCount: 3 }); // выделяем уже имеющийся имейл
        await page.type('[data-test=subscription-settings-email-input]', 'newemail@mail.ru'); // и пишем сверху новый
        await page.click('[data-test=subscription-settings-submit-button]');
        await page.waitFor(200);
        await page.click('[data-test=subscription-tile-options-button]');
        await page.waitFor(500);
        await page.click('[data-test=subscription-options-settings]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
