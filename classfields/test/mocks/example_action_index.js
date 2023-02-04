var ExampleController = require('./example'),
    vow = require('vow'),
    sinon = require('sinon'),
    ExampleActionIndexController = ExampleController.create('example_action_index');

ExampleActionIndexController.prototype.action_index = sinon.spy(function(beforeResult) {
    /* jshint unused:false */
    return vow.fulfill({ ok: true });
});

module.exports = ExampleActionIndexController;
