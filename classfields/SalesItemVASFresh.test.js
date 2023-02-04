const React = require('react');
const SalesItemVASFresh = require('./SalesItemVASFresh');

const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const userMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const userWithTiedCardsMock = require('auto-core/react/dataDomain/user/mocks/withTiedCards.mock');
const stateMock = require('autoru-frontend/mockData/state/state.mock');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const { InView } = require('react-intersection-observer');

let initialState;
let props;

beforeEach(() => {
    initialState = {
        state: {
            paymentModalResult: stateMock.paymentModalResult,
        },
        user: userMock,
    };

    props = {
        offer: cardMock,
        onSubmit: jest.fn(),
        onMouseEnter: jest.fn(),
        onMouseLeave: jest.fn(),
    };

    contextMock.logVasEvent.mockClear();
});

it('правильно логирует событие показа если кнопка в поле видимости', () => {
    const page = shallowRenderComponent();
    const observer = page.find(InView);
    observer.simulate('change', true);

    expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.logVasEvent.mock.calls[0][0]).toMatchSnapshot();
});

it('не логирует событие показа если кнопка вне поля видимости', () => {
    const page = shallowRenderComponent();
    const observer = page.find(InView);
    observer.simulate('change', false);

    expect(contextMock.logVasEvent).toHaveBeenCalledTimes(0);
});

describe('обернет кнопку в модал автоподнятия', () => {
    it('если автоподнятие включено', () => {
        props.offer = cloneOfferWithHelpers(cardMock).withServiceSchedule('all_sale_fresh').value();
        const page = shallowRenderComponent();
        const infoPopup = page.find('InfoPopup');

        expect(page.find('Connect(SaleServicesPopup)').isEmptyRender()).toBe(true);
        expect(infoPopup.isEmptyRender()).toBe(false);
        expect(infoPopup.prop('content').props.isActive).toBe(true);
    });

    it('если поднятие в поиске активно и у пользователя есть привязанные карты', () => {
        initialState.user = userWithTiedCardsMock;
        props.offer = cloneOfferWithHelpers(cardMock).withActiveVas([ 'all_sale_fresh' ]).value();
        const page = shallowRenderComponent();
        const infoPopup = page.find('InfoPopup');

        expect(page.find('Connect(SaleServicesPopup)').isEmptyRender()).toBe(true);
        expect(infoPopup.isEmptyRender()).toBe(false);
        expect(infoPopup.prop('content').props.isActive).toBe(false);
    });
});

it('обернет кнопку в инфо попап про поднятие в поиске', () => {
    const page = shallowRenderComponent();

    expect(page.find('Connect(SaleServicesPopup)').isEmptyRender()).toBe(false);
    expect(page.find('InfoPopup').isEmptyRender()).toBe(true);
});

function shallowRenderComponent() {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(contextMock);

    const wrapper = shallow(
        <ContextProvider>
            <SalesItemVASFresh { ...props } store={ store }/>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
