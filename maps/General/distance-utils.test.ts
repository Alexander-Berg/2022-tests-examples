import * as distanceUtils from './distance-utils';

describe('distance utils', () => {
    describe('round distance', () => {
        it('infinite significance', () => {
            expect(distanceUtils.roundDistance(0.3)).toEqual(0);
            expect(distanceUtils.roundDistance(0.9)).toEqual(1);
            expect(distanceUtils.roundDistance(1)).toEqual(1);
            expect(distanceUtils.roundDistance(1.1)).toEqual(1);
            expect(distanceUtils.roundDistance(1.23)).toEqual(1);
            expect(distanceUtils.roundDistance(1.49)).toEqual(1);
            expect(distanceUtils.roundDistance(1.5)).toEqual(2);
            expect(distanceUtils.roundDistance(1.51)).toEqual(2);
            expect(distanceUtils.roundDistance(9.91)).toEqual(10);
            expect(distanceUtils.roundDistance(41.91)).toEqual(42);
            expect(distanceUtils.roundDistance(49.91)).toEqual(50);
            expect(distanceUtils.roundDistance(540.91)).toEqual(541);
            expect(distanceUtils.roundDistance(549.91)).toEqual(550);
            expect(distanceUtils.roundDistance(1239.91)).toEqual(1240);
        });

        it('with significance of 0', () => {
            expect(() => distanceUtils.roundDistance(540.91, 0)).toThrow("Can't round to significance of 0.");
        });

        it('with significance of 1', () => {
            expect(distanceUtils.roundDistance(0.3, 1)).toEqual(0);
            expect(distanceUtils.roundDistance(0.9, 1)).toEqual(1);
            expect(distanceUtils.roundDistance(1, 1)).toEqual(1);
            expect(distanceUtils.roundDistance(1.1, 1)).toEqual(1);
            expect(distanceUtils.roundDistance(1.23, 1)).toEqual(1);
            expect(distanceUtils.roundDistance(1.49, 1)).toEqual(1);
            expect(distanceUtils.roundDistance(1.5, 1)).toEqual(2);
            expect(distanceUtils.roundDistance(1.51, 1)).toEqual(2);
            expect(distanceUtils.roundDistance(9.91, 1)).toEqual(10);
            expect(distanceUtils.roundDistance(41.91, 1)).toEqual(40);
            expect(distanceUtils.roundDistance(49.91, 1)).toEqual(50);
            expect(distanceUtils.roundDistance(540.91, 1)).toEqual(500);
            expect(distanceUtils.roundDistance(549.91, 1)).toEqual(500);
            expect(distanceUtils.roundDistance(1239.91, 1)).toEqual(1000);
        });

        it('with significance of 2', () => {
            expect(distanceUtils.roundDistance(0.3, 2)).toEqual(0);
            expect(distanceUtils.roundDistance(0.9, 2)).toEqual(1);
            expect(distanceUtils.roundDistance(1, 2)).toEqual(1);
            expect(distanceUtils.roundDistance(1.1, 2)).toEqual(1);
            expect(distanceUtils.roundDistance(1.23, 2)).toEqual(1);
            expect(distanceUtils.roundDistance(1.49, 2)).toEqual(1);
            expect(distanceUtils.roundDistance(1.5, 2)).toEqual(2);
            expect(distanceUtils.roundDistance(1.51, 2)).toEqual(2);
            expect(distanceUtils.roundDistance(9.91, 2)).toEqual(10);
            expect(distanceUtils.roundDistance(41.91, 2)).toEqual(42);
            expect(distanceUtils.roundDistance(49.91, 2)).toEqual(50);
            expect(distanceUtils.roundDistance(540.91, 2)).toEqual(540);
            expect(distanceUtils.roundDistance(549.91, 2)).toEqual(550);
            expect(distanceUtils.roundDistance(1239.91, 2)).toEqual(1200);
        });
    });

    it('format distance', () => {
        expect(getFormattedDetails(distanceUtils.formatDistance(0.9))).toEqual([1, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(1))).toEqual([1, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(1.1))).toEqual([1, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(1.49))).toEqual([1, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(1.5))).toEqual([2, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(1.51))).toEqual([2, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(9.91))).toEqual([10, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(41.91))).toEqual([42, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(49.91))).toEqual([50, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(540.91))).toEqual([540, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(549.91))).toEqual([550, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(891.91))).toEqual([890, 'm']);
        expect(getFormattedDetails(distanceUtils.formatDistance(901.91))).toEqual([1, 'km']);
        expect(getFormattedDetails(distanceUtils.formatDistance(1239.91))).toEqual([1, 'km']);
        expect(getFormattedDetails(distanceUtils.formatDistance(12391.91))).toEqual([12, 'km']);
        expect(getFormattedDetails(distanceUtils.formatDistance(12591.91))).toEqual([13, 'km']);
        expect(getFormattedDetails(distanceUtils.formatDistance(125912.91))).toEqual([130, 'km']);
        expect(getFormattedDetails(distanceUtils.formatDistance(1249124.91))).toEqual([1200, 'km']);
        expect(getFormattedDetails(distanceUtils.formatDistance(1259124.91))).toEqual([1300, 'km']);
    });
});

function getFormattedDetails(object: React.ReactNode): [number, 'm' | 'km' | null] {
    const [, key, params] = object as [string, string, Record<string, string>];
    const size = key === 'meter' ? 'm' : key === 'kilometer' ? 'km' : null;
    const distance = Number(params.distance);
    return [distance, size];
}
