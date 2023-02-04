jest.mock('auto-core/react/lib/cookie', () => {
    return {
        set: jest.fn(),
        get: jest.fn(),
    };
});
jest.mock('www-cabinet/react/dataDomain/state/selectors/isVisibleCashbackPopup', () => jest.fn());

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { Provider } = require('react-redux');
const _ = require('lodash');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const bunkerData = require('./mock/bunkerData');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const isVisibleCashbackPopup = require('www-cabinet/react/dataDomain/state/selectors/isVisibleCashbackPopup');

const cookie = require('auto-core/react/lib/cookie');

const ContextProvider = createContextProvider(contextMock);

const PromoPopupFromBunker = require('./PromoPopupFromBunker');

it('Должен отрендерить попап со всеми элементами', () => {
    const appPromoPopup = shallow(
        <ContextProvider>
            <Provider store={ mockStore(bunkerData) }>
                <PromoPopupFromBunker/>
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();

    expect(shallowToJson(appPromoPopup)).toMatchSnapshot();
});

describe('componentDidMount тесты', () => {
    it('Должен отправить событие в метрику, установить корректный state и cookie', () => {
        const appPromoPopup = shallow(
            <ContextProvider>
                <Provider store={ mockStore(bunkerData) }>
                    <PromoPopupFromBunker/>
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();

        appPromoPopup.state = { isVisible: false };

        appPromoPopup.setState = jest.fn();
        cookie.set.mockClear();
        contextMock.metrika.params.mockClear();

        appPromoPopup.instance().componentDidMount();

        expect(contextMock.metrika.params).toHaveBeenCalledWith([ 'app-promo-shown' ]);
        expect(cookie.set).toHaveBeenCalledWith('appPromoPopupWasShown2', true, { expires: 30 });
        expect(appPromoPopup.setState).toHaveBeenCalledWith({ isVisible: true });
    });

    it('Должен перезаписать куку, если isVisibleAnotherPopup', () => {
        isVisibleCashbackPopup.mockImplementationOnce(() => true);

        const appPromoPopup = shallow(
            <ContextProvider>
                <Provider store={ mockStore(bunkerData) }>
                    <PromoPopupFromBunker/>
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();

        appPromoPopup.instance().componentDidMount();

        expect(cookie.set).toHaveBeenCalledWith('appPromoPopupWasShown2', true, { expires: 1 });
    });

    it('Не должен ничего делать, если store.bunker.app_promo_popup.enabled = false', () => {
        const store = _.cloneDeep(bunkerData);
        store.bunker.app_promo_popup.enabled = false;

        const appPromoPopup = shallow(
            <ContextProvider>
                <Provider store={ mockStore(store) }>
                    <PromoPopupFromBunker/>
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();

        appPromoPopup.setState = jest.fn();

        appPromoPopup.instance().componentDidMount();

        expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalled();
        expect(cookie.set).not.toHaveBeenCalled();
        expect(appPromoPopup.setState).not.toHaveBeenCalled();
    });

    it('Не должен ничего делать, если cookie уже была установлена', () => {
        const appPromoPopup = shallow(
            <ContextProvider>
                <Provider store={ mockStore(bunkerData) }>
                    <PromoPopupFromBunker/>
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();

        appPromoPopup.setState = jest.fn();
        cookie.get.mockImplementationOnce(() => 'somecookie');
        contextMock.metrika.sendPageEvent.mockClear();
        cookie.set.mockClear();

        appPromoPopup.instance().componentDidMount();

        expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalled();
        expect(cookie.set).not.toHaveBeenCalled();
        expect(appPromoPopup.setState).not.toHaveBeenCalled();
    });

    it('Не должен ничего делать, если regionId не входит в список регионов таргетирования', () => {
        const store = _.cloneDeep(bunkerData);
        store.config = {
            regionId: 2,
        };
        store.bunker.app_promo_popup.targetRegions = [ 1 ];

        const appPromoPopup = shallow(
            <ContextProvider>
                <Provider store={ mockStore(store) }>
                    <PromoPopupFromBunker/>
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();

        appPromoPopup.setState = jest.fn();

        appPromoPopup.instance().componentDidMount();

        expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalled();
        expect(cookie.set).not.toHaveBeenCalled();
        expect(appPromoPopup.setState).not.toHaveBeenCalled();
    });

    it('Не должен ничего делать, если у пользователя не подключены нужные тарифы', () => {
        const store = _.cloneDeep(bunkerData);
        store.promoPopup = {
            tariffsCategorySection: [],
        };
        store.bunker.app_promo_popup.targetTariffs = [ 'CARS_NEW' ];

        const appPromoPopup = shallow(
            <ContextProvider>
                <Provider store={ mockStore(store) }>
                    <PromoPopupFromBunker/>
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();

        appPromoPopup.setState = jest.fn();

        appPromoPopup.instance().componentDidMount();

        expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalled();
        expect(cookie.set).not.toHaveBeenCalled();
        expect(appPromoPopup.setState).not.toHaveBeenCalled();
    });

    it('Не должен ничего делать, если стоит флаг multipostingOnly, но клиент не мультипостинговый', () => {
        const store = _.cloneDeep(bunkerData);
        store.client = {
            multiposting_enabled: '0',
        };
        store.bunker.app_promo_popup.multipostingOnly = true;

        const appPromoPopup = shallow(
            <ContextProvider>
                <Provider store={ mockStore(store) }>
                    <PromoPopupFromBunker/>
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();

        appPromoPopup.setState = jest.fn();

        appPromoPopup.instance().componentDidMount();

        expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalled();
        expect(cookie.set).not.toHaveBeenCalled();
        expect(appPromoPopup.setState).not.toHaveBeenCalled();
    });

});

it('onClose тест: должен установить корректный state', () => {
    const appPromoPopup = shallow(
        <ContextProvider>
            <Provider store={ mockStore(bunkerData) }>
                <PromoPopupFromBunker/>
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();

    appPromoPopup.setState = jest.fn();
    appPromoPopup.state = { isVisible: true };

    appPromoPopup.instance().onClose();

    expect(appPromoPopup.setState).toHaveBeenCalledWith({ isVisible: false });
});
