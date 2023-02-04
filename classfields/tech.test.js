const { commercial, moto } = require('./tech');

describe('commercial', () => {

    describe('axis', () => {
        it('should return axis from PublicAPI response', () => {
            expect(commercial.axis({
                truck_info: { axis: 2 },
            })).toEqual('2 оси');
        });
        it('should return axis from Searcher response', () => {
            expect(commercial.axis({
                axis: 2,
            })).toEqual('2 оси');
        });
        describe('plural', () => {
            it('single', () => {
                expect(commercial.axis({
                    axis: 1,
                })).toEqual('1 ось');
            });
            it('few', () => {
                expect(commercial.axis({
                    axis: 2,
                })).toEqual('2 оси');
            });
            it('many', () => {
                expect(commercial.axis({
                    axis: 5,
                })).toEqual('5 осей');
            });
        });
    });

});

describe('moto', () => {

    describe('cylinders', () => {
        it('should return pluralForm for 1', () => {
            expect(moto.cylinders({
                cylinders: 1,
            })).toEqual('1 цилиндр');
        });
        it('should return pluralForm for 2', () => {
            expect(moto.cylinders({
                cylinders: 2,
            })).toEqual('2 цилиндра');
        });
        it('should return pluralForm for 5', () => {
            expect(moto.cylinders({
                cylinders: 5,
            })).toEqual('5 цилиндров');
        });
        it('should return pluralForm for string', () => {
            expect(moto.cylinders({
                cylinders: '1',
            })).toEqual('1 цилиндр');
        });
        it('should return pluralForm for string with non-numbers', () => {
            expect(moto.cylinders({
                cylinders: '1e',
            })).toEqual('1 цилиндр');
        });
    });

    describe('strokes', () => {
        it('should return pluralForm for 1', () => {
            expect(moto.strokes({
                strokes: 1,
            })).toEqual('1 такт');
        });
        it('should return pluralForm for 2', () => {
            expect(moto.strokes({
                strokes: 2,
            })).toEqual('2 такта');
        });
        it('should return pluralForm for 5', () => {
            expect(moto.strokes({
                strokes: 5,
            })).toEqual('5 тактов');
        });
        it('should return pluralForm for string', () => {
            expect(moto.strokes({
                strokes: '1',
            })).toEqual('1 такт');
        });
        it('should return pluralForm for string with non-numbers', () => {
            expect(moto.strokes({
                strokes: '1e',
            })).toEqual('1 такт');
        });
    });

});
