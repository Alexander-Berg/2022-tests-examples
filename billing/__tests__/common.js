jest.mock('common/utils/formatters');

import { removeEmptyFields, getPersonTypeItems } from '../common';
import { formatMessage } from 'common/utils/formatters';

describe('Common data processor', () => {
    describe('removeEmptyFields', () => {
        it('remove nulls and empty strings', () => {
            expect(
                removeEmptyFields({
                    a: 'a',
                    b: null,
                    c: 2,
                    d: 0,
                    e: '',
                    f: false
                })
            ).toEqual({ a: 'a', c: 2, d: 0, f: false });
        });
    });
});
