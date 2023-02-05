ymaps.modules.define(util.testfile(), [
    'multiRoute.model.common.combinePointModels'
], function (provide, combinePointModels) {
    describe('multiRoute.model.common.combinePointModels', function () {
        it('combines partly empty points correctly', function () {
            expect(combinePointModels([], [], [])).to.eql([]);
            expect(combinePointModels([], ['via'], [0])).to.eql(['via']);
            expect(combinePointModels(['way'], [], [])).to.eql(['way']);
        });

        it('combines points correctly', function () {
            expect(combinePointModels(['start', 'end'], ['via'], [1]))
                .to.eql(['start', 'via', 'end']);
            expect(combinePointModels(['start', 'end'], ['via1', 'via2'], [1, 2]))
                .to.eql(['start', 'via1', 'via2', 'end']);
            expect(combinePointModels(['start', 'middle', 'end'], ['via1', 'via2'], [1, 3]))
                .to.eql(['start', 'via1', 'middle', 'via2', 'end']);
        });
    });

    provide({});
});
