jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const React = require('react');
const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const getResource = require('auto-core/react/lib/gateApi').getResource;

const SaleOfferHideDialog = require('./SaleOfferHideDialog');

it('должен стриггерить правильные действия на offerHideSuccess', () => {
    const expectedAtions = [
        {
            payload: { offerId: '123-456', isUnfolded: false },
            type: 'SALES_SET_UNFOLDED_STATE',
        },
        {
            payload: { offer: { id: 'offer-response-id' }, offerID: '123-456' },
            type: 'SALES_UPDATE_OFFER',
        },
    ];

    const mockResponse = Promise.resolve({
        id: 'offer-response-id',
    });
    getResource.mockImplementation(() => mockResponse);

    const store = mockStore({
        state: { hideModalParams: { offerID: '123-456', category: 'cars' } },
        bunker: { bunkerKey: 'bunkerValue' },
        sales: { items: [ { saleId: '123-456' } ] },
    });
    const wrapper = shallow(<SaleOfferHideDialog store={ store }/>);
    wrapper.dive().simulate('offerHideSuccess');

    return mockResponse.then(() => {
        expect(store.getActions()).toEqual(expectedAtions);
    });
});
