jest.mock('auto-core/lib/util/vas/logger', () => {
    return {
        'default': jest.fn(() => ({
            logVasEvent: jest.fn(),
        })),
    };
});

const _ = require('lodash');
const React = require('react');
const Sales = require('./Sales');
const { shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const salesMock = require('auto-core/react/dataDomain/sales/mocks').default;
const VasLogger = require('auto-core/lib/util/vas/logger').default;

let initialState;
let defaultContext;

beforeEach(() => {
    initialState = {
        sales: salesMock.value(),
    };

    defaultContext = _.cloneDeep(contextMock);
    defaultContext.pageParams = {
        category: 'cars',
    };

    VasLogger.mockClear();
});

it('записывает в контекст логгер васов с дефолтным параматром', () => {
    const page = shallowRenderSales(defaultContext);

    expect(VasLogger).toHaveBeenCalledTimes(1);
    expect(VasLogger).toHaveBeenCalledWith(defaultContext.metrika, { from: 'new-lk-tab' });
    expect(page.instance().getChildContext()).toHaveProperty('logVasEvent');
});

describe('эксп AUTORUFRONT-19219_new_lk_and_vas_block_design', () => {
    it('отрендерит компонент UserOffersWithSidebar, если нет экспа', () => {
        const wrapper = shallowRenderSales(defaultContext);

        expect(wrapper.find('Connect(UserOffersWithSidebar)')).toExist();
        expect(wrapper.find('Connect(SalesNewDesign)')).not.toExist();
    });

    it('отрендерит компонент SalesNewDesign в экспе', () => {
        const wrapper = shallowRenderSales({
            ...contextMock,
            hasExperiment: jest.fn(exp => exp === 'AUTORUFRONT-19219_new_lk_and_vas_block_design'),
        });

        expect(wrapper.find('Connect(UserOffersWithSidebar)')).not.toExist();
        expect(wrapper.find('Connect(SalesNewDesign)')).toExist();
    });
});

function shallowRenderSales(context) {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <Sales store={ store }/>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
