const _ = require('lodash');
const React = require('react');
const SalesItemVASColor = require('./SalesItemVASColor');

const { shallow } = require('enzyme');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const { InView } = require('react-intersection-observer');

let props;

let context;
let vasLogParams;
contextMock.logVasEvent = jest.fn((params) => {
    vasLogParams = params;
});

beforeEach(() => {
    props = {
        offerID: cardMock.saleId,
        serviceInfo: _.find(cardMock.service_prices, { service: 'all_sale_color' }),
        onSubmit: jest.fn(),
        category: 'cars',
    };

    context = _.cloneDeep(contextMock);

    context.logVasEvent.mockClear();
});

it('правильно логирует событие показа если кнопка в поле видимости', () => {
    const page = shallowRenderComponent();
    const observer = page.find(InView);
    observer.simulate('change', true);

    expect(context.logVasEvent).toHaveBeenCalledTimes(1);
    expect(vasLogParams).toMatchSnapshot();
});

it('не логирует событие показа если кнопка вне поля видимости', () => {
    const page = shallowRenderComponent();
    const observer = page.find(InView);
    observer.simulate('change', false);

    expect(context.logVasEvent).toHaveBeenCalledTimes(0);
});

function shallowRenderComponent() {
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <SalesItemVASColor { ...props }/>
        </ContextProvider>,
    );

    return wrapper.dive();
}
