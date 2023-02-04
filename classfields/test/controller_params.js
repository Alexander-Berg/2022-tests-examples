/* global describe, it */
var assert = require('chai').assert;

describe('ControllerParams', function() {
    var Controller = require('../lib').Controller,
        ControllerParams = require('../lib').ControllerParams;

    it('should be correctly applied to Controller.prototype', function() {
        assert.strictEqual(Controller.prototype.hasParam, ControllerParams.hasParam);
        assert.strictEqual(Controller.prototype.hasParams, ControllerParams.hasParams);
        assert.strictEqual(Controller.prototype.getParam, ControllerParams.getParam);
        assert.strictEqual(Controller.prototype.getBooleanParam, ControllerParams.getBooleanParam);
        assert.strictEqual(Controller.prototype.getNumericParam, ControllerParams.getNumericParam);
        assert.strictEqual(Controller.prototype.getParams, ControllerParams.getParams);
    });
});
