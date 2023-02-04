/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const userWithAuthMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock.js');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock.js');

const HistoryOfferContacts = require('./HistoryOfferContacts');
const statApi = require('auto-core/lib/event-log/statApi').default;

it('должен отправить событие chat_init_event во фронтлог', () => {
    window.vertis_chat = { open_chat_for_offer: jest.fn() };

    const card = _.cloneDeep(cardMock);
    card.additional_info = {};
    card.seller.phones = [];
    card.state.image_urls = [];

    const store = {
        bunker: getBunkerMock([ 'common/metrics' ]),
        config: configStateMock.withPageType('history-by-vin').value(),
        sendMarketingEvent: _.noop,
        state: {
            sellerPopup: {
                isOpened: false,
            },
        },
        user: userWithAuthMock,
    };

    const wrapper = shallow(
        <HistoryOfferContacts offer={ card } sendMarketingEvent={ jest.fn() }/>,
        { context: { ...contextMock, store: mockStore(store) } },
    );

    wrapper.find('Connect(OpenChatByOffer)').dive().dive().simulate('click');

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        chat_init_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_CONTACTS',
            context_page: 'PAGE_HISTORY',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});
