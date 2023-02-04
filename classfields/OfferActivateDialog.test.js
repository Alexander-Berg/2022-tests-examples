jest.mock('auto-core/react/dataDomain/state/actions/offerActivateDialogClose');
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

const _ = require('lodash');
const React = require('react');
const OfferActivateDialog = require('./OfferActivateDialog');
const Button = require('auto-core/react/components/islands/Button');
const Modal = require('auto-core/react/components/islands/Modal');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const stateMock = require('autoru-frontend/mockData/state/state.mock');
const closeActivateModal = require('auto-core/react/dataDomain/state/actions/offerActivateDialogClose');
const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

let initialState;
let props;
let vasLogParams;

closeActivateModal.mockImplementation(() => jest.fn());

let paymentModalParams;
paymentModalOpen.mockImplementation((params) => {
    paymentModalParams = params;
    return jest.fn();
});

let context;
contextMock.logVasEvent = jest.fn((params) => {
    vasLogParams = params;
});

beforeEach(() => {
    initialState = {
        state: {
            activateOfferDialogParams: _.cloneDeep(stateMock.activateOfferDialogParams),
        },
    };
    initialState.state.activateOfferDialogParams.isOpened = true;

    props = {
        offer: _.cloneDeep(offerMock),
    };

    context = _.cloneDeep(contextMock);
    context.link = jest.fn((route, params) => `link to "${ route }" with params: ${ JSON.stringify(params) }`);

    closeActivateModal.mockClear();
    paymentModalOpen.mockClear();
    context.logVasEvent.mockClear();
    context.metrika.sendParams.mockClear();
});

it('правильно рисует компонент если закончилась квота', () => {
    initialState.state.activateOfferDialogParams.paidReason = 'FREE_LIMIT';
    const page = shallowRenderOfferActivateDialog();

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент если машинка премиальная', () => {
    initialState.state.activateOfferDialogParams.paidReason = 'PREMIUM_OFFER';
    const page = shallowRenderOfferActivateDialog();

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент если машинка попала под условия первого платного размещения', () => {
    initialState.state.activateOfferDialogParams.paidReason = 'PAYMENT_GROUP';
    const page = shallowRenderOfferActivateDialog();

    expect(shallowToJson(page)).toMatchSnapshot();
});

describe('если юзер перекуп с семью днями', () => {
    beforeEach(() => {
        initialState.state.activateOfferDialogParams.paidReason = 'FREE_LIMIT';
        props.offer.service_prices = props.offer.service_prices.map((service) => {
            if (service.service !== 'all_sale_activate') {
                return service;
            }
            return {
                ...service,
                prolongation_forced_not_togglable: true,
                auto_prolong_price: 1499,
                days: 7,
            };
        });
    });

    it('правильно рисует компонент', () => {
        const page = shallowRenderOfferActivateDialog();

        expect(shallowToJson(page)).toMatchSnapshot();
    });

    it('правильно отправляет метрику при открытии попапа', () => {
        // тут все несколько сложнее: сначала попап всегда закрыт, а потом мы его открываем
        // вот тогда то и отправляется метрика
        initialState.state.activateOfferDialogParams.isOpened = false;
        const page = shallowRenderOfferActivateDialog();
        page.setProps({ params: { isOpened: true, from: 'new-lk-tab' } });

        expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendParams).toHaveBeenCalledWith([ '7days-placement', 'landing-page', 'shows', 'from-lk' ]);
    });

    it('правильно отправляет метрику при клике на ссылку', () => {
        const page = shallowRenderOfferActivateDialog();
        const link = page.find('Link');
        link.simulate('click');

        expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendParams).toHaveBeenCalledWith([ '7days-placement', 'landing-page', 'clicks', 'from-lk' ]);
    });
});

it('правильно логирует показы васа при рендере', () => {
    // тут все несколько сложнее: сначала попап всегда закрыт, а потом мы его открываем
    // вот тогда то и отправляется лог
    initialState.state.activateOfferDialogParams.isOpened = false;
    const page = shallowRenderOfferActivateDialog();
    page.setProps({ params: { isOpened: true, from: 'new-lk-tab' } });

    expect(context.logVasEvent).toHaveBeenCalledTimes(1);
    expect(vasLogParams).toMatchSnapshot();
});

describe('вызывает экшен для закрытия модала', () => {
    let page;

    beforeEach(() => {
        page = shallowRenderOfferActivateDialog();
    });

    it('при нажатии мимо модала', () => {
        const modal = page.find(Modal);
        modal.simulate('requestHide');

        expect(closeActivateModal).toHaveBeenCalledTimes(1);
    });
});

describe('при клике на кнопку', () => {
    beforeEach(() => {
        const page = shallowRenderOfferActivateDialog();
        const button = page.find(Button);
        button.simulate('click');
    });

    it('закроет модал', () => {
        expect(closeActivateModal).toHaveBeenCalledTimes(1);
    });

    it('передаст правильные параметры в модал оплаты', () => {
        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalParams).toMatchSnapshot();
    });

    it('правильно залогирует событие клика', () => {
        expect(context.logVasEvent).toHaveBeenCalledTimes(1);
        expect(vasLogParams).toMatchSnapshot();
    });
});

function shallowRenderOfferActivateDialog() {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <OfferActivateDialog { ...props } store={ store }/>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
