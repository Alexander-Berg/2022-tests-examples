/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/dataDomain/state/actions/userPromoPopupFetch');
const userPromoPopupFetchMock = require('auto-core/react/dataDomain/state/actions/userPromoPopupFetch');
userPromoPopupFetchMock.mockImplementation(() => () => {});

const React = require('react');
const { Provider } = require('react-redux');
const { mount, shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const Context = createContextProvider(contextMock);

const PromoPopup = require('./PromoPopup');

it('попапы запрашиваются на маунте', () => {
    const store = mockStore({
        user: { data: {} },
        config: { data: { pageParams: { showhistory: 'on' }, pageType: 'sales' } },
        cookie: {},
        state: { userPromoPopup: { name: 'any-other', data: {} } },
        geo: {},
        sales: { items: [ offerMock ] },
    });

    mount(
        <Context>
            <Provider store={ store }>
                <PromoPopup/>
            </Provider>
        </Context>,
    );

    expect(userPromoPopupFetchMock).toHaveBeenCalledTimes(1);
    expect(userPromoPopupFetchMock).toHaveBeenCalledWith({
        routeName: 'sales',
        showhistory: 'on',
    });
});

it('если передаем shouldNotFetchOnMount, то попапы НЕ запрашиваются на маунте', () => {
    const store = mockStore({
        user: { data: {} },
        config: { data: { pageParams: { showhistory: 'on' }, pageType: 'sales' } },
        cookie: {},
        state: { userPromoPopup: { name: 'any-other', data: {} } },
        geo: {},
        sales: { items: [ offerMock ] },
    });

    mount(
        <Context>
            <Provider store={ store }>
                <PromoPopup shouldNotFetchOnMount/>
            </Provider>
        </Context>,
    );

    expect(userPromoPopupFetchMock).not.toHaveBeenCalled();
});

it('в случае попапа no-photos запросит промо-попапы после закрытия', () => {
    const store = mockStore({
        user: { data: {} },
        config: { data: { pageParams: { showhistory: 'on' }, pageType: 'sales' } },
        cookie: {},
        state: { userPromoPopup: { name: 'no-photos', data: {} } },
        geo: {},
        sales: { items: [ offerMock ] },
    });

    const component = shallow(
        <Context>
            <Provider store={ store }>
                <PromoPopup shouldNotFetchOnMount/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    const modal = component.find('Connect(AddPhotosPopup)').dive();

    modal.props().onPopupClose();

    expect(userPromoPopupFetchMock).toHaveBeenCalledTimes(1);
    expect(userPromoPopupFetchMock).toHaveBeenCalledWith({
        routeName: 'sales',
        showhistory: 'on',
    });
});

it('в случае попапа reseller-public-profile-promo запросит промо-попапы после закрытия', () => {
    const store = mockStore({
        user: { data: {} },
        config: { data: { pageParams: { showhistory: 'on' }, pageType: 'sales' } },
        cookie: {},
        state: { userPromoPopup: { name: 'no-photos', data: {} } },
        geo: {},
        sales: { items: [ offerMock ] },
    });

    const component = shallow(
        <Context>
            <Provider store={ store }>
                <PromoPopup shouldNotFetchOnMount/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    const modal = component.find('Connect(AddPhotosPopup)').dive();

    modal.props().onPopupClose();

    expect(userPromoPopupFetchMock).toHaveBeenCalledTimes(1);
    expect(userPromoPopupFetchMock).toHaveBeenCalledWith({
        routeName: 'sales',
        showhistory: 'on',
    });
});
