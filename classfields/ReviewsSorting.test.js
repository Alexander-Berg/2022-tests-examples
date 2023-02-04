const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const ReviewsSorting = require('./ReviewsSorting');

it('должен отрендерить селект сортировки', () => {
    const ContextProvider = createContextProvider(contextMock);

    const wrapper = shallow(
        <ContextProvider>
            <ReviewsSorting
                value="foo"
            />
        </ContextProvider>,
    ).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
