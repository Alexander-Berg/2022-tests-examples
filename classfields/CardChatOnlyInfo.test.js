const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CardChatOnlyInfo = require('./CardChatOnlyInfo');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

// Offer mock helpers
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const withChatOnly = require('autoru-frontend/mockData/state/helpers/offer/withChatOnly');

let props;
let context;

beforeEach(() => {
    props = {
        offer: cloneOfferWithHelpers(offerMock)
            .withIsOwner()
            .withChatOnly()
            .value(),
        className: 'Foo',
    };
    context = _.cloneDeep(contextMock);

    context.metrika.sendParams.mockClear();
});

describe('ничего не нарисует', () => {
    it('если нет флага "только чат"', () => {
        props.offer = withChatOnly(props.offer, false);
        const page = shallowRenderComponent({ props, context });

        expect(page.html()).toBeNull();
    });

    it('если есть флаг "только чат" но я не хозяин', () => {
        props.offer = cloneOfferWithHelpers(props.offer)
            .withIsOwner(false)
            .withChatOnly()
            .value();
        const page = shallowRenderComponent({ props, context });

        expect(page.html()).toBeNull();
    });
});

it('правильно рисует компонент если пришел флаг', () => {
    const page = shallowRenderComponent({ props, context });

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('отправит метрику при открытии тултипа', () => {
    const page = shallowRenderComponent({ props, context });
    const tooltip = page.find('HoveredTooltip');

    tooltip.simulate('open');

    expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(context.metrika.sendParams).toHaveBeenCalledWith([ 'Dont_call_me_settings', 'hint_pop_up' ]);
});

function shallowRenderComponent({ context, props }) {
    const ContextProvider = createContextProvider(context);

    return shallow(
        <ContextProvider>
            <CardChatOnlyInfo { ...props }/>
        </ContextProvider>,
    ).dive();
}
