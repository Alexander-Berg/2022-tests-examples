jest.mock('auto-core/lib/marketing', () => ({
    getDataFromReact: () => ({}),
    sendEvents: jest.fn(),
}));

const _ = require('lodash');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const sendMarketingEvent = require('./sendMarketingEvent');
const marketingMock = require('auto-core/lib/marketing');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const store = mockStore({
    config: {
        data: { pageType: 'card' },
    },
});

let offer;

beforeEach(() => {
    offer = _.cloneDeep(offerMock);
});

it('должен отправить цели для бу частника', () => {

    store.dispatch(sendMarketingEvent(
        [ { counter: 'adwords', type: 'conversionPhoneСM360' } ],
        offer,
        contextMock.metrika,
    ));

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('GOOGLE_CONVERSION_CODE');
    expect(marketingMock.sendEvents).toHaveBeenCalledWith({
        cost: 0,
        sellerType: 'used',
    },
    [
        { counter: 'adwords', type: 'conversionPhoneСM360' },
        { counter: 'adwords', type: 'conversionUsed' },
        { counter: 'adwords', type: 'contact_view_mgcom' },
        { counter: 'adwords', type: 'contact_view_mgcom_used' },
    ],
    );
});

it('должен отправить цели для бу диллера', () => {
    offer.seller_type = 'COMMERCIAL';

    store.dispatch(sendMarketingEvent(
        [ { counter: 'adwords', type: 'conversionPhoneСM360' } ],
        offer,
        contextMock.metrika,
    ));

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('GOOGLE_CONVERSION_CODE');
    expect(marketingMock.sendEvents).toHaveBeenCalledWith({
        cost: 0,
        sellerType: 'dealerbu',
    },
    [
        { counter: 'adwords', type: 'conversionPhoneСM360' },
        { counter: 'adwords', type: 'conversionUsed' },
        { counter: 'adwords', type: 'contact_view_mgcom' },
        { counter: 'adwords', type: 'contact_view_mgcom_dealer_used' },
    ],
    );
});

it('должен отправить цели для новых авто', () => {
    offer.section = 'new';

    store.dispatch(sendMarketingEvent(
        [ { counter: 'adwords', type: 'conversionPhoneСM360' } ],
        offer,
        contextMock.metrika,
    ));

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('GOOGLE_CONVERSION_CODE');
    expect(marketingMock.sendEvents).toHaveBeenCalledWith({
        cost: 0,
        sellerType: 'new',
    },
    [
        { counter: 'adwords', type: 'conversionPhoneСM360' },
        { counter: 'adwords', type: 'conversionNew' },
        { counter: 'adwords', type: 'contact_view_mgcom' },
    ],
    );
});

it('не должен добавлять цели для офферов', () => {
    store.dispatch(sendMarketingEvent(
        [ { counter: 'adwords', type: 'page_view' } ],
    ));

    expect(marketingMock.sendEvents).toHaveBeenCalledWith({},
        [
            { counter: 'adwords', type: 'page_view' },
        ],
    );
});
