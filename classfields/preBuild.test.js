const preBuild = require('./preBuild');

describe('versus', () => {
    const correctResult = {
        first_mark: 'audi',
        first_model: 'a6',
        second_mark: 'bmw',
        second_model: 'x4',
    };

    it('должен привести к нижнему регистру все параметры', () => {
        const newRouteParams = preBuild({
            first_mark: 'AUDI',
            first_model: 'A6',
            second_mark: 'bmw',
            second_model: 'x4',
        });
        expect(newRouteParams).toEqual(correctResult);
    });

    it('должен отсортировать тачки по алфавиту', () => {
        const newRouteParams = preBuild({
            first_mark: 'BMW',
            first_model: 'X4',
            second_mark: 'AUDI',
            second_model: 'A6',
        });
        expect(newRouteParams).toEqual(correctResult);
    });

    it('должен отсортировать тачки по алфавиту, с учётом того что они одной марки', () => {
        const newRouteParams = preBuild({
            first_mark: 'AUDI',
            first_model: 'S7',
            second_mark: 'AUDI',
            second_model: 'A6',
        });
        expect(newRouteParams).toEqual({
            first_mark: 'audi',
            first_model: 'a6',
            second_mark: 'audi',
            second_model: 's7',
        });
    });
});
