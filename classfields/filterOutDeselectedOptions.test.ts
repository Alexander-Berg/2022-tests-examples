import mock from '../mock';

import filterOutDeselectedOptions from './filterOutDeselectedOptions';

it('отфильтрует опции которых нет в словаре и которые были отжаты пользователем ранее', () => {
    const allOptions = mock.value().options;
    const deselectedOptions = mock.value().deselectedOptions;
    const dictionary = {
        usb: { code: 'usb', name: 'unknown', group: 'unknown' },
        esp: { code: 'esp', name: 'unknown', group: 'unknown' },
        aux: { code: 'aux', name: 'unknown', group: 'unknown' },
        airbag: { code: 'airbag', name: 'unknown', group: 'unknown' },
    };
    const result = filterOutDeselectedOptions(allOptions, deselectedOptions, dictionary);

    expect(result).toEqual([ 'esp', 'aux', 'airbag' ]);
});
