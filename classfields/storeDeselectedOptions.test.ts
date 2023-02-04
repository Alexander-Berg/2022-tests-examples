jest.mock('auto-core/react/lib/localstorage', () => ({
    setItem: jest.fn(),
}));
jest.mock('www-poffer/react/utils/retrieveDeselectedOptions');

import { setItem } from 'auto-core/react/lib/localstorage';

import retrieveDeselectedOptions from 'www-poffer/react/utils/retrieveDeselectedOptions';

import storeDeselectedOptions from './storeDeselectedOptions';

const retrieveDeselectedOptionsMock = retrieveDeselectedOptions as jest.MockedFunction<typeof retrieveDeselectedOptions>;

const draftId = 'draft-id';
const otherValue = { 'other-draft': [ 'foo', 'bar' ] };

it('если в сторадже нет ничего для данного драфта, добавит все опции', () => {
    retrieveDeselectedOptionsMock.mockReturnValueOnce(otherValue);

    storeDeselectedOptions({ usb: true, esp: false, aux: false }, draftId);

    expect(setItem).toHaveBeenCalledTimes(1);
    expect(setItem).toHaveBeenCalledWith('deselected_options', JSON.stringify({
        ...otherValue,
        [draftId]: [ 'esp', 'aux' ],
    }));
});

it('если в сторадже что-то есть для данного драфта, сморжит массивы', () => {
    retrieveDeselectedOptionsMock.mockReturnValueOnce({
        ...otherValue,
        [draftId]: [ 'some', 'stuff' ],
    });

    storeDeselectedOptions({ usb: true, esp: false, aux: false, some: true, stuff: false }, draftId);

    expect(setItem).toHaveBeenCalledTimes(1);
    expect(setItem).toHaveBeenCalledWith('deselected_options', JSON.stringify({
        ...otherValue,
        [draftId]: [ 'stuff', 'esp', 'aux' ],
    }));
});
