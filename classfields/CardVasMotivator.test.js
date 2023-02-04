const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CardVasMotivator = require('./CardVasMotivator');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cardStateMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const { defaultState: vasMotivatorDefaultStateMock } = require('../../../dataDomain/vasMotivator/mocks');

let props;
let context;
let initialState;

beforeEach(() => {
    initialState = {
        vasMotivator: _.cloneDeep(vasMotivatorDefaultStateMock),
        card: _.cloneDeep(cardStateMock),
    };
    props = {};
    context = _.cloneDeep(contextMock);

    context.metrika.sendParams.mockClear();
});

it('правильно рисует компонент первого типа', () => {
    initialState.vasMotivator.bannerType = 1;
    const page = shallowRenderComponent({ initialState, props, context });

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент второго типа', () => {
    initialState.vasMotivator.bannerType = 2;
    const page = shallowRenderComponent({ initialState, props, context });

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('ничего не нарисует если не передан тип баннера', () => {
    initialState.vasMotivator.bannerType = undefined;
    const page = shallowRenderComponent({ initialState, props, context });

    expect(page.html()).toBeNull();
});

it('при рендере отправит события в метрику', () => {
    shallowRenderComponent({ initialState, props, context });

    expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(context.metrika.sendParams).toHaveBeenCalledWith([ 'vas', 'cars', 'card', 'shows', 'promo' ]);
});

it('при клике на кнопку отправит события в метрику', () => {
    const page = shallowRenderComponent({ initialState, props, context });
    const button = page.find('Button');
    button.simulate('click');

    expect(context.metrika.sendParams).toHaveBeenCalledTimes(2);
    expect(context.metrika.sendParams).toHaveBeenCalledWith([ 'vas', 'cars', 'card', 'clicks', 'promo' ]);
});

function shallowRenderComponent({ context, initialState, props }) {
    const ContextProvider = createContextProvider(context);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <CardVasMotivator { ...props } store={ store }/>
        </ContextProvider>,
    ).dive().dive();
}
