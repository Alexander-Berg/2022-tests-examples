const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');
const MockDate = require('mockdate');

const CallsFilters = require('./CallsFilters');

const filtersMock = require('www-cabinet/react/dataDomain/calls/mocks/withFilters.mock').filters;
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const FIELD_NAMES = require('www-cabinet/data/calls/filter-call-field-names.json');
const FILTER_CALLBACK_VALUES = require('www-cabinet/data/calls/filter-call-callback-values.json');

const baseProps = {
    breadcrumbs: breadcrumbsPublicApiMock.data,
    initialFilters: filtersMock,
    onChangeFilters: _.noop,
    requestExport: _.noop,
    getTags: _.noop,
    isAvailableOfferFilters: false,
};

beforeEach(() => {
    MockDate.set('2020-02-02');
});

afterEach(() => {
    MockDate.reset();
});

it('должен вызывать onChange после изменения', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <CallsFilters
                { ...baseProps }
                initialFilters={{}}
            />
        </ContextProvider>,
    );

    const instance = tree.dive().instance();

    instance.debouncedOnChangeFilters = jest.fn();

    instance.onChange({ [FIELD_NAMES.CALLBACK]: FILTER_CALLBACK_VALUES.CALLBACK_GROUP });

    expect(instance.debouncedOnChangeFilters).toHaveBeenCalledWith({
        [FIELD_NAMES.CALLBACK]: FILTER_CALLBACK_VALUES.CALLBACK_GROUP,
    }, undefined);
});

it('не должен вызывать onChange, если фильтры не поменялись', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <CallsFilters
                { ...baseProps }
                initialFilters={{ [ FIELD_NAMES.CALLBACK ]: FILTER_CALLBACK_VALUES.CALLBACK_GROUP }}
            />
        </ContextProvider>,
    );

    const instance = tree.dive().instance();

    instance.debouncedOnChangeFilters = jest.fn();

    instance.onChange({ [FIELD_NAMES.CALLBACK]: FILTER_CALLBACK_VALUES.CALLBACK_GROUP });

    expect(instance.debouncedOnChangeFilters).not.toHaveBeenCalled();
});

it('должен сбрасывать фильтры в стейте на дефолтные при нажатии на reset', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <CallsFilters
                { ...baseProps }
                initialFilters={{ [ FIELD_NAMES.CALLBACK ]: FILTER_CALLBACK_VALUES.CALLBACK_GROUP }}
            />
        </ContextProvider>,
    );

    const instance = tree.dive().instance();

    instance.debouncedOnChangeFilters = jest.fn();

    instance.resetFilters();

    expect(instance.state.filters).toMatchSnapshot();
});

it('должен вызывать onChange со сбросом фильтров при нажатии на reset', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <CallsFilters
                { ...baseProps }
                initialFilters={{ [ FIELD_NAMES.CALLBACK ]: FILTER_CALLBACK_VALUES.CALLBACK_GROUP }}
            />
        </ContextProvider>,
    );

    const instance = tree.dive().instance();

    instance.debouncedOnChangeFilters = jest.fn();

    instance.resetFilters();

    expect(instance.debouncedOnChangeFilters.mock.calls[0][0]).toMatchSnapshot();
});
