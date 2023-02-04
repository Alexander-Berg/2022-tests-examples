const TRAILER_TYPES = require('auto-core/data/filters/drags/trailer_types.json');
const getChildGroup = require('auto-core/lib/util/getChildGroup');

it('should ignore unknown values', () => {
    const selectedValues = [
        'ST_ASSORTMENT',
        'ST_CEMENT_CARRIER',
        'ST_FARM_CARRIER',
        'ST_GAS_TRANSPORT',
        'ST_GLASS_CARRIER',
        'ST_HEAVY',
        'ST_LOW_FRAME_TRAWL',
        'ST_SPLINT_CARRIER',
        'ST_TANK',
        'ST_TIMBER_CARRIER',
        'S',
    ];
    expect(getChildGroup(selectedValues, TRAILER_TYPES)).toEqual([
        'ST_ASSORTMENT',
        'ST_CEMENT_CARRIER',
        'ST_FARM_CARRIER',
        'ST_GAS_TRANSPORT',
        'ST_GLASS_CARRIER',
        'ST_HEAVY',
        'ST_LOW_FRAME_TRAWL',
        'ST_SPLINT_CARRIER',
        'ST_TANK',
        'ST_TIMBER_CARRIER',
    ]);
});
