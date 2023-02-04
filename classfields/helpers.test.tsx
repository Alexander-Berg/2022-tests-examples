import _ from 'lodash';

import { getColumns } from './helpers';

describe('function getColumns()', () => {
    it('правильно разбивает массив на части', () => {
        const groups = [
            { name: 'a', values: [ 'a', 'a', 'a' ] },
            { name: 'b', values: [ 'b' ] },
            { name: 'c', values: [ 'c', 'c', 'c' ] },
        ];

        const { firstColumn, secondColumn } = getColumns(groups, false);
        expect(firstColumn).toEqual(groups.slice(0, 2));
        expect(secondColumn).toEqual(groups.slice(2));
    });

    it('учитывает заголовки групп и отступы', () => {
        const groups = [
            { name: 'a', values: [ 'a' ] },
            { name: 'b', values: [ 'b' ] },
            { name: 'c', values: [ 'c' ] },
            { name: 'd', values: [ 'd' ] },
            { name: 'e', values: [ 'e', 'e', 'e' ] },
        ];

        const { firstColumn, secondColumn } = getColumns(groups, false);
        expect(firstColumn).toEqual(groups.slice(0, 3));
        expect(secondColumn).toEqual(groups.slice(3));
    });

    it('первая колонка может быть меньше чем вторая', () => {
        const groups = [
            { name: 'a', values: _.fill(Array(5), 'a') },
            { name: 'b', values: _.fill(Array(8), 'b') },
        ];

        const { firstColumn, secondColumn } = getColumns(groups, false);
        expect(firstColumn).toEqual(groups.slice(0, 1));
        expect(secondColumn).toEqual(groups.slice(1));
    });

    describe('если передан cut', () => {
        it('в каждом столбце будет только по одной группе', () => {
            const groups = [
                { name: 'a', values: _.fill(Array(5), 'a') },
                { name: 'b', values: _.fill(Array(3), 'b') },
                { name: 'c', values: _.fill(Array(1), 'c') },
                { name: 'd', values: _.fill(Array(7), 'd') },
                { name: 'e', values: _.fill(Array(2), 'e') },
            ];

            const { firstColumn, secondColumn } = getColumns(groups, true);
            expect(firstColumn).toHaveLength(1);
            expect(secondColumn).toHaveLength(1);
        });

        it('максимальный дифф между длинами будет 2', () => {
            const groups = [
                { name: 'a', values: _.fill(Array(5), 'a') },
                { name: 'b', values: [ 'b' ] },
            ];

            const { firstColumn, secondColumn } = getColumns(groups, true);
            expect(firstColumn[0].values).toHaveLength(3);
            expect(secondColumn[0].values).toHaveLength(1);
        });

        it('максимальный размер будет 8', () => {
            const groups = [
                { name: 'a', values: _.fill(Array(12), 'a') },
                { name: 'b', values: _.fill(Array(10), 'a') },
            ];

            const { firstColumn, secondColumn } = getColumns(groups, true);
            expect(firstColumn[0].values).toHaveLength(8);
            expect(secondColumn[0].values).toHaveLength(8);
        });

        it('в первой колонке будет всегда больше опций или равно чем во второй', () => {
            const groups = [
                { name: 'a', values: _.fill(Array(5), 'a') },
                { name: 'b', values: _.fill(Array(8), 'b') },
            ];

            const { firstColumn, secondColumn } = getColumns(groups, true);
            expect(firstColumn[0].values).toHaveLength(5);
            expect(secondColumn[0].values).toHaveLength(5);
        });

        it('правильно обрабатывает одну группу', () => {
            const groups = [
                { name: 'a', values: _.fill(Array(5), 'a') },
            ];

            const { firstColumn, secondColumn } = getColumns(groups, true);
            expect(firstColumn[0].values).toHaveLength(2);
            expect(secondColumn).toEqual([]);
        });
    });
});
