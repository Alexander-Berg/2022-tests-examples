const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const _ = require('lodash');

const CallsFiltersOffer = require('./CallsFiltersOffer');

const MarkFilter = require('auto-core/react/components/desktop/filters/MarkFilter');
const ModelFilter = require('auto-core/react/components/desktop/filters/ModelFilter');
const PriceFromToFilter = require('auto-core/react/components/common/filters/PriceFromToFilter');
const YearFromToFilter = require('auto-core/react/components/common/filters/YearFromToFilter');
const TextInput = require('auto-core/react/components/islands/TextInput');
const BodyTypeGroupFilter = require('auto-core/react/components/common/filters/BodyTypeGroupFilter');
const TransmissionFilter = require('auto-core/react/components/common/filters/TransmissionFilter');

const FIELD_NAMES = require('www-cabinet/data/calls/filter-call-field-names.json');

const filtersMock = require('www-cabinet/react/dataDomain/calls/mocks/withFilters.mock').filters;
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const ContextProvider = createContextProvider(contextMock);

const baseProps = {
    filters: {},
    onChange: _.noop,
    breadcrumbs: breadcrumbsPublicApiMock.data,
};

it('должен заполнять селект моделей моделями выбранной марки', () => {
    const filter = _.cloneDeep(filtersMock);

    filter[FIELD_NAMES.MARK] = 'FORD';

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filter }
            />
        </ContextProvider>,
    ).dive();

    const select = tree.find(ModelFilter);

    expect(select.props().items).toHaveLength(88);
    expect(select.props().items.slice(0, 3)).toMatchSnapshot(); // частично, чтобы не снэпшотить всю коллекцию
});

it('должен заполнять селект поколений поколениями выбранной марки и модели', () => {
    const filter = _.cloneDeep(filtersMock);

    filter[FIELD_NAMES.MARK] = 'FORD';
    filter[FIELD_NAMES.MODEL] = 'ECOSPORT';

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filter }
            />
        </ContextProvider>,
    ).dive();

    const select = tree.find({ placeholder: 'Поколение' });

    expect(shallowToJson(select)).toMatchSnapshot();
});

it('должен очищать модель и поколение при выборе марки', () => {
    const onChange = jest.fn();

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filtersMock }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(MarkFilter).simulate('change', [ 'BMW' ]);

    expect(onChange).toHaveBeenCalledWith({
        [FIELD_NAMES.MARK]: 'BMW',
        [FIELD_NAMES.MODEL]: undefined,
        [FIELD_NAMES.GENERATION]: undefined,
    });
});

it('должен очищать поколение при выборе модели', () => {
    const onChange = jest.fn();

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filtersMock }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(ModelFilter).simulate('change', [ 'ACURA' ]);

    expect(onChange).toHaveBeenCalledWith({
        [FIELD_NAMES.MODEL]: 'ACURA',
        [FIELD_NAMES.GENERATION]: undefined,
    });
});

it('должен очищать vin при событии очистки', () => {
    const onChange = jest.fn();

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filtersMock }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(TextInput).simulate('change', '123', {}, { source: 'clear' });

    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.VIN]: '' });
});

it('должен заполнять цены в соответствии с props.name', () => {
    const onChange = jest.fn();

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filtersMock }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(PriceFromToFilter).simulate('change', 150, { name: 'price_from' });
    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.PRICE_FROM]: 150 });

    tree.find(PriceFromToFilter).simulate('change', 150, { name: 'price_to' });
    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.PRICE_TO]: 150 });
});

it('должен заполнять года в соответствии с props.name', () => {
    const onChange = jest.fn();
    const ContextProvider = createContextProvider(contextMock);

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filtersMock }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(YearFromToFilter).simulate('change', 1990, { name: 'year_from' });
    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.YEAR_FROM]: 1990 });

    tree.find(YearFromToFilter).simulate('change', 2010, { name: 'year_to' });
    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.YEAR_TO]: 2010 });
});

it('должен выбирать типы кузовов в соответствии с props.name', () => {
    const onChange = jest.fn();

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filtersMock }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(BodyTypeGroupFilter).simulate('change', [ 'HATCHBACK_3_DOORS' ], { name: 'body_type' });
    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.BODY_TYPE]: [ 'HATCHBACK_3_DOORS' ] });
});

it('должен выбирать коробку передач в соответствии с props.name', () => {
    const onChange = jest.fn();

    const tree = shallow(
        <ContextProvider>
            <CallsFiltersOffer
                { ...baseProps }
                filters={ filtersMock }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    tree.find(TransmissionFilter).simulate('change', [ 'ROBOT' ], { name: 'transmission' });
    expect(onChange).toHaveBeenCalledWith({ [FIELD_NAMES.TRANSMISSION]: [ 'ROBOT' ] });
});
