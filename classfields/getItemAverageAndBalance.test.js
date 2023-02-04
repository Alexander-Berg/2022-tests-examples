jest.mock('auto-core/react/lib/gateApi');
const getItemAverageAndBalance = require('./getItemAverageAndBalance');

it('должен вызвать gateApi.getResource с корректными параметрами', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve(true));

    return getItemAverageAndBalance('clientId')
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('getDealerAccount', { dealer_id: 'clientId' });
        });
});
