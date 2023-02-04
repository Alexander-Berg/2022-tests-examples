const de = require('descript');
const cleanDescriptResult = require('./cleanResult');

it('should not remove valid responses', () => {
    expect(
        cleanDescriptResult({
            data1: { foo: 'bar' },
            data2: { foo: 'baz' },
        }),
    ).toEqual({
        data1: { foo: 'bar' },
        data2: { foo: 'baz' },
    });
});

it('should remove DEPS_ERROR from result', () => {
    expect(
        cleanDescriptResult({
            data1: { foo: 'bar' },
            data2: de.error({
                id: 'DEPS_ERROR',
            }),
        }),
    ).toEqual({
        data1: { foo: 'bar' },
    });
});

it('should remove BLOCK_GUARDED from result', () => {
    expect(
        cleanDescriptResult({
            data1: { foo: 'bar' },
            data2: de.error({
                id: 'BLOCK_GUARDED',
            }),
        }),
    ).toEqual({
        data1: { foo: 'bar' },
    });
});

it('should remove custom error from result', () => {
    expect(
        cleanDescriptResult({
            data1: { foo: 'bar' },
            data2: { error: 'AUTH_ERROR', status: 'ERROR' },
        }),
    ).toEqual({
        data1: { foo: 'bar' },
    });
});
