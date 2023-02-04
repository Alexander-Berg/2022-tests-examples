const React = require('react');
const { shallow } = require('enzyme');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
jest.mock('auto-core/react/lib/gateApi');
const backOnSaleFilterProps = require('./mocks/backOnSaleFiltersProps');
const geoData = require('./mocks/geoData');
const BackOnSaleFilters = require('./BackOnSaleFilters');
const searchParameters = require('./mocks/searchParameters');
const subscriptions = require('./mocks/subscriptions');

it('renderMainForm: должен вернуть набор основных фильтров', () => {
    const props = {
        ...backOnSaleFilterProps,
        geoData,
        breadcrumbsPublicApi: { data: [], status: 'SUCCESS' },
    };
    const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...props }/>, { context: contextMock }).instance();

    expect(backOnSaleFiltersInstance.renderMainForm()).toMatchSnapshot();
});

describe('renderExtendedForm', () => {
    it('должен вернуть набор дополнительных фильтров', () => {
        const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...backOnSaleFilterProps } geoData={ geoData }/>, { context: contextMock }).instance();
        backOnSaleFiltersInstance.state = {
            showExtendedFilters: true,
        };

        expect(backOnSaleFiltersInstance.renderExtendedForm()).toMatchSnapshot();
    });

    it('должен вернуть null, если !this.state.showExtendedFilters', () => {
        const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...backOnSaleFilterProps } geoData={ geoData }/>, { context: contextMock }).instance();

        expect(backOnSaleFiltersInstance.renderExtendedForm()).toMatchSnapshot();
    });
});

it('render: должен нарисовать набор основных и дополнительных фильтров и плашку с действиями', () => {
    const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...backOnSaleFilterProps } geoData={ geoData }/>, { context: contextMock }).instance();
    backOnSaleFiltersInstance.renderMainForm = () => 'mainForm';
    backOnSaleFiltersInstance.renderExtendedForm = () => 'extendedForm';
    backOnSaleFiltersInstance.renderActions = () => 'actions';

    expect(backOnSaleFiltersInstance.render()).toMatchSnapshot();
});

it('renderActionCollapse: должен нарисовать кнопку Свернуть/Развернуть', () => {
    const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...backOnSaleFilterProps } geoData={ geoData }/>, { context: contextMock }).instance();

    expect(backOnSaleFiltersInstance.renderActionCollapse()).toMatchSnapshot();
});

it('renderActions: должен нарисовать панель действий', () => {
    const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...backOnSaleFilterProps } geoData={ geoData }/>, { context: contextMock }).instance();
    backOnSaleFiltersInstance.renderActionCollapse = () => 'actionCollapse';
    backOnSaleFiltersInstance.renderFiltersCount = () => 'filtersCount';
    backOnSaleFiltersInstance.renderResetLink = () => 'renderResetLink';
    backOnSaleFiltersInstance.renderSubmitButton = () => 'submitButton';

    expect(backOnSaleFiltersInstance.renderActions()).toMatchSnapshot();
});

describe('renderFiltersCount', () => {
    it('должен вернуть null, если нет дополнительных фильтров', () => {
        const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...backOnSaleFilterProps } geoData={ geoData }/>, { context: contextMock }).instance();

        expect(backOnSaleFiltersInstance.renderFiltersCount()).toMatchSnapshot();
    });

    it('должен нарисовать счетчик дополнительных фильтров', () => {
        const backOnSaleFiltersInstance = shallow(
            <BackOnSaleFilters
                { ...backOnSaleFilterProps }
                geoData={ geoData }
                searchParametersCounters={{ main: 0, extended: 2 }}
            />, { context: contextMock }).instance();

        expect(backOnSaleFiltersInstance.renderFiltersCount()).toMatchSnapshot();
    });
});

describe('renderResetLink', () => {
    it('должен вернуть null, если счетчики фильтров равны 0', () => {
        const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters
            { ...backOnSaleFilterProps }
            geoData={ geoData }
        />, { context: contextMock }).instance();

        expect(backOnSaleFiltersInstance.renderResetLink()).toMatchSnapshot();
    });

    it('должен нарисовать кнопку сброса фильтров', () => {
        const backOnSaleFiltersInstance = shallow(
            <BackOnSaleFilters
                { ...backOnSaleFilterProps }
                geoData={ geoData }
                searchParametersCounters={{ main: 10, extended: 2 }}
            />, { context: contextMock }).instance();

        expect(backOnSaleFiltersInstance.renderResetLink()).toMatchSnapshot();
    });
});

it('renderSubmitButton: должен нарисовать кнопку отправки формы', () => {
    const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...backOnSaleFilterProps } geoData={ geoData }/>, { context: contextMock }).instance();

    expect(backOnSaleFiltersInstance.renderSubmitButton()).toMatchSnapshot();
});

describe('onFilterChange', () => {
    it('должен вызвать handleChangedControl и sendPageEventDebounced с корректными параметрами', () => {
        const handleChangedControl = jest.fn();
        const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters
            { ...backOnSaleFilterProps }
            geoData={ geoData }
            handleChangedControl={ handleChangedControl }
        />, { context: contextMock }).instance();
        backOnSaleFiltersInstance.sendPageEventDebounced = jest.fn();
        backOnSaleFiltersInstance.onFilterChange([ 1999 ], { name: 'year_from' });

        expect(handleChangedControl).toHaveBeenCalledWith([ { control: { name: 'year_from' }, newValue: [ 1999 ] } ]);
        expect(backOnSaleFiltersInstance.sendPageEventDebounced).toHaveBeenCalledWith([ 'filter', 'year_from_change' ]);
    });

    it('должен вызвать getBreadcrumbs, handleChangedControl и sendPageEventDebounced, если controlProps.name === "mmm-filter"', () => {
        const getBreadcrumbs = jest.fn();
        const catalogFilter = [ { mark: 'AUDI' } ];
        const handleChangedControl = jest.fn(() => 'newSearchParams');
        const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters
            { ...backOnSaleFilterProps }
            geoData={ geoData }
            getBreadcrumbs={ getBreadcrumbs }
            handleChangedControl={ handleChangedControl }
            searchParameters={{ catalog_filter: catalogFilter }}
        />, { context: contextMock }).instance();
        backOnSaleFiltersInstance.sendPageEventDebounced = jest.fn();

        backOnSaleFiltersInstance.onFilterChange([ 'value' ], { name: 'mmm-filter' });

        expect(handleChangedControl).toHaveBeenCalledWith([ { control: { name: 'mmm-filter' }, newValue: [ 'value' ] } ]);
        expect(getBreadcrumbs).toHaveBeenCalledWith('newSearchParams');
        expect(backOnSaleFiltersInstance.sendPageEventDebounced).toHaveBeenCalledWith([ 'filter', 'mark_change' ]);
    });

    it('должен вызвать handleChangedControl, sendPageEventDebounced с корректными параметрами, если controlProps.name === "generation"', () => {
        const handleChangedControl = jest.fn();
        const catalogFilter = [ { mark: 'AUDI', model: '100' } ];
        const backOnSaleFiltersInstance = shallow(
            <BackOnSaleFilters
                { ...backOnSaleFilterProps }
                geoData={ geoData }
                handleChangedControl={ handleChangedControl }
                searchParameters={{ catalog_filter: catalogFilter }}
            />, { context: contextMock },
        ).instance();
        backOnSaleFiltersInstance.sendPageEventDebounced = jest.fn();

        backOnSaleFiltersInstance.onFilterChange([ 7879464 ], { name: 'generation' });

        expect(handleChangedControl)
            .toHaveBeenCalledWith([
                {
                    control: { name: 'mmm-filter' },
                    newValue: { catalog_filter: [ { mark: 'AUDI', model: '100', generation: 7879464 } ] },
                } ]);
        expect(backOnSaleFiltersInstance.sendPageEventDebounced).toHaveBeenCalledWith([ 'filter', 'generation_change' ]);
    });
});

it('toggleCollapseClick: должен установить корректный state.showExtendedFilters', () => {
    const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters
        { ...backOnSaleFilterProps }
        geoData={ geoData }
    />, { context: contextMock }).instance();
    backOnSaleFiltersInstance.state.showExtendedFilters = false;
    backOnSaleFiltersInstance.toggleCollapseClick();

    expect(backOnSaleFiltersInstance.state.showExtendedFilters).toBe(true);
});

describe('isSaved', () => {
    it('должен вернуть true, если такой набор фильтров сохранен у пользователя в подписках', () => {
        const props = {
            ...backOnSaleFilterProps,
            searchParameters,
            geoData,
            subscriptions,
        };

        const backOnSaleFiltersInstance = shallow(
            <BackOnSaleFilters { ...props }/>, { context: contextMock }).instance();
        expect(backOnSaleFiltersInstance.getIsSaved()).toBe(true);
    });

    it('должен вернуть false, если набор фильтров не сохранен в подписках', () => {
        const props = {
            ...backOnSaleFilterProps,
            searchParameters,
            geoData,
            subscriptions: { data: [] },
        };

        const backOnSaleFiltersInstance = shallow(<BackOnSaleFilters { ...props }/>, { context: contextMock }).instance();

        expect(backOnSaleFiltersInstance.getIsSaved()).toBe(false);
    });
});
