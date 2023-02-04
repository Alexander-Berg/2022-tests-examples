import getFeaturesFromBunker from './getFeaturesFromBunker';

jest.mock('auto-core/lib/util/getBunkerDict', () => jest.fn(x => ({ [x]: true, [`${ x }Label`]: false })));

describe('function "getFeaturesFromBunker"', () => {
    it('принимает строку', () => {
        const result = getFeaturesFromBunker('test');

        expect(result).toEqual({ test: true, testLabel: false });
    });

    it('принимает массив', () => {
        const result = getFeaturesFromBunker([ 'test', 'trivial' ]);

        expect(result).toEqual({ test: true, trivial: true, testLabel: false, trivialLabel: false });
    });
});
