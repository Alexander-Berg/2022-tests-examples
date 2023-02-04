const agencyStats = require('../mocks/agencyStats');
const prepareLinearChartData = require('./prepareLinearChartData');
it('должен подготовить данные для рисования DashboardAgencyCharts', () => {
    expect(prepareLinearChartData(agencyStats)).toEqual({
        badge: {
            total: 1160,
            label: 'Стикеры',
            data: [
                { value: 0, date: '11 июня' },
                { value: 1160, date: '16 июня' },
            ],
        },
        boost: {
            total: 29000,
            label: 'Поднятие в поиске',
            data: [
                { value: 0, date: '11 июня' },
                { value: 29000, date: '16 июня' },
            ],
        },
        'match-application:cars:new': {
            total: 2800,
            label: undefined,
            data: [
                { value: 0, date: '11 июня' },
                { value: 2800, date: '16 июня' },
            ],
        },
        placement: {
            total: 2389797,
            label: 'Размещение объявления',
            data: [
                { value: 1396841, date: '11 июня' },
                { value: 992956, date: '16 июня' },
            ],
        },
        premium: {
            total: 15200,
            label: 'Премиум',
            data: [
                { value: 15200, date: '11 июня' },
                { value: 0, date: '16 июня' },
            ],
        },
        'quota:placement:cars:new': {
            total: 2800,
            label: 'Размещение «Легковые Новые»',
            data: [
                { value: 2800, date: '11 июня' },
                { value: 0, date: '16 июня' },
            ],
        },
        'quota:placement:moto': {
            total: 70,
            label: 'Размещение «Мото»',
            data: [
                { value: 70, date: '11 июня' },
                { value: 0, date: '16 июня' },
            ],
        },
        all: {
            total: 2440827,
            label: 'Все расходы',
            data: [
                { value: 1414911, date: '11 июня' },
                { value: 1025916, date: '16 июня' },
            ],
        },
    });
});
