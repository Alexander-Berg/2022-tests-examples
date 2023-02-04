const React = require('react');
const { shallow } = require('enzyme');
// const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

// const contextMock = require('autoru-frontend/mocks/contextMock').default;

// const Context = createContextProvider(contextMock);

const GenericItemsList = require('./GenericItemsList');

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const getResource = require('auto-core/react/lib/gateApi').getResource;

it('должен загрузить данные на маунт', () => {
    getResource.mockImplementation(() => Promise.resolve({ status: 'SUCSESS' }));
    shallow(
        <GenericItemsList resourceName="test" resourceParams={{ foo: 'bar' }}/>,
    );
    expect(getResource).toHaveBeenCalledWith('test', { foo: 'bar' });
});

it('должен перезапросить данные с новым resourceName', () => {
    getResource.mockImplementation(() => Promise.resolve({ status: 'SUCSESS' }));
    const tree = shallow(
        <GenericItemsList resourceName="test" resourceParams={{ foo: 'bar' }}/>,
    );
    tree.setProps({ resourceName: 'test2' });
    expect(getResource.mock.calls[0]).toEqual([ 'test', { foo: 'bar' } ]);
    expect(getResource.mock.calls[1]).toEqual([ 'test2', { foo: 'bar' } ]);
});

it('должен перезапросить данные с новым resourceParams', () => {
    getResource.mockImplementation(() => Promise.resolve({ status: 'SUCSESS' }));
    const tree = shallow(
        <GenericItemsList resourceName="test" resourceParams={{ foo: 'bar' }}/>,
    );
    tree.setProps({ resourceParams: { foo: 'bar2' } });
    expect(getResource.mock.calls[0]).toEqual([ 'test', { foo: 'bar' } ]);
    expect(getResource.mock.calls[1]).toEqual([ 'test', { foo: 'bar2' } ]);
});
