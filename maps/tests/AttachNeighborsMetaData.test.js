const expect = require('chai').expect;
const utilNeighbors = require('../lib/AttachNeighborsMetaData.js');

describe('AttachNeighborsMetaData', function () {
    describe('Метод ._createArcToIso3166Dictionary', function () {
        it('создает словарь с положительными арками', function () {
            var paths = {
                'A': [[1, 2, 3], [4, 5, 6, 1]],
                'B': [[1, 2], [8, 5, 9]]
            };

            expect(utilNeighbors._createArcToIso3166Dictionary(paths))
                .to.eql({
                    1: ['A', 'B'],
                    2: ['A', 'B'],
                    3: ['A'],
                    4: ['A'],
                    5: ['A', 'B'],
                    6: ['A'],
                    8: ['B'],
                    9: ['B']
                });
        });

        it('создает словарь с отрицательными арками', function () {
            var paths = {
                'A': [[-1, 1]],
                'B': [[-1, 1], [-2]]
            };

            expect(utilNeighbors._createArcToIso3166Dictionary(paths))
                .to.eql({
                    1: ['A', 'B'],
                    2: ['B']
                });
        });
    });

    describe('Метод ._utilNeighbors', function () {
        it('правильно расчитывает соседей если массив путей пуст', function () {
            var paths = {}; 
            expect(utilNeighbors._calculateNeighbors(paths)).to.eql({});
        });

        it('правильно расчитывает соседей если есть только один регион', function () {
            var paths = { 'A': [[1]] };
            expect(utilNeighbors._calculateNeighbors(paths)).to.eql({ 'A': [] });
        });

        it('правильно расчитывает соседей если регионы не соседние', function () {
            var paths = { 'A': [[1]], 'B': [[2]] };
            expect(utilNeighbors._calculateNeighbors(paths)).to.eql({ 'A': [], 'B': [] });
        });

        it('правильно расчитывает соседей', function () {
            var paths = {
                'A': [[1, 2, 3], [4, 5, 6, 1, 2]],
                'B': [[1, 2], [8, 5, 9]],
                'C': [[1]],
                'D': [[6]],
                'E': [[111]]
            };

            expect(utilNeighbors._calculateNeighbors(paths))
                .to.eql({
                    'A': ['B', 'C', 'D'],
                    'B': ['A', 'C'],
                    'C': ['A', 'B'],
                    'D': ['A'],
                    'E': []
                });
        });
    });

    describe('Метод .attachNeighbors', function () {
        it('присоеденяет данные о соседях к данным регионов', function () {
            var regionsJson = {
                regions: { 'A':  {}, 'B': {}, 'C': {}},
                paths: { 'A': [[1, 2]], 'B': [[2]], 'C': [[3]] },
                ways: {0: '', 1: '', 2: '', 3: ''}
            };

            var result = utilNeighbors.attachNeighbors(regionsJson);
            expect(result.regions.A.neighbors).to.eql(['B']);
            expect(result.regions.B.neighbors).to.eql(['A']);
            expect(result.regions.C.neighbors).to.eql([]);
        }); 
    });
});
