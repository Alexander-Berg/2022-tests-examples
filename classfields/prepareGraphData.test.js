const MockDate = require('mockdate');

const prepareGraphData = require('./prepareGraphData');

beforeEach(() => {
    MockDate.set('2020-01-01');
});

afterEach(() => {
    MockDate.reset();
});

it('должен вернуть исходный массив, если в нем не больше 6 точек', () => {
    const data = [ { age: 1 }, { age: 2 }, { age: 3 }, { age: 4 }, { age: 5 }, { age: 6 } ];
    expect(prepareGraphData(data)).toEqual(data);
});

it('должен вернуть новый массив, если в нем больше 6 точек', () => {
    const data = [ { age: 0 }, { age: 1 }, { age: 2 }, { age: 3 }, { age: 4 }, { age: 5 }, { age: 6 }, { age: 10 } ];
    const offer = { documents: { year: 2015 } };
    expect(prepareGraphData(data, offer)).toEqual([ { age: 1 }, { age: 2 }, { age: 3 }, { age: 4 }, { age: 5 }, { age: 6 } ]);
});
