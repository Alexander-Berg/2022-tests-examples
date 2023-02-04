jest.mock('auto-core/react/dataDomain/state/actions/sameOfferDialogClose');
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

const _ = require('lodash');
const React = require('react');

const Button = require('auto-core/react/components/islands/Button');
const SameOfferDialog = require('./SameOfferDialog');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const stateMock = require('autoru-frontend/mockData/state/state.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const closeSameOfferDialog = require('auto-core/react/dataDomain/state/actions/sameOfferDialogClose');
const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');

let initialState;
let props;
let context;

closeSameOfferDialog.mockImplementation(() => jest.fn());

let paymentModalParams;
paymentModalOpen.mockImplementation((params) => {
    paymentModalParams = params;
    return jest.fn();
});

let vasLogParams;
contextMock.logVasEvent = jest.fn((params) => {
    vasLogParams = params;
});

beforeEach(() => {
    initialState = {
        state: {
            sameOfferModalOpened: true,
            sameOfferModalParams: _.cloneDeep(stateMock.sameOfferModalParams),
        },
    };

    props = {};

    context = _.cloneDeep(contextMock);

    closeSameOfferDialog.mockClear();
    paymentModalOpen.mockClear();
    context.logVasEvent.mockClear();
});

it('правильно рисует компонент', () => {
    // для отрисовки конкретно этого компонента похожий оффер не важен, поэтому делаем так, чтобы не раздувать снэпшот ненужными данными
    initialState.state.sameOfferModalParams.similarOffer = { foo: 'тут должен быть объект с похожим оффером' };
    const page = shallowRenderOfferActivateDialog();

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно логирует показы васа при рендере', () => {
    // тут все несколько сложнее: сначала попап всегда закрыт, а потом мы его открываем
    // вот тогда то и отправляется лог
    initialState.state.sameOfferModalOpened = false;
    const page = shallowRenderOfferActivateDialog();
    page.setProps({ isOpened: true, params: stateMock.sameOfferModalParams });

    expect(context.logVasEvent).toHaveBeenCalledTimes(1);
    expect(vasLogParams).toMatchSnapshot();
});

describe('при клике на кнопку "активировать новый"', () => {
    beforeEach(() => {
        const page = shallowRenderOfferActivateDialog();
        const button = page.find('.SameOfferDialog__Button').find({ color: Button.COLOR.BLUE });
        button.simulate('click');
    });

    it('закроет модал', () => {
        expect(closeSameOfferDialog).toHaveBeenCalledTimes(1);
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
            <SameOfferDialog { ...props } store={ store }/>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
