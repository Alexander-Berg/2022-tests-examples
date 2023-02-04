/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');
jest.mock('auto-core/react/dataDomain/state/actions/sellerPopupOpen', () => {
    return jest.fn(() => ({ type: 'sellerPopupOpen_action' }));
});

const React = require('react');
const { shallow } = require('enzyme');

const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const userWithAuthMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock.js');

const statApi = require('auto-core/lib/event-log/statApi').default;

const sellerPopupOpen = require('auto-core/react/dataDomain/state/actions/sellerPopupOpen');
const CardOwner = require('./CardOwner');

it('должен отрендерить один телефон без времени', () => {
    const map = {};

    window.addEventListener = jest.fn((event, cb) => {
        map[event] = cb;
    });

    const wrapper = shallow(
        <CardOwner canShowSafeDealBlock={ false } offer={ offer }/>,
        { context: { ...contextMock, store: mockStore() } },
    ).dive();

    wrapper.instance().sellerPopupOpen = jest.fn();
    map.message({
        data: { source: 'chat', action: 'open_phone_popup' },
    });
    expect(sellerPopupOpen).toHaveBeenCalled();
});

it('должен отправить событие chat_init_event во фронтлог', () => {
    window.vertis_chat = { open_chat_for_offer: jest.fn() };

    const store = {
        bunker: getBunkerMock([ 'common/metrics' ]),
        card: offer,
        config: configStateMock.withPageType('card').value(),
        user: userWithAuthMock,
    };

    const wrapper = shallow(
        <CardOwner canShowSafeDealBlock={ false } offer={ offer }/>,
        { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    wrapper.find('Connect(OpenChatByOffer)').dive().dive().instance().onClick();

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        chat_init_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_CARD',
            context_page: 'PAGE_CARD',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});
