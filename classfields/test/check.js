/* eslint-env node, mocha */

'use strict';

var Check = require('../').Check,
    sinon = require('sinon'),
    expect = require('chai').expect;

describe('Check', function() {
    var check,
        CHECK_CAPACITY = 5,
        WARN_THRESHOLD = 2,
        CRIT_THRESHOLD = 4;

    beforeEach(function() {
        check = new Check('my-check', {
            capacity: CHECK_CAPACITY,
            warnThreshold: WARN_THRESHOLD,
            critThreshold: CRIT_THRESHOLD,
        });
    });

    it('implements interface', function() {
        expect(Check).to.respondTo('status');
        expect(Check).to.respondTo('pass');
        expect(Check).to.respondTo('fail');
    });

    it('throws if capacity is less than critThreshold', function() {
        function newCheck() {
            return new Check({ capacity: 2, critThreshold: 3 });
        }
        expect(newCheck).to.throw(RangeError);
    });

    it('throws if critThreshold is less or equal to warnThreshold', function() {
        expect(function() {
            return new Check({ capacity: 5, critThreshold: 2, warnThreshold: 2 });
        }).to.throw(RangeError);

        expect(function() {
            return new Check({ capacity: 5, critThreshold: 1, warnThreshold: 2 });
        }).to.throw(RangeError);
    });

    it('returns code "ok" by default', function() {
        var status = check.status();
        expect(status.code).to.equal(Check.STATUS_OK);
    });

    it('returns code "ok" if checks passed', function() {
        check.pass();

        var status = check.status();
        expect(status.code).to.equal(Check.STATUS_OK);
    });

    it('returns code "ok" if all checks passed', function() {
        check.pass();
        check.pass();
        check.pass();
        check.pass();
        check.pass();

        var status = check.status();
        expect(status.code).to.equal(Check.STATUS_OK);
    });

    it('returns code "warn" if warnThreshold checks failed', function() {
        check.fail();
        check.fail();

        var status = check.status();
        expect(status.code).to.equal(Check.STATUS_WARN);
    });

    it('returns code "warn" if less than critThreshold checks failed', function() {
        check.fail();
        check.fail();
        check.fail();

        var status = check.status();
        expect(status.code).to.equal(Check.STATUS_WARN);
    });

    it('returns code "crit" if critThreshold checks failed', function() {
        check.fail();
        check.fail();
        check.fail();
        check.fail();

        var status = check.status();
        expect(status.code).to.equal(Check.STATUS_CRIT);
    });

    it('returns status struct with proper message', function() {
        var error = 'something happen',
            status;

        check.fail(error);
        check.fail(error);
        check.fail(error);
        check.fail(error);

        status = check.status();
        expect(status.label).to.equal('my-check');
        expect(status.message).to.equal(error);

        check.pass();

        status = check.status();
        expect(status.label).to.equal('my-check');
        expect(status.message).to.equal(error);
    });

    it('replaces old statuses with new ones', function() {
        var error1 = 'something happen 1',
            error2 = 'something happen 2',
            status;

        check.fail(error1);
        check.fail(error1);

        status = check.status();
        expect(status.code).to.equal(Check.STATUS_WARN);
        expect(status.message).to.equal(error1);

        check.fail(error2);
        check.fail(error2);

        status = check.status();
        expect(status.code).to.equal(Check.STATUS_CRIT);
        expect(status.message).to.equal(error2);

        check.pass();
        check.pass();

        status = check.status();
        expect(status.code).to.equal(Check.STATUS_WARN);
        expect(status.message).to.equal(error2);

        check.pass();
        check.pass();

        status = check.status();
        expect(status.code).to.equal(Check.STATUS_OK);
        expect(status.message).to.be.undefined;
    });

    describe('expire', function() {
        var clock,
            CHECK_EXPIRE = 10;

        before(function() {
            clock = sinon.useFakeTimers();
        });

        after(function() {
            clock.restore();
        });

        it('considers only non-expired results', function() {
            check = new Check('my-check', {
                expire: CHECK_EXPIRE,
                capacity: CHECK_CAPACITY,
                warnThreshold: WARN_THRESHOLD,
                critThreshold: CRIT_THRESHOLD,
            });

            var error = 'something happen';

            check.fail(error);
            check.fail(error);

            clock.tick(CHECK_EXPIRE - 1);
            check.fail(error);

            var status = check.status();
            expect(status.code).to.equal(Check.STATUS_WARN);

            clock.tick(CHECK_EXPIRE + 1);

            status = check.status();
            expect(status.code).to.equal(Check.STATUS_OK);
        });
    });
});
