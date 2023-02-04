const React = require('react');
const { shallow } = require('enzyme');

const PlacementAutoProlongationExpireNotice = require('./PlacementAutoProlongationExpireNotice');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cardStateMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

let props;

beforeEach(() => {
    props = {
        offer: cloneOfferWithHelpers(cardStateMock).withCustomVas({
            service: 'all_sale_activate',
            auto_prolong_price: 999,
            price: 1777,
            days: 7,
            prolongation_forced_not_togglable: true,
            prolongation_interval_will_expire: '2020-03-18T13:55:33Z',
        }).value(),
        from: 'card-vas',
        currentUrl: 'my-current-url',
        onActivateButtonClick: jest.fn(),
        onTimerFinish: jest.fn(),
    };

    contextMock.metrika.sendParams.mockClear();
});

it('отправит вас лог при маунте', () => {
    shallowRenderComponent({ props });

    expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.logVasEvent.mock.calls[0]).toMatchSnapshot();
});

describe('при клике на кнопку', () => {
    beforeEach(() => {
        const page = shallowRenderComponent({ props });
        const button = page.find('.PlacementAutoProlongationExpireNotice__button');
        button.simulate('click');
    });

    it('залогирует клик', () => {
        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.logVasEvent.mock.calls[1]).toMatchSnapshot();
    });

    it('дернет проп', () => {
        expect(props.onActivateButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onActivateButtonClick.mock.calls[0]).toMatchSnapshot();
    });
});

describe('когда выйдет таймер', () => {
    let page;

    beforeEach(() => {
        page = shallowRenderComponent({ props });
        const timer = page.find('Timer');
        timer.simulate('timerFinish');
    });

    it('скроет компонент', () => {
        expect(page.isEmptyRender()).toBe(true);
    });

    it('дернет проп', () => {
        expect(props.onTimerFinish).toHaveBeenCalledTimes(1);
    });
});

function shallowRenderComponent({ props }) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <PlacementAutoProlongationExpireNotice { ...props }/>
        </ContextProvider>,
    ).dive();
}
