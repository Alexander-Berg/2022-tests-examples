const buildMarkModelNameplateFromParams = require('./buildMarkModelNameplateFromParams');

it('should return "" if no mark in params', () => {
    expect(buildMarkModelNameplateFromParams({})).toEqual('');
});

it('should return "AUDI" for {mark:"AUDI"}', () => {
    expect(buildMarkModelNameplateFromParams({
        mark: 'audi',
    })).toEqual('AUDI');
});

it('should return "AUDI#A4" for {mark:"AUDI", model:"A4"}', () => {
    expect(buildMarkModelNameplateFromParams({
        mark: 'audi',
        model: 'a4',
    })).toEqual('AUDI#A4');
});

it('should return "AUDI#A4##123" for {mark:"AUDI", model:"A4",super_gen:"123"}', () => {
    expect(buildMarkModelNameplateFromParams({
        mark: 'audi',
        model: 'a4',
        super_gen: '123',
    })).toEqual('AUDI#A4##123');
});
