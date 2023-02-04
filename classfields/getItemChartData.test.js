jest.mock('auto-core/react/lib/gateApi');
const getItemChartData = require('./getItemChartData');

it('должен вызвать gateApi.getResource с корректными параметрами', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve(true));

    return getItemChartData({ start: '2020-10-11', end: '2020-10-12', dealer_id: 'clientId' })
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('getDealerProductTotalStats', { start: '2020-10-11', end: '2020-10-12', dealer_id: 'clientId' });
        });
});
