const React = require('react');
const SalesOfferFilters = require('./SalesOfferFilters');
const { shallow } = require('enzyme');
const salesMarkModels = require('./mocks/salesMarkModels');
const lodash = require('lodash');
lodash.debounce = jest.fn(fn => fn);

describe('changeRouteParams', () => {
    it('должен установить правильное значение поля mark_model и super_gen, ' +
        'когда пользователь нажимает на кнопку Любая в поле mark', () => {
        const router = { replace: jest.fn() };
        const linkCabinet = jest.fn();
        const salesOfferFilters = shallow(
            <SalesOfferFilters
                salesMarkModels={ salesMarkModels }
                routeParams={{
                    super_gen: [ '3473273' ],
                    mark_model: [ 'AUDI', 'BMW#X5' ],
                }}
                router={ router }
            />,
            { context: {
                linkCabinet,
                pageParams: {},
                metrika: {
                    sendParams: jest.fn(),
                },
            } });
        salesOfferFilters
            .find('Select[name="mark"]')
            .simulate('change', [ 'CLEAR', 'AUDI', 'BMW' ], { name: 'mark' });

        expect(linkCabinet).toHaveBeenCalledWith('sales', { mark_model: [], super_gen: [], resetSales: false });
    });

    it('должен установить правильное значение поля mark_model и super_gen, ' +
        'когда пользователь нажимает на кнопку Любая в поле mark_model', () => {
        const router = { replace: jest.fn() };
        const linkCabinet = jest.fn();
        const salesOfferFilters = shallow(
            <SalesOfferFilters
                salesMarkModels={ salesMarkModels }
                routeParams={{
                    super_gen: [ '3473273' ],
                    mark_model: [ 'AUDI#Q3', 'BMW#X5' ],
                }}
                router={ router }
            />,
            { context: {
                linkCabinet,
                pageParams: {},
                metrika: {
                    sendParams: jest.fn(),
                },
            } });
        salesOfferFilters
            .find('Select[name="mark_model"]')
            .simulate('change', [ 'CLEAR', 'AUDI#Q3', 'BMW#X5' ], { name: 'mark_model' });

        expect(linkCabinet).toHaveBeenCalledWith('sales', { mark_model: [ 'AUDI', 'BMW' ], super_gen: [], resetSales: false });
    });

    it('должен установить правильное значение поля mark_model и super_gen, когда пользователь выбирает марку', () => {
        const router = { replace: jest.fn() };
        const linkCabinet = jest.fn();
        const salesOfferFilters = shallow(
            <SalesOfferFilters
                salesMarkModels={ salesMarkModels }
                routeParams={{
                    super_gen: [ '20293979' ],
                    mark_model: [ 'AUDI#Q3', 'BMW#X5' ],
                }}
                router={ router }
            />,
            { context: {
                linkCabinet,
                pageParams: {},
                metrika: {
                    sendParams: jest.fn(),
                },
            } });

        salesOfferFilters
            .find('Select[name="mark"]')
            .simulate('change', [ 'CADILLAC', 'AUDI', 'BMW' ], { name: 'mark' });
        expect(linkCabinet).toHaveBeenCalledWith('sales', {
            mark_model: [ 'AUDI#Q3', 'BMW#X5', 'CADILLAC' ],
            super_gen: [ '20293979' ],
            resetSales: false,
        });
    });

    it('должен установить правильное значение поля mark_model и super_gen, когда пользователь выбирает модель', () => {
        const router = { replace: jest.fn() };
        const linkCabinet = jest.fn();
        const salesOfferFilters = shallow(
            <SalesOfferFilters
                salesMarkModels={ salesMarkModels }
                routeParams={{
                    mark_model: [ 'AUDI#Q3', 'BMW#X5' ],
                    super_gen: [ '20293979' ],
                }}
                router={ router }
            />,
            { context: {
                linkCabinet,
                pageParams: {},
                metrika: {
                    sendParams: jest.fn(),
                },
            } });

        salesOfferFilters
            .find('Select[name="mark_model"]')
            .simulate('change', [ 'CADILLAC#ESCALADE', 'AUDI#Q3', 'BMW#X5' ], { name: 'mark_model' });
        expect(linkCabinet)
            .toHaveBeenCalledWith('sales', {
                mark_model: [ 'CADILLAC#ESCALADE', 'AUDI#Q3', 'BMW#X5' ],
                super_gen: [ '20293979' ],
                resetSales: false,
            });
    });

    it('должен установить правильное значение поля mark_model и super_gen, когда пользователь отменяет выбор модели', () => {
        const router = { replace: jest.fn() };
        const linkCabinet = jest.fn();
        const salesOfferFilters = shallow(
            <SalesOfferFilters
                salesMarkModels={ salesMarkModels }
                routeParams={{
                    super_gen: [ '3473273' ],
                    mark_model: [ 'BMW#X5' ],
                }}
                router={ router }
            />,
            { context: {
                linkCabinet,
                pageParams: {},
                metrika: {
                    sendParams: jest.fn(),
                },
            } });

        salesOfferFilters
            .find('Select[name="mark_model"]')
            .simulate('change', [ ], { name: 'mark_model' });

        expect(linkCabinet).toHaveBeenCalledWith('sales', {
            mark_model: [ 'BMW' ],
            super_gen: [],
            resetSales: false,
        });
    });

    it('должен установить правильное значение поля mark_model, когда пользователь отменяет выбор марки', () => {
        const router = { replace: jest.fn() };
        const linkCabinet = jest.fn();
        const salesOfferFilters = shallow(
            <SalesOfferFilters
                salesMarkModels={ salesMarkModels }
                routeParams={{
                    mark_model: [ 'BMW#X5', 'AUDI#Q5', 'AUDI#Q7' ],
                }}
                router={ router }
            />,
            { context: {
                linkCabinet,
                pageParams: {},
                metrika: {
                    sendParams: jest.fn(),
                },
            } });

        salesOfferFilters
            .find('Select[name="mark"]')
            .simulate('change', [ 'BMW' ], { name: 'mark' });

        expect(linkCabinet).toHaveBeenCalledWith('sales', { mark_model: [ 'BMW#X5' ], super_gen: [], resetSales: false });
    });
});

it('должен установить правильное значение поля vin, ' +
    'когда пользователь вводит несколько винов через разные разделители', () => {
    const router = { replace: jest.fn() };
    const linkCabinet = jest.fn();
    const salesOfferFilters = shallow(
        <SalesOfferFilters
            salesMarkModels={ salesMarkModels }
            routeParams={{
                vin: [ 123, 345, 567 ],
            }}
            router={ router }
        />,
        { context: {
            linkCabinet,
            pageParams: {},
            metrika: {
                sendParams: jest.fn(),
            },
        } });
    salesOfferFilters
        .find('TextInput[name="VIN"]')
        .simulate('change', 'SALGV3TF0EA173321, MEFGV3TF0EA173324;5N1AL0MMXGC542221 Z94C251ABJR006678');

    return Promise.resolve().then(() => {
        expect(linkCabinet).toHaveBeenCalledWith('sales', {
            vin: [ 'SALGV3TF0EA173321', 'MEFGV3TF0EA173324', '5N1AL0MMXGC542221', 'Z94C251ABJR006678' ],
            super_gen: [],
            resetSales: false,
        });
    });
});

it('не должен вызывать router.replace, ' +
    'если пользователь повторно ввел значения фильтров вина и года', () => {
    const router = { replace: jest.fn() };
    const linkCabinet = jest.fn();
    const salesOfferFilters = shallow(
        <SalesOfferFilters
            salesMarkModels={ salesMarkModels }
            routeParams={{
                year_from: '1920',
                price_from: 2000,
                vin: [ 'MEFGV3TF0EA173324' ],
            }}
            router={ router }
        />,
        { context: {
            linkCabinet,
            pageParams: {},
            metrika: {
                sendParams: jest.fn(),
            },
        } });
    salesOfferFilters
        .find('TextInput[name="year_from"]')
        .simulate('change', '1920', { name: 'year_from' });
    salesOfferFilters
        .find('TextInput[name="VIN"]')
        .simulate('vin', 'MEFGV3TF0EA173324', { name: 'vin' });

    return Promise.resolve().then(() => {
        expect(router.replace).not.toHaveBeenCalled();
    });
});
