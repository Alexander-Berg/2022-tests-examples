import { calculateEdgePoints } from '../';

describe('graph', () => {
    describe('edge points', () => {
        it('generates edge points', () => {
            expect(calculateEdgePoints(99, 3)).toEqual([
                99,
                66,
                33,
                0
            ]);
        });

        it('generates edge points for empty data', () => {
            expect(calculateEdgePoints(0, 3)).toEqual([
                3,
                2,
                1,
                0
            ]);
        });

        it('rounds top edge to provided step', () => {
            expect(calculateEdgePoints(100, 30)).toEqual([
                120,
                80,
                40,
                0
            ]);
        });
    });
});
