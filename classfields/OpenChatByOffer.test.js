/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

jest.mock('auto-core/react/dataDomain/state/actions/authModalWithCallbackOpen', () => jest.fn(
    () => ({ type: '' }),
));

const _ = require('lodash');
const React = require('react');
const OpenChatByOffer = require('./OpenChatByOffer').default;

const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const openAuthModalWithCallback = require('auto-core/react/dataDomain/state/actions/authModalWithCallbackOpen');
const withoutAuth = require('auto-core/react/dataDomain/user/mocks/withoutAuth.mock');
const privateUserStateMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const dealerUserStateMock = require('auto-core/react/dataDomain/user/mocks/dealerWithAccess.mock');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const cardStateMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const statApi = require('auto-core/lib/event-log/statApi').default;

let initialState;
let props;
let store;
let context;
let _location;
beforeEach(() => {
    _location = global.location;
    delete global.location;
    global.location = new URL('https://auto.ru');
    initialState = {
        config: configStateMock.value(),
        bunker: getBunkerMock([ 'common/metrics' ]),
        user: { data: {} },
    };

    const offer = cloneOfferWithHelpers(cardStateMock).withIsOwner(false);

    props = {
        offer: offer.value(),
    };
    context = _.cloneDeep(contextMock);

    context.metrika.sendParams.mockClear();
    statApi.logImmediately.mockClear();
});

afterEach(() => {
    global.location = _location;
});

it('для частника должен прокидывать хэндлер в потомка', () => {
    initialState = {
        ...initialState,
        user: _.cloneDeep(privateUserStateMock),
    };

    const wrapper = shallowRenderOpenChatByOffer();

    const onClickProp = wrapper.props().onClick;

    expect(Boolean(onClickProp)).toBe(true);
});

it('для неавторизованного юзера должен прокидывать хэндлер в потомка', () => {
    initialState = {
        ...initialState,
        user: _.cloneDeep(withoutAuth),
    };

    const wrapper = shallowRenderOpenChatByOffer();

    const onClickProp = wrapper.props().onClick;

    expect(Boolean(onClickProp)).toBe(true);
});

it('для дилера с доступом к чатам должен прокидывать хэндлер в потомка', () => {
    initialState = {
        ...initialState,
        user: _.cloneDeep(dealerUserStateMock),
    };

    const wrapper = shallowRenderOpenChatByOffer();

    const { onClick } = wrapper.props();

    expect(Boolean(onClick)).toBe(true);
});

it('для дилера без доступа к чатам должен дизейблить потомка и передавать noop-функцию', () => {
    const dealerUser = _.cloneDeep(dealerUserStateMock);
    dealerUser.data.access = {};

    initialState = {
        ...initialState,
        user: dealerUser,
    };

    const wrapper = shallowRenderOpenChatByOffer();

    const { disabled, onClick } = wrapper.props();

    expect(disabled).toBe(true);
    expect(onClick).toBe(_.noop);
});

describe('если есть флаг "только чат" и прокинут флаг отправления метрики', () => {
    beforeEach(() => {
        props.offer = cloneOfferWithHelpers(props.offer).withChatOnly().value();
    });

    it('при рендере должен отправить метрику показа', () => {
        shallowRenderOpenChatByOffer();

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'dont_call_me', 'shows' ]);
    });

    it('при открытии тултипа должен отправить метрику показа подсказки', () => {
        const wrapper = shallowRenderOpenChatByOffer();
        wrapper.simulate('tooltipOpen');

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(context.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'dont_call_me', 'tap_hint' ]);
    });

    it('при клике на "открыть чат" отправит метрику клика', () => {
        initialState = {
            ...initialState,
            user: _.cloneDeep(privateUserStateMock),
        };

        const wrapper = shallowRenderOpenChatByOffer();
        wrapper.simulate('click');

        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(3);
        expect(context.metrika.sendPageEvent.mock.calls[1][0]).toEqual([ 'dont_call_me', 'click_chat' ]);
        expect(context.metrika.sendPageEvent.mock.calls[2][0]).toEqual([ 'chat_open' ]);
    });
});

describe('авторизация и открытие чата', () => {
    beforeEach(() => {
        global.vertis_chat = {
            _fns: [],
            onready(fn) {
                this._fns.push(fn);
            },
            open_chat_for_offer: jest.fn(),
        };
    });

    it('должен открыть модал авторизации для анонима', () => {
        initialState = {
            ...initialState,
            user: _.cloneDeep(withoutAuth),
        };

        const wrapper = shallowRenderOpenChatByOffer();
        wrapper.simulate('click');
        expect(openAuthModalWithCallback).toHaveBeenCalled();
    });

    it('должен открыть чат для авторизованного юзера', () => {
        initialState = {
            ...initialState,
            user: _.cloneDeep(privateUserStateMock),
        };

        const wrapper = shallowRenderOpenChatByOffer();
        wrapper.simulate('click');

        expect(global.vertis_chat.open_chat_for_offer).toHaveBeenCalledTimes(1);
        expect(global.vertis_chat.open_chat_for_offer).toHaveBeenCalledWith(
            'cars',
            `${ cardStateMock.id }-${ cardStateMock.hash }`,
            {
                startCall: [
                    { name: 'CONTACT_CARS' },
                    { name: 'CONTACT_CARS_DESKTOP' },
                    { name: 'CONTACT_CARS_REGULAR' },
                    { name: 'CONTACT_CARS_PHONE' },
                    { name: 'PHONE_ALL_CARS2_PRICE-500-1500' },
                    { name: 'CONTACT_CARS_PHONE_USED' },
                    { name: 'ym-send-message' },
                    { name: 'MAUTORU_PHONE_CARS_SHOW' },
                ],
                startDialog: [
                    { name: 'CONTACT_CARS' },
                    { name: 'CONTACT_CARS_DESKTOP' },
                    { name: 'CONTACT_CARS_REGULAR' },
                    { name: 'CHAT_START_DIALOG_CARS_ALL_ALL' },
                    { name: 'PHONE_ALL_CARS2_PRICE-500-1500' },
                    { name: 'CHAT_START_DIALOG_CARS_ALL_USED' },
                    { name: 'CHAT_START_DIALOG_DESKTOP' },
                    { name: 'CHAT_START_DIALOG_REGULAR' },
                ],
                startDialogGoogle360: expect.any(Function),
            },
            { send_preset_text: '' },
        );
    });
});

describe('открытие чата по хешу', () => {
    beforeEach(() => {
        global.vertis_chat = {
            _fns: [],
            onready(fn) {
                this._fns.push(fn);
            },
            open_chat_for_offer: jest.fn(),
        };
        global.location.href = new URL(`https://auto.ru/#open-chat/${ cardStateMock.id }-${ cardStateMock.hash }/`);
    });

    it('должен открыть чат после авторизации, если в урле есть хеш #open-chat/', () => {
        initialState = {
            ...initialState,
            user: _.cloneDeep(privateUserStateMock),
        };

        shallowRenderOpenChatByOffer();
        // эмуляция бага vertis-chat
        global.vertis_chat._fns.forEach((fn) => fn());
        global.vertis_chat._fns.forEach((fn) => fn());
        expect(global.vertis_chat.open_chat_for_offer).toHaveBeenCalledTimes(1);
        expect(global.vertis_chat.open_chat_for_offer).toHaveBeenCalledWith(
            'cars',
            `${ cardStateMock.id }-${ cardStateMock.hash }`,
            {
                startCall: [
                    { name: 'CONTACT_CARS' },
                    { name: 'CONTACT_CARS_DESKTOP' },
                    { name: 'CONTACT_CARS_REGULAR' },
                    { name: 'CONTACT_CARS_PHONE' },
                    { name: 'PHONE_ALL_CARS2_PRICE-500-1500' },
                    { name: 'CONTACT_CARS_PHONE_USED' },
                    { name: 'ym-send-message' },
                    { name: 'MAUTORU_PHONE_CARS_SHOW' },
                ],
                startDialog: [
                    { name: 'CONTACT_CARS' },
                    { name: 'CONTACT_CARS_DESKTOP' },
                    { name: 'CONTACT_CARS_REGULAR' },
                    { name: 'CHAT_START_DIALOG_CARS_ALL_ALL' },
                    { name: 'PHONE_ALL_CARS2_PRICE-500-1500' },
                    { name: 'CHAT_START_DIALOG_CARS_ALL_USED' },
                    { name: 'CHAT_START_DIALOG_DESKTOP' },
                    { name: 'CHAT_START_DIALOG_REGULAR' },
                ],
                startDialogGoogle360: expect.any(Function),
            },
            { send_preset_text: '' },
        );
    });

    it('должен открыть чат после авторизации, если в урле есть хеш #open-chat/ и имя пресета', () => {
        initialState = {
            ...initialState,
            user: _.cloneDeep(privateUserStateMock),
        };
        props.presetName = 'change';
        global.location.href = new URL(`https://auto.ru/#open-chat/${ cardStateMock.id }-${ cardStateMock.hash }/change`);

        shallowRenderOpenChatByOffer();
        // эмуляция бага vertis-chat
        global.vertis_chat._fns.forEach((fn) => fn());
        global.vertis_chat._fns.forEach((fn) => fn());
        expect(global.vertis_chat.open_chat_for_offer).toHaveBeenCalledTimes(1);
        expect(global.vertis_chat.open_chat_for_offer).toHaveBeenCalledWith(
            'cars',
            `${ cardStateMock.id }-${ cardStateMock.hash }`,
            {
                startCall: [
                    { name: 'CONTACT_CARS' },
                    { name: 'CONTACT_CARS_DESKTOP' },
                    { name: 'CONTACT_CARS_REGULAR' },
                    { name: 'CONTACT_CARS_PHONE' },
                    { name: 'PHONE_ALL_CARS2_PRICE-500-1500' },
                    { name: 'CONTACT_CARS_PHONE_USED' },
                    { name: 'ym-send-message' },
                    { name: 'MAUTORU_PHONE_CARS_SHOW' },
                ],
                startDialog: [
                    { name: 'CONTACT_CARS' },
                    { name: 'CONTACT_CARS_DESKTOP' },
                    { name: 'CONTACT_CARS_REGULAR' },
                    { name: 'CHAT_START_DIALOG_CARS_ALL_ALL' },
                    { name: 'PHONE_ALL_CARS2_PRICE-500-1500' },
                    { name: 'CHAT_START_DIALOG_CARS_ALL_USED' },
                    { name: 'CHAT_START_DIALOG_DESKTOP' },
                    { name: 'CHAT_START_DIALOG_REGULAR' },
                ],
                startDialogGoogle360: expect.any(Function),
            },
            { send_preset_text: 'change' },
        );
    });

    it('не должен открыть чат без авторизации, если в урле есть хеш #open-chat/', () => {
        initialState = {
            ...initialState,
            user: _.cloneDeep(withoutAuth),
        };

        shallowRenderOpenChatByOffer();
        // эмуляция бага vertis-chat
        global.vertis_chat._fns.forEach((fn) => fn());
        global.vertis_chat._fns.forEach((fn) => fn());
        expect(global.vertis_chat.open_chat_for_offer).toHaveBeenCalledTimes(0);
    });

    it('не должен открыть чат после авторизации, если в урле есть хеш #open-chat/, но id от другого объявления', () => {
        global.location.href = new URL(`https://auto.ru/#open-chat/${ cardStateMock.id }-abc`);
        initialState = {
            ...initialState,
            user: _.cloneDeep(withoutAuth),
        };

        shallowRenderOpenChatByOffer();
        // эмуляция бага vertis-chat
        global.vertis_chat._fns.forEach((fn) => fn());
        global.vertis_chat._fns.forEach((fn) => fn());
        expect(global.vertis_chat.open_chat_for_offer).toHaveBeenCalledTimes(0);
    });
});

describe('event log', () => {
    it('должен логировать', () => {
        initialState = {
            ...initialState,
            user: _.cloneDeep(privateUserStateMock),
        };

        const wrapper = shallowRenderOpenChatByOffer();
        wrapper.simulate('click');

        expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
        expect(statApi.logImmediately.mock.calls[0][0]).toMatchSnapshot();
    });

    it('не должен логировать, если есть disableEventsLog', () => {
        initialState = {
            ...initialState,
            user: _.cloneDeep(privateUserStateMock),
        };

        props = {
            ...props,
            disableEventsLog: true,
        };

        const wrapper = shallowRenderOpenChatByOffer();
        wrapper.simulate('click');

        expect(statApi.logImmediately).not.toHaveBeenCalled();
    });
});

it('отправит метрику если есть from=reseller_public', () => {
    initialState = {
        ...initialState,
        user: _.cloneDeep(privateUserStateMock),
        config: { data: { pageParams: { from: 'reseller_public' } } },
    };

    const wrapper = shallowRenderOpenChatByOffer();
    wrapper.simulate('click');

    expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
    expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'chat_open' ]);
    expect(context.metrika.sendPageEvent.mock.calls[1][0]).toEqual([ 'chat_open', 'from_reseller_public' ]);
});

function shallowRenderOpenChatByOffer() {
    store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <OpenChatByOffer store={ store } { ...props }>
                <div/>
            </OpenChatByOffer>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
