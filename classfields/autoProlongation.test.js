jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const {
    addAutoProlongationToOffer,
    setAutoBoostSchedule,
    toggleAutoProlongationToTransaction,
} = require('./autoProlongation');

const getResource = require('auto-core/react/lib/gateApi').getResource;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const MockDate = require('mockdate');

const pageParamsState = {
    category: 'cars',
    salesmanDomain: 'autoru',
    offerId: '1234567-xxx',
};

let store;
let gateApiMock;

beforeEach(() => {
    gateApiMock = jest.fn();
    getResource.mockImplementation(gateApiMock);
});

describe('экшн "toggleAutoProlongationToTransaction"', () => {
    const paymentInfoState = {
        ticket_id: 'my-awesome-payment',
        salesman_domain: 'autoru',
    };

    beforeEach(() => {
        store = mockStore({
            billing: {
                paymentInfo: [ paymentInfoState ],
                selectedTicketId: 'my-awesome-payment',
            },
        });
    });

    it('вызовет правильный ресурс если передан флаг true', () => {
        store.dispatch(toggleAutoProlongationToTransaction(true));
        expect(gateApiMock).toHaveBeenCalledTimes(1);
        expect(getResource).toHaveBeenCalledWith('addAutoProlongToTransaction', {
            domain: paymentInfoState.salesman_domain,
            transactionId: paymentInfoState.ticket_id,
        });
    });

    it('вызовет правильный ресурс если передан флаг false', () => {
        store.dispatch(toggleAutoProlongationToTransaction(false));
        expect(gateApiMock).toHaveBeenCalledTimes(1);
        expect(getResource).toHaveBeenCalledWith('deleteAutoProlongFromTransaction', {
            domain: paymentInfoState.salesman_domain,
            transactionId: paymentInfoState.ticket_id,
        });
    });
});

it('экшн "addAutoProlongationToOffer" вызовет правильный ресурс с правильными параметрами', () => {
    const service = 'all_sale_toplist';
    store = mockStore({
        config: { data: { pageParams: pageParamsState } },
    });
    store.dispatch(addAutoProlongationToOffer(service));

    expect(gateApiMock).toHaveBeenCalledTimes(1);
    expect(getResource).toHaveBeenCalledWith('addAutoProlong', {
        domain: pageParamsState.salesmanDomain,
        category: pageParamsState.category,
        offerId: pageParamsState.offerId,
        product: service,
    });
});

it('экшн "setAutoBoostSchedule" вызовет правильный ресурс с правильными параметрами', () => {
    MockDate.set('2019-02-26T10:10:10.000+0300');
    const time = '17:00';
    store = mockStore({
        config: { data: { pageParams: pageParamsState } },
    });
    store.dispatch(setAutoBoostSchedule(time));

    expect(gateApiMock).toHaveBeenCalledTimes(1);
    expect(getResource).toHaveBeenCalledWith('putBillingSchedules', {
        category: pageParamsState.category,
        offerId: pageParamsState.offerId,
        schedule_type: 'ONCE_AT_TIME',
        time,
        timezone: '+03:00',
    });
});
