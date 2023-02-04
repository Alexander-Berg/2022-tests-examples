/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/subscriptions/actions/createFromListingState');
jest.mock('auto-core/react/dataDomain/subscriptions/actions/sendEmailConfirmation');

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const ListingPromoSubscriptionCore = require('./ListingPromoSubscriptionCore');
const ListingPromoSubscription = require('../ListingPromoSubscription');

const createSubscriptionFromListingState = require('auto-core/react/dataDomain/subscriptions/actions/createFromListingState');
const sendEmailConfirmation = require('auto-core/react/dataDomain/subscriptions/actions/sendEmailConfirmation');
const userWithoutAuthMock = require('auto-core/react/dataDomain/user/mocks/withoutAuth.mock');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

let props;
let context;
let initialState;

beforeEach(() => {
    initialState = {
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                },
            },
        },
        geo: { gidsInfo: [] },
        user: { data: {} },
    };
    props = {};
    context = _.cloneDeep(contextMock);
});

describe('при сабмите формы', () => {
    describe('если почта корректная', () => {
        let page;
        const createSubscriptionPromise = Promise.resolve();
        let originalWindowLocation;

        beforeEach(() => {
            originalWindowLocation = global.window.location;
            delete global.window.location;

            props.userHasEmail = false;
            initialState.user = userWithoutAuthMock;
            createSubscriptionFromListingState.mockReturnValue(() => createSubscriptionPromise);
            sendEmailConfirmation.mockReturnValue(() => () => {});

            page = shallowRenderComponent({ initialState, props, context }).dive().dive();
            const input = page.find('TextInput');
            input.simulate('change', 'foo@bar.com');
        });

        afterEach(() => {
            global.window.location = originalWindowLocation;
        });

        it('прокинет в экшн корректный return_url если у текущего url не было параметров', () => {
            global.window.location = { href: 'https://foo.bar' };

            const form = page.find('form');
            form.simulate('submit', { preventDefault: () => {} });

            return createSubscriptionPromise
                .then(() => {
                    expect(sendEmailConfirmation).toHaveBeenCalledTimes(1);
                    expect(sendEmailConfirmation).toHaveBeenCalledWith({
                        email: 'foo@bar.com',
                        id: undefined,
                        redirect_path: 'https://foo.bar/?show-searches=true&subs_confirm_popup=true',
                    });
                });
        });

        it('прокинет в экшн корректный return_url если у текущего url были параметры', () => {
            global.window.location = { href: 'https://foo.bar/?baz=true' };

            const form = page.find('form');
            form.simulate('submit', { preventDefault: () => { } });

            return createSubscriptionPromise
                .then(() => {
                    expect(sendEmailConfirmation).toHaveBeenCalledTimes(1);
                    expect(sendEmailConfirmation).toHaveBeenCalledWith({
                        email: 'foo@bar.com',
                        id: undefined,
                        redirect_path: 'https://foo.bar/?baz=true&show-searches=true&subs_confirm_popup=true',
                    });
                });
        });
    });
});

describe('рендерит правильный компонент подписки в листинге', () => {
    it('без экспа и при пустом листинге', () => {
        const component = shallowRenderComponent({
            initialState,
            props,
            context,
        }).dive();

        expect(component.type()).toEqual(ListingPromoSubscription);
    });
});

it('не рендерит компонент подписки в листинге если уже есть подписка и нет экспа', () => {
    const newState = {
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'used',
                },
            },
        },
        subscriptions: { data: [ { data: { params: { section: 'used' } } } ] },
        geo: { gidsInfo: [] },
        user: { data: {} },
    };
    const component = shallowRenderComponent({
        initialState: newState,
        props: { ...props, isEmptyListing: true },
        context,
    }).dive().dive();

    expect(component).toBeEmptyRender();
});

function shallowRenderComponent({ context, initialState, props }) {
    const ContextProvider = createContextProvider(context);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <ListingPromoSubscriptionCore { ...props } store={ store }/>
        </ContextProvider>,
    ).dive();
}
