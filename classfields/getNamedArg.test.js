const getNamedArg = require('./getNamedArg');

const ARGV = process.argv;

beforeEach(() => {
    jest.resetModules();
    process.argv = [ '', 'aadfdf.js', '', 'part=5', '', 'total_parts=10' ];
});

afterAll(() => {
    process.env = ARGV;
});

it('должен вернуть искомый аргумент part', () => {
    expect(getNamedArg('part')).toEqual('5');
});

it('должен вернуть искомый аргумент total_parts', () => {
    expect(getNamedArg('total_parts')).toEqual('10');
});

it('должен не вернуть аргумент random', () => {
    expect(getNamedArg('random')).toBeUndefined();
});
