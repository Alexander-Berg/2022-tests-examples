const React = require('react');
const SalesOfferFiltersVinReports = require('./SalesOfferFiltersVinReports').default;
const { shallow } = require('enzyme');
const _ = require('lodash');

describe('onChange', () => {
    it('должен сбросить фильтр Нет отчета', () => {
        const linkCabinet = jest.fn();
        const router = { replace: jest.fn() };
        const salesOfferFiltersVinReports = shallow(
            <SalesOfferFiltersVinReports
                hideGroupActions={ _.noop }
                routeParams={{
                    all: '1',
                    client_id: '16453',
                    exclude_tag: [
                        'vin_resolution_ok',
                        'vin_resolution_unknown',
                        'vin_resolution_error',
                        'vin_resolution_invalid',
                        'vin_resolution_untrusted',
                    ],
                    resetSales: 'false',
                }}
                router={ router }
                routeParamsExcludeTag={ [
                    'vin_resolution_ok',
                    'vin_resolution_unknown',
                    'vin_resolution_error',
                    'vin_resolution_invalid',
                    'vin_resolution_untrusted',
                ] }
                routeParamsTag={ [] }
            />,
            { context: {
                linkCabinet,
                metrika: {
                    sendPageEvent: jest.fn(),
                },
            } });
        salesOfferFiltersVinReports.instance().onChange('CLEAR');

        expect(linkCabinet).toHaveBeenCalledWith('sales', { all: '1', client_id: '16453', resetSales: false, exclude_tag: [] });
    });

    it('должен сбросить фильтр Серые отчеты', () => {
        const linkCabinet = jest.fn();
        const router = { replace: jest.fn() };
        const salesOfferFiltersVinReports = shallow(
            <SalesOfferFiltersVinReports
                hideGroupActions={ _.noop }
                routeParams={{
                    all: '1',
                    client_id: '16453',
                    resetSales: 'false',
                    tag: [ 'vin_resolution_untrusted', 'vin_resolution_unknown' ],
                }}
                router={ router }
                routeParamsExcludeTag={ [] }
                routeParamsTag={ [ 'vin_resolution_untrusted', 'vin_resolution_unknown' ] }
            />,
            { context: {
                linkCabinet,
                metrika: {
                    sendPageEvent: jest.fn(),
                },
            } });
        salesOfferFiltersVinReports.instance().onChange('CLEAR');

        expect(linkCabinet).toHaveBeenCalledWith('sales', { all: '1', client_id: '16453', resetSales: false, tag: [] });
    });
});
