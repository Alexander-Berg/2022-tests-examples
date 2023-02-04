/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const getResource = require('auto-core/react/lib/gateApi').getResource;

const ModalDialogHideOffer = require('./ModalDialogHideOffer');

const storeMock = {
    state: {
        activeDialogParams: {
            offerID: '123-456',
            category: 'testCategory',
        },
    },
};

let store;
let context;

beforeEach(() => {
    store = mockStore(storeMock);
    context = contextMock;
});

it('должен сразу показать синюю кнопку "Снять" после выбора второстепенной причины снятия', () => {
    const wrapper = shallow(<ModalDialogHideOffer store={ store }/>, { context: context }).dive();
    wrapper.setState({ reason: 'RETHINK' });
    expect(wrapper.find('Button').props().color).toEqual('blue');
});

it('должен вызвать правильный экшен после указания причины снятия и клику на кнопку "Снять"', () => {
    getResource.mockImplementation(() => Promise.resolve({ status: 'SUCSESS' }));
    const expectedActions = [
        {
            type: 'CLOSE_MODAL_DIALOG',
        },
        {
            payload: { offerID: '123-456', isLoading: true },
            type: 'SALES_SET_LOADING_STATE',
        },
    ];
    const wrapper = shallow(<ModalDialogHideOffer store={ store }/>, { context: context }).dive();
    getResource.mockImplementation(() => Promise.resolve({ status: 'SUCCESS' }));
    wrapper.setState({ reason: 'RETHINK' });
    wrapper.find('Button').simulate('click');
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен обновить чекбокс и state, если проставили галку "Много спама"', () => {
    const wrapper = shallow(<ModalDialogHideOffer store={ store }/>, { context: context }).dive();
    expect(wrapper.state().manySpamCalls).toBe(false);
    wrapper.find('Checkbox').simulate('check', true);
    expect(wrapper.state().manySpamCalls).toBe(true);
});

it('должен вызывать getResource с правильными параметрами', () => {
    const resourcePromise = Promise.resolve({ status: 'SUCCESS' });
    getResource.mockImplementation(() => resourcePromise);

    const expectedParams = {
        add_to_white_list: false,
        offerID: '123-456',
        category: 'testCategory',
        reason: 'SOLD_ON_AUTORU',
        buyer_phone: '+79999999999',
        many_spam_calls: true,
    };

    const wrapper = shallow(<ModalDialogHideOffer store={ store }/>, { context: context }).dive();
    expect(getResource.mock.calls).toHaveLength(0);
    wrapper.find('Checkbox').simulate('check', true);
    wrapper.setState({
        reason: 'SOLD_ON_AUTORU',
        approvedPhone: 'otherPhone',
        otherPhone: { value: '+7 999 999 99-99' },
    });
    wrapper.find('Button').simulate('click');

    return resourcePromise
        .then(() => {
            expect(getResource.mock.calls).toHaveLength(1);
            expect(getResource).toHaveBeenCalledWith('offerHide', expectedParams);
        });
});
