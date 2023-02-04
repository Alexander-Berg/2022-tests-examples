const _ = require('lodash');
const updateCardInfoWithUserFormat = require('./updateCardInfoWithUserFormat');
const updateCardInfoMock = require('autoru-frontend/mockData/responses/billing/updateCardInfo.mock');

let response;
beforeEach(() => {
    response = _.cloneDeep(updateCardInfoMock);
});

it('если нет массива с картами, то отдаст ничего', () => {
    response.result.card_ps = undefined;

    const result = updateCardInfoWithUserFormat({ result: response.result });
    expect(result).toBeUndefined();
});

it('правильно форматирует ответ ручки', () => {
    const result = updateCardInfoWithUserFormat({ result: response.result });
    expect(result).toMatchSnapshot();
});
