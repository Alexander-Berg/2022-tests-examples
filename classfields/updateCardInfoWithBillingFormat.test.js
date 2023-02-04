const _ = require('lodash');
const updateCardInfoWithBillingFormat = require('./updateCardInfoWithBillingFormat');
const updateCardInfoMock = require('autoru-frontend/mockData/responses/billing/updateCardInfo.mock');

let result;
beforeEach(() => {
    result = _.cloneDeep(updateCardInfoMock);
});

it('если нет массива с картами, то отдаст ничего', () => {
    result.result.card_ps = undefined;

    expect(
        updateCardInfoWithBillingFormat({ result: result.result }),
    ).toBeUndefined();
});

it('правильно форматирует ответ ручки', () => {
    expect(
        updateCardInfoWithBillingFormat({ result: result.result }),
    ).toMatchSnapshot();
});
