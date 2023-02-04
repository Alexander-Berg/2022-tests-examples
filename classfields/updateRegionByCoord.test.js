const updateRegionById = require('./updateRegionByCoord');

it('updateRegionByCoord тест: должен dispatch корректный объект', () => {
    const dispatch = jest.fn();

    updateRegionById('coord', { deleted: true })(dispatch);
    expect(dispatch.mock.calls).toEqual([
        [ {
            type: 'UPDATE_DELIVERY_SETTINGS_REGION_BY_COORD',
            payload: { coord: 'coord', fields: { deleted: true } },
        } ],
    ]);
});
