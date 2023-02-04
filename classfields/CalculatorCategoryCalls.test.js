const React = require('react');
const { shallow } = require('enzyme');

const { Provider } = require('react-redux');

const createLinkMock = require('autoru-frontend/mocks/createLinkMock').default;
const contextMock = {
    linkCabinet: createLinkMock('linkCabinet'),
};
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const CollapseCard = require('www-cabinet/react/components/CollapseCard');

const CalculatorCategoryCalls = require('./CalculatorCategoryCalls');

const defaultState = {
    config: {
        client: {
            salon: {
                dealership: [],
            },
        },
    },
    bunker: {
        calculator: {
            callsLimitsDescription: '',
            minimalDepositDescription: '',
        },
        tariffs_info: {
            CARS_NEW: '',
        },
    },
    user: { data: {} },
};

const defaultProps = {
    category: {
        category: 'CARS',
        section: [ 'NEW' ],
        enabled: false,
        calls: {
            price: 1500,
        },
    },
    counts: {
        CARS_NEW: {},
    },
    hasRightsChangeTariff: true,
    summaryInfo: '',
};

describe('если не указано дилерство', () => {

    it('задизейблит кнопку активации тарифа', () => {
        const tree = shallowRenderComponent();

        expect(tree.find(CollapseCard).prop('buttonDisabled')).toEqual(true);
    });

    it('покажет предупреждение о необходимости указания дилерства для активации тарифа', () => {
        const tree = shallowRenderComponent();

        expect(tree.find('.CalculatorCategoryCalls__disclaimer')).toExist();
    });
});

function shallowRenderComponent(state = defaultState, props = defaultProps) {
    const store = mockStore(state);
    const wrapper = shallow(
        <Provider store={ store }>
            <CalculatorCategoryCalls { ...props }/>
        </Provider>,
        { context: contextMock },
    );
    return wrapper.dive().dive();
}
