const React = require('react');
const { shallow } = require('enzyme');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const backOnSaleSortsProps = require('./mocks/backOnSaleSortsProps');
const BackOnSaleSorts = require('./BackOnSaleSorts');

it('render: должен вернуть набор сортировок', () => {
    const BackOnSaleSortsInstance = shallow(<BackOnSaleSorts { ...backOnSaleSortsProps }/>, { context: contextMock }).instance();

    expect(BackOnSaleSortsInstance.render()).toMatchSnapshot();
});

describe('onChange тесты:', () => {
    it('должен вызвать getListing с корректными параметрами', () => {
        const getListing = jest.fn();
        const BackOnSaleSortsInstance = shallow(
            <BackOnSaleSorts
                { ...backOnSaleSortsProps }
                getListing={ getListing }
            />,
            { context: contextMock }).instance();
        BackOnSaleSortsInstance.onChange(false, { name: 'sorting' });

        expect(getListing).toHaveBeenCalledWith(1);
    });

    it('должен вызвать handleChangedControl, metrika.sendPageEvent с корректными параметрами', () => {
        const handleChangedControl = jest.fn();
        const BackOnSaleSortsInstance = shallow(
            <BackOnSaleSorts
                { ...backOnSaleSortsProps }
                handleChangedControl={ handleChangedControl }
            />,
            { context: contextMock }).instance();
        BackOnSaleSortsInstance.onChange(false, { name: 'only_last_seller' });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'sort', 'only_last_seller' ]);
        expect(handleChangedControl).toHaveBeenCalledWith(
            [ { control: { name: 'only_last_seller' }, newValue: false } ],
        );
    });

    it('должен вызвать handleChangedControl, metrika.sendPageEvent с корректными параметрами, если controlProps.name === "dateRange"', () => {
        const handleChangedControl = jest.fn();
        const BackOnSaleSortsInstance = shallow(
            <BackOnSaleSorts
                { ...backOnSaleSortsProps }
                handleChangedControl={ handleChangedControl }
            />,
            { context: contextMock }).instance();
        BackOnSaleSortsInstance.onChange(
            {
                from: '2020-01-01',
                to: '2020-02-02',
            },
            { name: 'dateRange' },
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'sort', 'dateRange' ]);
        expect(handleChangedControl).toHaveBeenCalledWith(
            [
                { control: { name: 'creation_date_from' }, newValue: '2020-01-01' },
                { control: { name: 'creation_date_to' }, newValue: '2020-02-02' },

            ],
        );
    });

    it('должен вызвать handleChangedControl, metrika.sendPageEvent с корректными параметрами, если controlProps.name === "sorting"', () => {
        const handleChangedControl = jest.fn();
        const BackOnSaleSortsInstance = shallow(
            <BackOnSaleSorts
                { ...backOnSaleSortsProps }
                handleChangedControl={ handleChangedControl }
            />,
            { context: contextMock }).instance();
        BackOnSaleSortsInstance.onChange([ 'LAST_EVENT_DATE_DESC' ], { name: 'sorting' });

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'sort', 'sorting', 'LAST_EVENT_DATE_DESC' ]);
        expect(handleChangedControl).toHaveBeenCalledWith(
            [ { control: { name: 'sorting' }, newValue: 'LAST_EVENT_DATE_DESC' } ],
        );
    });
});

it('onDateRangeChange: должен вызвать this.onChange с корректными параметрами', () => {
    const BackOnSaleSortsInstance = shallow(
        <BackOnSaleSorts
            { ...backOnSaleSortsProps }
        />, { context: contextMock }).instance();
    BackOnSaleSortsInstance.onChange = jest.fn();
    BackOnSaleSortsInstance.onDateRangeChange({ creation_date_from: '2020-01-01', creation_date_to: '2020-02-02' });

    expect(BackOnSaleSortsInstance.onChange).toHaveBeenCalledWith({ creation_date_from: '2020-01-01', creation_date_to: '2020-02-02' }, { name: 'dateRange' });
});
