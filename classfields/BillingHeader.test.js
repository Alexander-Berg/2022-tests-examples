const _ = require('lodash');
const React = require('react');
const BillingHeader = require('./BillingHeader');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ContextProvider = createContextProvider(contextMock);

const DEFAULT_PROPS = {
    cost: 90,
    baseCost: 90,
    isMobile: false,
    shouldShowCostInfo: true,
    title: 'Турбо-продажа',
};

let props;

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
});

it('правильно рисует компонент для десктопа', () => {
    props.isMobile = false;
    const wrapper = shallow(<ContextProvider><BillingHeader { ...props }/></ContextProvider>).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('правильно рисует компонент для мобилки', () => {
    props.isMobile = true;
    const wrapper = shallow(<ContextProvider><BillingHeader { ...props }/></ContextProvider>).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('правильно рисует компонент если оплачивается 1 сервис', () => {
    const wrapper = shallow(<ContextProvider><BillingHeader { ...props }/></ContextProvider>).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('покажет скидку если baseCost больше cost', () => {
    props.baseCost = 101;
    const wrapper = shallow(<ContextProvider><BillingHeader { ...props }/></ContextProvider>).dive();
    const baseCostEl = wrapper.find('.BillingHeader__baseCost');

    expect(shallowToJson(baseCostEl)).toMatchSnapshot();
});

it('не покажет скидку если baseCost равен 0', () => {
    props.baseCost = 0;
    const wrapper = shallow(<ContextProvider><BillingHeader { ...props }/></ContextProvider>).dive();
    const subtitleEl = wrapper.find('.BillingHeader__price');

    expect(shallowToJson(subtitleEl)).toMatchSnapshot();
});

it('не покажет информацию о стоимости если не передан флаг', () => {
    props.shouldShowCostInfo = false;
    const wrapper = shallow(<ContextProvider><BillingHeader { ...props }/></ContextProvider>).dive();
    const subtitleEl = wrapper.find('.BillingHeader__subtitle');

    expect(subtitleEl).toHaveLength(0);
});
