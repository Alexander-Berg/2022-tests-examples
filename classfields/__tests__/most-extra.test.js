import * as most from 'most';
import { collect, combineObject, pairwise, withInitialValueFromStream, shareReplay } from '../most-extra';

describe('pairwise', () => {
    it('should work', async() => {
        const result = await most.from([ 1, 2, 3 ])
            .thru(pairwise)
            .thru(collect);

        expect(result).toEqual([
            [ 1, 2 ],
            [ 2, 3 ]
        ]);
    });

    it('should work with empty stream', async() => {
        const result = await most.empty()
            .thru(pairwise)
            .thru(collect);

        expect(result).toEqual([]);
    });
});

describe('collect', () => {
    it('should work', async() => {
        const result = await most.from([ 1, 2, 3 ])
            .thru(collect);

        expect(result).toEqual([ 1, 2, 3 ]);
    });
});

describe('combineObject', () => {
    it('should work', async() => {
        const result = await combineObject({
            a: most.of('a'),
            b: most.of('b')
        })
            .thru(collect);

        expect(result).toEqual([ { a: 'a', b: 'b' } ]);
    });
});

describe('withInitialValueFromStream', () => {
    it('should work', async() => {
        const result = await withInitialValueFromStream(most.from([ 1, 2 ]), most.of(0))
            .thru(collect);

        expect(result).toEqual([ 0, 1, 2 ]);
    });

    it('should take only one first value', async() => {
        const result = await withInitialValueFromStream(most.from([ 1, 2 ]), most.from([ 0, 5 ]))
            .thru(collect);

        expect(result).toEqual([ 0, 1, 2 ]);
    });
});

describe('shareReplay', () => {
    it('should hold value', () => {
        expect.assertions(2);
        const stream = shareReplay(most.from([ 1, 2, 3 ]));

        stream.take(1).forEach(x => {
            expect(x).toBe(1);
        });

        stream.take(1).forEach(x => {
            expect(x).toBe(1);
        });
    });
});
