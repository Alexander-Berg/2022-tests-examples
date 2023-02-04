const reduceMmmInfoToMmmMultiInfo = require('./reduceMmmInfoToMmmMultiInfo');

it('должен сгруппировать ммм по марке', () => {
    const mmmInfo = [
        { mark: 'BMW', model: 'X5', nameplate_name: '1', generation: '222' },
        { mark: 'BMW', model: 'X5', nameplate_name: '2', generation: '111' },
        { mark: 'AUDI', model: 'Q5', nameplate_name: '1' },
        { mark: 'AUDI', model: 'Q5', nameplate_name: '2' },
        { mark: 'AUDI', model: 'Q3' },
        { vendor: 'VENDOR1' },
    ];
    const mmmMultiInfo = [
        { mark: 'BMW', models: [ { id: 'X5', nameplates: [ '1', '2' ], generations: [ '222', '111' ] } ] },
        { mark: 'AUDI', models: [ { id: 'Q5', nameplates: [ '1', '2' ], generations: [] }, { id: 'Q3', nameplates: [], generations: [] } ] },
        { vendor: 'VENDOR1' },
    ];
    expect(reduceMmmInfoToMmmMultiInfo(mmmInfo)).toEqual(mmmMultiInfo);
});

it('должен сгруппировать ммм по марке с учетом исключения', () => {
    const mmmInfo = [
        { mark: 'BMW', model: 'X5', nameplate_name: '1', generation: '222' },
        { mark: 'BMW', model: 'X5', nameplate_name: '2', generation: '111' },
        { mark: 'AUDI', model: 'Q5', nameplate_name: '1', exclude: true },
        { mark: 'AUDI', model: 'Q5', nameplate_name: '2', exclude: true },
        { mark: 'AUDI', model: 'Q3', exclude: true },
        { vendor: 'VENDOR1' },
    ];
    const mmmMultiInfo = [
        { mark: 'BMW', models: [ { id: 'X5', nameplates: [ '1', '2' ], generations: [ '222', '111' ] } ] },
        { exclude: true, mark: 'AUDI', models: [ { id: 'Q5', nameplates: [ '1', '2' ], generations: [] }, { id: 'Q3', nameplates: [], generations: [] } ] },
        { vendor: 'VENDOR1' },
    ];
    expect(reduceMmmInfoToMmmMultiInfo(mmmInfo)).toEqual(mmmMultiInfo);
});
