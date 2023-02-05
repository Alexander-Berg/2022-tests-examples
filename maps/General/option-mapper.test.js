ymaps.modules.define(util.testfile(), [
    'option.Mapper'
], function(provide, OptionMapper) {
    describe('option.Mapper', function () {
        it('should fire rulechange', function () {
            var mapper = new OptionMapper();
            var eventCounter = 0;
            mapper.events.add('rulechange', function () { eventCounter++; });
            expect(eventCounter).to.be(0);

            mapper.setRule({ name: ['a'], rule: 'plain' });
            expect(eventCounter).to.be(1);

            mapper
                .setRule({ name: ['b'], rule: 'plain' })
                .setRule({ key: ['c'], rule: 'plain' });
            expect(eventCounter).to.be(3);

            mapper.unsetRule({ name: 'b' });
            mapper.unsetRule({ key: 'c' });
            expect(eventCounter).to.be(5);
        });

        it('should map keys', function () {
            var mapper = new OptionMapper('prefixed');

            expect(mapper.resolve('contentLayout', 'balloon')).to.be('balloonContentLayout');

            mapper.setRule({
                name: ['hint'],
                rule: 'plain'
            });

            expect(mapper.resolve('contentLayout', 'hint')).to.be('contentLayout');

            mapper.setRule({
                key: ['contentLayout'],
                rule: function (key, name) { return name + 'CL'; }
            });

            expect(mapper.resolve('contentLayout', 'balloon')).to.be('balloonCL');
            expect(mapper.resolve('contentLayout', 'hint')).to.be('contentLayout');

            mapper
                .unsetRule({ key: 'contentLayout' })
                .unsetRule({ name: 'hint' })
                .unsetRule({
                    key: ['contentLayout'],
                    name: 'hint'
                });

            expect(mapper.resolve('contentLayout', 'balloon')).to.be('balloonContentLayout');
            expect(mapper.resolve('contentLayout', 'hint')).to.be('hintContentLayout');

            mapper.setRule({
                key: 'position',
                name: ['trafficControl'],
                rule: ['prefixed', 'plain']
            });

            expect(mapper.resolve('position', 'trafficControl')).to.eql(['trafficControlPosition', 'position']);
        });
    });

    provide();
});
