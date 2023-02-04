/* eslint-env node, mocha */

'use strict';

var registry = require('../lib/registry'),
    expect = require('chai').expect;

describe('registry', function() {
    beforeEach(function() {
        registry.clean();
    });

    describe('status()', function() {
        it('returns code 0 if no checks registered', function() {
            var status = registry.status();
            expect(status.code).to.be.equal(0);
        });

        it('returns proper code for registered checks', function() {
            registry.registerCheck(fakeCheck(2));
            registry.registerCheck(fakeCheck(0));
            registry.registerCheck(fakeCheck(1));

            var status = registry.status();
            expect(status.code).to.be.equal(2);
        });

        it('returns statuses for all registered checks that are not 0', function() {
            registry.registerCheck(fakeCheck(2));
            registry.registerCheck(fakeCheck(0));
            registry.registerCheck(fakeCheck(1));
            registry.registerCheck(fakeCheck(0));
            registry.registerCheck(fakeCheck(2));

            var status = registry.status();
            expect(status.statuses).to.have.lengthOf(3);

            var firstStatus = status.statuses[0];
            expect(firstStatus).to.have.property('code');
            expect(firstStatus).to.have.property('label');
            expect(firstStatus).to.have.property('message');
        });
    });
});

function fakeCheck(status) {
    return {
        status: function() {
            return { code: status || 0, label: 'my label', message: 'my message' };
        }
    };
}
