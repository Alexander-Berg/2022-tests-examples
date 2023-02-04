const React = require('react');
const { shallow } = require('enzyme');

const FIELD_NAMES = require('www-cabinet/data/calls/filter-call-field-names.json');

const CallsFiltersTags = require('./CallsFiltersTags');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

it('должен убирать тег из списка при нажатии при действии remove', () => {
    const onChange = jest.fn();
    const ContextProvider = createContextProvider(contextMock);

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersTags
                tags={ [ 'tag_1', 'tag_2' ] }
                onChange={ onChange }
            />
        </ContextProvider>,
    );

    tree.dive().find('.CallsFiltersTags__icon').at(0).simulate('click');

    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.TAG]: [ 'tag_2' ] });
});
