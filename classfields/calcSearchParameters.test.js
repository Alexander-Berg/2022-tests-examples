const calcSearchParameters = require('./calcSearchParameters');

const DATA = {
    array: [ 'yo', 'whats up' ], // дает 2
    empty_array: [], // дает 0
    has_video: true, // дает 1
    has_audio: false, // дает 1
    test: undefined, // дает 0
};

it('calcSearchParameters должен правильно посчитать фильтры', () => {
    const result = calcSearchParameters(DATA);
    expect(result).toEqual(4);
});
