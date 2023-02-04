const getBodyTypeParamName = require('auto-core/react/lib/offer/getBodyTypeParamName');
const mockOffer = require('autoru-frontend/mockData/responses/offer.mock');

it('Должен вернуть body_type_group для легковой', () => {
    expect(getBodyTypeParamName(mockOffer.offer)).toEqual('body_type_group');
});

it('Должен вернуть truck_type для грузовика', () => {
    expect(getBodyTypeParamName(mockOffer.offerTruck)).toEqual('truck_type');
});

it('Должно вернуть undefined, если не нашлось поле', () => {
    expect(getBodyTypeParamName({})).toBeUndefined();
});
