const data = require('./mockData/payment-response.json');
const { parsePurchasePromocodes } = require('../parse-products-info');

const { purchase } = data.response;

describe('parsePromocodes', () => {
    it('default', () => {
        const promocodes = parsePurchasePromocodes(purchase);

        expect(promocodes).toEqual({
            raising: {
                discount: 30,
                count: 1
            },
            premium: {
                discount: 459,
                count: 1
            },
            money: {
                discount: 20,
                count: 1
            }
        });
    });
});
