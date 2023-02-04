const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const TradeInDumb = require('./TradeInDumb');
const MockDate = require('mockdate');
const _ = require('lodash');

jest.mock('./img/tradeIn.png', () => 'image');

beforeEach(() => {
    MockDate.set('1988-01-03');
});

const baseProps = {
    hasNeverActivated: false,
    canWrite: true,
    onPageClick: _.noop,
    onDateRangeChange: _.noop,
    onSwitcherToggle: _.noop,
    pagination: {},
    items: [],
};

describe('render тесты', () => {
    it('должен вернуть промо-плейсхолдер, если раздел никогда не активировался', () => {
        const tree = shallow(
            <TradeInDumb
                { ...baseProps }
                hasNeverActivated={ true }
            />,
        );

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если items.length > 0', () => {
        const items = [
            { id: 1, create_date: 1 },
            { id: 2, create_date: 2 },
            { id: 3, create_date: 3 },
        ];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
            />,
        ).instance();

        tradeInDumbInstance.renderFilters = () => 'renderFilters';
        tradeInDumbInstance.renderTable = () => 'renderTable';
        tradeInDumbInstance.renderPagination = () => 'renderPagination';

        expect(tradeInDumbInstance.render()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если items.length === 0', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
            />).instance();

        tradeInDumbInstance.renderFilters = () => 'renderFilters';
        tradeInDumbInstance.renderTable = () => 'renderTable';
        tradeInDumbInstance.renderPagination = () => 'renderPagination';

        expect(tradeInDumbInstance.render()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если isLoading = true', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                isLoading
                pagination={ pagination }
                items={ items }
            />).instance();

        tradeInDumbInstance.renderFilters = () => 'renderFilters';
        tradeInDumbInstance.renderTable = () => 'renderTable';
        tradeInDumbInstance.renderPagination = () => 'renderPagination';

        expect(tradeInDumbInstance.render()).toMatchSnapshot();
    });
});

describe('renderFilters тесты', () => {
    it('должен вернуть корректный компонент, ' +
        'если isNewFilterDisabled и isUsedFilterDisabled', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                filters={{
                    isNewFilterDisabled: true,
                    isUsedFilterDisabled: true,
                    dateRange: {
                        fromDate: '2019-03-20',
                    },
                    totalRequestCount: 40,
                    totalCost: 500,
                    section: 'ALL',
                }}
            />
            ,
        ).instance();

        expect(tradeInDumbInstance.renderFilters()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если !isNewFilterDisabled && isUsedFilterDisabled', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                filters={{
                    isUsedFilterDisabled: true,
                    dateRange: {
                        fromDate: '2019-03-20',
                    },
                    totalRequestCount: 40,
                    totalCost: 500,
                    section: 'ALL',
                }}
            />,
        ).instance();

        expect(tradeInDumbInstance.renderFilters()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если isNewFilterDisabled && !isUsedFilterDisabled', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                filters={{
                    isNewFilterDisabled: true,
                    dateRange: {
                        fromDate: '2019-03-20',
                    },
                    totalRequestCount: 40,
                    totalCost: 500,
                    section: 'ALL',
                }}
            />,
        ).instance();

        expect(tradeInDumbInstance.renderFilters()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если !isNewFilterDisabled && !isUsedFilterDisabled', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                filters={{
                    dateRange: {
                        fromDate: '2019-03-20',
                    },
                    totalRequestCount: 40,
                    totalCost: 500,
                    section: 'ALL',
                }}
            />,
        ).instance();

        expect(tradeInDumbInstance.renderFilters()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если dateRange.toDate', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                filters={{
                    dateRange: {
                        fromDate: '2019-02-20',
                        toDate: '2019-03-20',
                    },
                    totalRequestCount: 40,
                    totalCost: 500,
                    section: 'ALL',
                }}
            />,
        ).instance();

        expect(tradeInDumbInstance.renderFilters()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если totalRequestCount === 0', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                filters={{
                    dateRange: {
                        fromDate: '2019-02-20',
                        toDate: '2019-03-20',
                    },
                    totalRequestCount: 0,
                    totalCost: 0,
                    section: 'ALL',
                }}
            />,
        ).instance();

        expect(tradeInDumbInstance.renderFilters()).toMatchSnapshot();
    });
});

it('renderPagination тест: должен вернуть корректный компонент', () => {
    const items = [];
    const pagination = {
        page_num: 1,
        page_size: 10,
        total_count: 27,
        total_page_count: 3,
    };

    const tradeInDumbInstance = shallow(
        <TradeInDumb
            { ...baseProps }
            pagination={ pagination }
            items={ items }
            filters={{
                isNewFilterDisabled: true,
                isUsedFilterDisabled: true,
                dateRange: {
                    fromDate: '2019-03-20',
                },
                totalRequestCount: 40,
                totalCost: 500,
                section: 'ALL',
            }}
        />
        ,
    ).instance();

    expect(tradeInDumbInstance.renderPagination()).toMatchSnapshot();
});

describe('renderTable тесты', () => {
    it('должен вернуть корректный компонент, ' +
        'если разные дни items.create_date', () => {
        const items = [
            { id: 1, create_date: '1988-03-01 00:00:01' },
            { id: 2, create_date: '1988-03-01 00:00:02' },
            { id: 3, create_date: '1988-03-02 00:00:03' },
        ];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                newCarsSwitcher={{
                    title: 'newCarsSwitcher',
                    isDisabled: true,
                    isActive: true,
                }}
            />,
        ).instance();

        expect(tradeInDumbInstance.renderTable()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если разные года в items.create_date', () => {
        const items = [
            { id: 1, create_date: '1988-03-01 00:00:01' },
            { id: 2, create_date: '1988-03-01 00:00:02' },
            { id: 3, create_date: '1987-03-02 00:00:03' },
        ];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                newCarsSwitcher={{
                    title: 'newCarsSwitcher',
                    isDisabled: true,
                    isActive: true,
                }}
            />,
        ).instance();

        expect(tradeInDumbInstance.renderTable()).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, ' +
        'если items.length === 0', () => {
        const items = [];
        const pagination = {
            page_num: 1,
            page_size: 10,
            total_count: 27,
            total_page_count: 3,
        };

        const tradeInDumbInstance = shallow(
            <TradeInDumb
                { ...baseProps }
                pagination={ pagination }
                items={ items }
                newCarsSwitcher={{
                    title: 'newCarsSwitcher',
                    isDisabled: true,
                    isActive: true,
                }}
            />,
        ).instance();

        expect(tradeInDumbInstance.renderTable()).toMatchSnapshot();
    });
});
