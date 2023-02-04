const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const CallsFiltersMain = require('./CallsFiltersMain');

const CallsFiltersTagSuggest = require('www-cabinet/react/components/CallsFiltersTagSuggest');
const TextInput = require('auto-core/react/components/islands/TextInput');

const FIELD_NAMES = require('www-cabinet/data/calls/filter-call-field-names.json');

const filtersMock = require('www-cabinet/react/dataDomain/calls/mocks/withFilters.mock').filters;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const baseProps = {
    changeRouteParams: jest.fn(),
    filters: {},
    onChange: jest.fn(),
    requestExport: jest.fn(),
    getTags: jest.fn(),
};

it('должен добавлять новый тег, если его не было', () => {
    const filters = _.cloneDeep(filtersMock);
    const onChange = jest.fn();
    const ContextProvider = createContextProvider(contextMock);

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersMain
                { ...baseProps }
                filters={ filters }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(CallsFiltersTagSuggest).simulate('select', { value: 'tag_1' });

    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.TAG]: [ 'кредит', 'трейдин', 'tag_1' ] });
});

it('должен удалять существующий тег, если он был', () => {
    const onChange = jest.fn();
    const ContextProvider = createContextProvider(contextMock);

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersMain
                { ...baseProps }
                filters={ filtersMock }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(CallsFiltersTagSuggest).simulate('select', { value: 'кредит' });

    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.TAG]: [ 'трейдин' ] });
});

it('должен форматировать телефон клиента, удаляя недопустимые символы', () => {
    const updateFilter = jest.fn();
    const ContextProvider = createContextProvider(contextMock);

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersMain
                { ...baseProps }
                filters={ filtersMock }
                updateFilter={ updateFilter }
            />
        </ContextProvider>,
    ).dive();

    tree.find(TextInput).at(0).simulate('change', '+d7979-777-66-55');

    expect(updateFilter).toHaveBeenCalledWith({ [FIELD_NAMES.CLIENT_PHONE]: '+7979-777-66-55' });
});

it('должен форматировать телефон салона, удаляя недопустимые символы', () => {
    const updateFilter = jest.fn();
    const ContextProvider = createContextProvider(contextMock);

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersMain
                { ...baseProps }
                filters={ filtersMock }
                updateFilter={ updateFilter }
            />
        </ContextProvider>,
    ).dive();

    tree.find(TextInput).at(1).simulate('change', '+d7979-777-66-55');

    expect(updateFilter).toHaveBeenCalledWith({ [FIELD_NAMES.SALON_PHONE]: '+7979-777-66-55' });
});

describe('кнопка экспорта', () => {
    it('должен показывать лоадер на время запроса экспорта', () => {
        const ContextProvider = createContextProvider(contextMock);

        const promise = Promise.resolve();
        const requestExport = jest.fn(() => promise);

        const tree = shallow(
            <ContextProvider>
                <CallsFiltersMain
                    { ...baseProps }
                    requestExport={ requestExport }
                />
            </ContextProvider>,
        ).dive();

        tree.find('.CallsFiltersMain__exportButton').simulate('click');

        const loaderSelector = 'Loader.CallsFiltersMain__exportButton';
        expect(tree.find(loaderSelector)).toExist();

        return promise.then(() => {
            expect(tree.find(loaderSelector)).not.toExist();
        });
    });

    it('должен дизейблить кнопку, если выбран период > 92 дней', () => {
        const requestExport = jest.fn();
        const ContextProvider = createContextProvider(contextMock);

        const tree = shallow(
            <ContextProvider>
                <CallsFiltersMain
                    { ...baseProps }
                    filters={{ from: '2019-12-05', to: '2020-03-31' }}
                    requestExport={ requestExport }
                />
            </ContextProvider>,
        ).dive();

        tree.find('.CallsFiltersMain__exportButton').simulate('click');

        expect(requestExport).not.toHaveBeenCalled();
    });
});
