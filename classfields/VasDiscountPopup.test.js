/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/state/actions/userPromoPopupOpen');
jest.mock('auto-core/react/dataDomain/state/actions/userPromoPopupClose');
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

jest.mock('auto-core/lib/util/vas/logger');
jest.mock('auto-core/react/dataDomain/cookies/actions/set');

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const VasDiscountPopup = require('./VasDiscountPopup');

const userPromoPopupOpen = require('auto-core/react/dataDomain/state/actions/userPromoPopupOpen');
userPromoPopupOpen.mockReturnValue(() => () => { });

const userPromoPopupClose = require('auto-core/react/dataDomain/state/actions/userPromoPopupClose');
userPromoPopupClose.mockReturnValue(() => () => { });

const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');
paymentModalOpen.mockImplementation(() => () => { });

const setCookieActions = require('auto-core/react/dataDomain/cookies/actions/set').default;
setCookieActions.mockReturnValue(() => () => {});

const VasLogger = require('auto-core/lib/util/vas/logger').default;
const logVasEventMock = jest.fn();
VasLogger.mockReturnValue({
    logVasEvent: logVasEventMock,
});

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const MockDate = require('mockdate');

const getServiceDiscountResponseMock = require('auto-core/server/resources/publicApiBilling/methods/getServiceDiscount.nock.fixture');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const vasPromoPreparer = require('auto-core/server/preparers/publicApi/promo/vasPromo');

let props;
let context;
const originalGlobalLocation = global.location;

beforeEach(() => {
    const getServiceDiscountResponse = vasPromoPreparer({
        getServicesDiscount: getServiceDiscountResponseMock.with_active_discount(),
        bunker: getBunkerMock([ 'common/vas', 'common/vas_vip' ], { spreadSubNodes: true }),
    });
    delete getServiceDiscountResponse.offer;

    props = {
        data: _.cloneDeep(getServiceDiscountResponse),
        isAllowedOpen: true,
        isOpened: false,
        dispatch: jest.fn(),
        hasBeenClosed: false,
    };
    context = _.cloneDeep(contextMock);

    context.metrika.sendParams.mockClear();
    userPromoPopupOpen.mockClear();
    VasLogger.mockClear();

    delete global.location;
    global.location = {
        assign: jest.fn(),
    };
});

afterEach(() => {
    jest.useRealTimers();
    global.location = originalGlobalLocation;
});

it('ничего не нарисует если модал нельзя открывать', () => {
    props.isAllowedOpen = false;
    const page = shallowRenderComponent({ props, context });

    expect(page.isEmptyRender()).toBe(true);
});

describe('при маунте', () => {

    it('ничего не будет делать если пользователь уже закрыл модал ранее', () => {
        props.hasBeenClosed = true;
        shallowRenderComponent({ props, context });

        expect(userPromoPopupOpen).toHaveBeenCalledTimes(0);
    });

    it('вызовет экшен на открытие модала если он не был закрыт ранее', () => {
        shallowRenderComponent({ props, context });

        expect(userPromoPopupOpen).toHaveBeenCalledTimes(1);
    });

    it('проинициализирует вас логгер', () => {
        shallowRenderComponent({ props, context });

        expect(VasLogger).toHaveBeenCalledTimes(1);
        expect(VasLogger).toHaveBeenCalledWith(contextMock.metrika, {
            from: 'api_m_popup_discount',
            isMobile: true,
        });
    });
});

describe('при открытии модала', () => {
    describe('если до конца распродажи осталось больше 10 секунд', () => {
        beforeEach(() => {
            MockDate.set('2019-11-06T20:59:49Z');
        });

        it('отправит метрику открытия если она еще не была отпралена', () => {
            const page = shallowRenderComponent({ props, context });
            page.setProps({ isOpened: true });
            expect(context.metrika.params).toHaveBeenCalledTimes(1);
            expect(context.metrika.params).toHaveBeenCalledWith({ 'vas-discount': 'open' });
        });

        it('не будет отправлять метрику открытия повторно', () => {
            const page = shallowRenderComponent({ props, context });
            page.setProps({ isOpened: true });
            page.setProps({ isOpened: false });
            page.setProps({ isOpened: true });
            expect(context.metrika.params).toHaveBeenCalledTimes(1);
        });

        it('если сервис со скидкой только один, расхлопнет его сниппет', () => {
            props.data.services = props.data.services.slice(0, 1);
            const page = shallowRenderComponent({ props, context });
            page.setProps({ isOpened: true });

            const snippet = page.find('VasPromoItem');
            expect(snippet.prop('isCollapsed')).toBe(false);
        });

        it('если сервисов несколько не будет расхловывать ни один спиппет', () => {
            const page = shallowRenderComponent({ props, context });
            page.setProps({ isOpened: true });

            const snippets = page.find('VasPromoItem');
            snippets.forEach((snippet) => {
                expect(snippet.prop('isCollapsed')).toBe(true);
            });
        });

        it('если у пользователя только одна объява залогирует показ васов', () => {
            const page = shallowRenderComponent({ props, context });
            page.setProps({ isOpened: true });

            expect(logVasEventMock).toHaveBeenCalledTimes(3);
            expect(logVasEventMock.mock.calls).toMatchSnapshot();
        });

        it('если объяв больше чем одна ничего не будет логировать', () => {
            props.data.hasMoreThanOneOffer = true;
            const page = shallowRenderComponent({ props, context });
            page.setProps({ isOpened: true });

            expect(logVasEventMock).toHaveBeenCalledTimes(0);
        });
    });

    it('если до конца распродажи меньше 10 секунд ничего не будет делать', () => {
        MockDate.set('2019-11-06T20:59:51Z');
        const page = shallowRenderComponent({ props, context });
        page.setProps({ isOpened: true });

        expect(context.metrika.params).toHaveBeenCalledTimes(0);
        expect(logVasEventMock).toHaveBeenCalledTimes(0);
    });
});

describe('при закрытии модала', () => {
    beforeEach(() => {
        MockDate.set('2019-11-06T18:00:00Z');

        props.isOpened = true;
        const page = shallowRenderComponent({ props, context });
        const modal = page.find('Modal');
        modal.simulate('requestHide');
    });

    it('поставит куку до конца распродажи', () => {
        expect(setCookieActions).toHaveBeenCalledTimes(1);
        expect(setCookieActions.mock.calls).toMatchSnapshot();
    });

    it('отправит экшен на закрытие модала', () => {
        expect(userPromoPopupClose).toHaveBeenCalledTimes(1);
    });
});

describe('при клике на кнопку "купить"', () => {
    beforeEach(() => {
        MockDate.set('2019-11-06T18:00:00Z');
        props.isOpened = true;
        const page = shallowRenderComponent({ props, context });
        const firstSnippet = page.find('VasPromoItem').at(0);
        firstSnippet.simulate('buyButtonClick', firstSnippet.prop('data').service);
    });

    it('отправит экшен на открытие диалога оплаты', () => {
        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalOpen.mock.calls[0]).toMatchSnapshot();
    });

    it('залогирует вас событие', () => {
        expect(logVasEventMock).toHaveBeenCalledTimes(1);
        expect(logVasEventMock.mock.calls[0]).toMatchSnapshot();
    });

    it('поставит куку до конца распродажи', () => {
        expect(setCookieActions).toHaveBeenCalledTimes(1);
        expect(setCookieActions.mock.calls).toMatchSnapshot();
    });

    it('отправит экшен на закрытие модала', () => {
        expect(userPromoPopupClose).toHaveBeenCalledTimes(1);
    });
});

it('когда таймер выйдет закроет модал', () => {
    MockDate.set('2019-11-06T18:00:00Z');
    props.isVisible = true;
    const page = shallowRenderComponent({ props, context });
    const timer = page.find('Timer');
    timer.simulate('timerFinish');

    expect(userPromoPopupClose).toHaveBeenCalledTimes(1);
});

it('при клике на сниппет расхлопнет его, и схлопнет остальные', () => {
    props.isVisible = true;
    const page = shallowRenderComponent({ props, context });
    const snippets = page.find('VasPromoItem');
    snippets.at(0).simulate('click', snippets.at(0).prop('data').service);
    snippets.at(1).simulate('click', snippets.at(1).prop('data').service);

    const updatedSnippets = page.find('VasPromoItem');

    expect(updatedSnippets.at(0).prop('isCollapsed')).toBe(true);
    expect(updatedSnippets.at(1).prop('isCollapsed')).toBe(false);
    expect(updatedSnippets.at(2).prop('isCollapsed')).toBe(true);
});

it('сделает правильный редирект при нажатии на кнопку если у пользователя больше одного объявления', () => {
    props.data.hasMoreThanOneOffer = true;
    props.isVisible = true;
    const page = shallowRenderComponent({ props, context });
    const button = page.find('Button[children="Начать покупки"]');
    button.simulate('click');

    expect(global.location.assign).toHaveBeenCalledTimes(1);
    expect(global.location.assign).toHaveBeenCalledWith('link/sales/?category=cars');
});

function shallowRenderComponent({ context, props }) {
    const ContextProvider = createContextProvider(context);

    return shallow(
        <ContextProvider>
            <VasDiscountPopup { ...props }/>
        </ContextProvider>,
    ).dive();
}
