/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
const getResource = require('auto-core/react/lib/gateApi').getResource;

const _ = require('lodash');
const React = require('react');
const { Provider } = require('react-redux');
const { mount, shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const userMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');

const SalesItemPrice = require('./SalesItemPrice');

contextMock.logVasEvent = jest.fn();
const Context = createContextProvider(contextMock);

const offerWithoutVas = Object.assign(_.cloneDeep(offer), { services: [] });
offerWithoutVas.services = [];

const initialState = {
    bunker: getBunkerMock([ 'common/vas' ]),
    user: userMock,
};

it('должен правильно обработать редактирование цены', () => {
    return new Promise((done) => {
        const store = mockStore(initialState);
        const expectedRecourceCalls = [
            [
                'offerChangePrice',
                { category: 'cars', currency: 'RUR', offerID: '1085562758-1970f439', price: 855001 },
            ],
            [
                'getUserOffer',
                { offerID: '1085562758-1970f439', category: 'cars' },
            ],
        ];
        const expectedActions = [
            {
                payload: { currency: 'RUR', offerID: '1085562758-1970f439', price: 855001 },
                type: 'OFFER_PRICE_UPDATE',
            },
            {
                payload: { offer: { id: 'offer-response-id' }, offerID: '1085562758-1970f439' },
                type: 'SALES_UPDATE_OFFER',
            },
            {
                payload: { message: 'Цена изменена', view: 'success' },
                type: 'NOTIFIER_SHOW_MESSAGE',
            },
        ];
        const wrapper = shallow(<SalesItemPrice offer={ offerWithoutVas }/>, { context: { ...contextMock, store } }).dive();
        getResource.mockImplementation(jest.fn(() => Promise.resolve({ id: 'offer-response-id' })));
        wrapper.find('TextInput').simulate('change', '855001');
        wrapper.find('Button').simulate('click');
        setTimeout(() => {
            try {
                expect(getResource.mock.calls).toEqual(expectedRecourceCalls);
                expect(store.getActions()).toEqual(expectedActions);
                done();
            } catch (e) {
                done(e);
            }
        }, 1000);
    });
});

it('должен правильно отображать валюту', () => {
    const store = mockStore(initialState);
    const offerUsdPrice = _.cloneDeep(offerWithoutVas);
    offerUsdPrice.price_info.currency = 'USD';
    const wrapper = shallow(<SalesItemPrice offer={ offerUsdPrice }/>, { context: { ...contextMock, store } }).dive();
    wrapper.setProps({ offer: { category: 'CARS', price: { currency: 'USD' } } });
    expect(wrapper.find('.SalesItemPrice__currency').children().matchesElement('$')).toBe(true);
});

it('integration price change', () => {
    const store = mockStore(initialState);
    const wrapper = mount(
        <Context>
            <Provider store={ store }>
                <SalesItemPrice offer={ offerWithoutVas }/>
            </Provider>
        </Context >,
    );
    expect(wrapper.find('input').props().value).toBe('855 000');
    expect(wrapper.find('button').hasClass('SalesItemPrice__save')).toBe(false);
    wrapper.find('input').simulate('change', { target: { value: '855001' } });
    expect(wrapper.find('button').hasClass('SalesItemPrice__save')).toBe(true);
});
