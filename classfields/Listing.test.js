const React = require('react');
const { shallow } = require('enzyme');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const Listing = require('./Listing');

it('покажет заглушку, если нет офферов', () => {
    const tree = shallowRenderComponent({ children: [], checkedSalesIds: {}, loadAction: () => {} });

    expect(tree.find('ListingEmpty')).toExist();
});

function shallowRenderComponent(props = {}) {
    return shallow(
        <Listing { ...props }/>,
        { context: contextMock },
    );
}
