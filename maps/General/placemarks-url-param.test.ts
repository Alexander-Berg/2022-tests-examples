import * as placemarksUrlParam from './placemarks-url-param';

describe('placemarksUrlParam', () => {
    describe('parse()', () => {
        it('should skip placemark in invalid format', () => {
            expect(placemarksUrlParam.parse('')).toEqual([]);
            expect(placemarksUrlParam.parse('1')).toEqual([]);
            expect(placemarksUrlParam.parse('1,')).toEqual([]);
            expect(placemarksUrlParam.parse('1,2,3,4')).toEqual([]);
            expect(placemarksUrlParam.parse('1.2')).toEqual([]);
            expect(placemarksUrlParam.parse('pmbl')).toEqual([]);
        });

        it("should parse placemark's coordinates", () => {
            expect(placemarksUrlParam.parse('1,2')).toEqual([{coordinates: [1, 2]}]);
        });

        it("should parse placemark's coordinates and color", () => {
            expect(placemarksUrlParam.parse('1,2,pmbl')).toEqual([
                {
                    coordinates: [1, 2],
                    color: 'pmbl'
                }
            ]);
        });

        it('should parse placemark collection', () => {
            expect(placemarksUrlParam.parse('1,2,pmbl~3,4,pmrd')).toEqual([
                {
                    coordinates: [1, 2],
                    color: 'pmbl'
                },
                {
                    coordinates: [3, 4],
                    color: 'pmrd'
                }
            ]);
        });

        it('should skip invalid placemark in collection', () => {
            expect(placemarksUrlParam.parse('1,invalid~3,4,pmrd')).toEqual([
                {
                    coordinates: [3, 4],
                    color: 'pmrd'
                }
            ]);
        });
    });
});
