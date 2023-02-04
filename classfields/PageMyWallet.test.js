const _ = require('lodash');
const React = require('react');
const PageMyWallet = require('./PageMyWallet');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

let context;

beforeEach(() => {
    context = _.cloneDeep(contextMock);
});

it('правильно рисует компонент', () => {
    const page = shallowRenderPageMyWallet();

    expect(shallowToJson(page)).toMatchSnapshot();
});

function shallowRenderPageMyWallet() {
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <PageMyWallet/>
        </ContextProvider>,
    );

    return wrapper.dive();
}
