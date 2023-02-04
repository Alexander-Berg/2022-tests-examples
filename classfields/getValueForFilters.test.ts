import filtersMock from 'auto-core/models/equipment/mock';

import getValueForFilters from './getValueForFilters';

it('правильно формирует параметры', () => {
    const result = getValueForFilters(filtersMock, [ 'laser-lights', 'led-lights', 'abs', 'airbag-driver', 'hatch' ]);

    expect(result).toEqual([ 'laser-lights,led-lights', 'abs', 'hatch', 'airbag-driver' ]);
});
