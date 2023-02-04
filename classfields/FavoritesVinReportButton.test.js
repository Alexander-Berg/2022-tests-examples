/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const React = require('react');
const { shallow } = require('enzyme');

const FavoritesVinReportButton = require('./FavoritesVinReportButton').default;

const gateApi = require('auto-core/react/lib/gateApi');
const { offer } = require('autoru-frontend/mockData/responses/offer.mock.js');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const oldWindowLocation = window.location;

beforeAll(() => {
    delete window.location;
    window.location = { assign: jest.fn() };
});

afterAll(() => {
    window.location = oldWindowLocation;
});

it('должен узнать статус оплаты отчета и отправить на /history, если отчет оплачен', async() => {
    const promise = new Promise((resolve) => {
        setTimeout(() => resolve({ report_type: 'PAID_REPORT' }), 0);
    });
    gateApi.getResource.mockImplementationOnce(() => promise);

    const wrapper = shallow(<FavoritesVinReportButton offer={ offer }/>, { context: contextMock });
    wrapper.find('Button').simulate('click');

    return promise.then(() => {
        const url = window.location.assign.mock.calls[0][0];
        expect(url.startsWith('link/proauto-report')).toBe(true);
    });

});

it('должен узнать статус оплаты отчета и отправить на карточку, если отчет не оплачен', async() => {
    const promise = new Promise((resolve) => {
        setTimeout(() => resolve({ report_type: 'FREE_REPORT' }), 0);
    });
    gateApi.getResource.mockImplementationOnce(() => promise);

    const wrapper = shallow(<FavoritesVinReportButton offer={ offer }/>, { context: contextMock });
    wrapper.find('Button').simulate('click');

    return promise.then(() => {
        const url = window.location.assign.mock.calls[0][0];
        expect(url.startsWith('link/card')).toBe(true);
    });

});

it('должен узнать статус оплаты отчета и отправить на карточку, если сервер ответил что-то не то', async() => {
    const promise = new Promise((resolve, reject) => {
        setTimeout(() => reject());
    });
    gateApi.getResource.mockImplementationOnce(() => promise);

    const wrapper = shallow(<FavoritesVinReportButton offer={ offer }/>, { context: contextMock });
    wrapper.find('Button').simulate('click');

    return promise.then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            const url = window.location.assign.mock.calls[0][0];
            expect(url.startsWith('link/card')).toBe(true);
        });
});
