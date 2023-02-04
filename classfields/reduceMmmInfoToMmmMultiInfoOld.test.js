const reduceMmmInfoToMmmMultiInfoOld = require('./reduceMmmInfoToMmmMultiInfoOld');

it('должен сгруппировать ммм по марке и модели', () => {
    const mmmInfo = [
        { mark: 'BMW', model: 'X5', nameplate: '1', generation: '222' },
        { mark: 'BMW', model: 'X5', nameplate: '1', generation: '111' },
        { mark: 'AUDI', model: 'Q5', nameplate: '1', generation: '222' },
        { mark: 'AUDI', model: 'Q5', nameplate: '2', generation: '111' },
        { mark: 'AUDI', model: 'Q3' },
        { vendor: 'VENDOR1' },
    ];
    const mmmMultiInfo = [
        { mark: 'BMW', model: 'X5', nameplate: '1', generations: [ '222', '111' ] },
        { mark: 'AUDI', model: 'Q5', nameplate: '1', generations: [ '222' ] },
        { mark: 'AUDI', model: 'Q5', nameplate: '2', generations: [ '111' ] },
        { mark: 'AUDI', model: 'Q3', generations: [] },
        { vendor: 'VENDOR1' },
    ];
    expect(reduceMmmInfoToMmmMultiInfoOld(mmmInfo)).toEqual(mmmMultiInfo);
});
