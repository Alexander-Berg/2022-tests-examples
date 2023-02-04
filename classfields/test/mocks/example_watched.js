var ExampleController = require('./example'),
    sinon = require('sinon');

module.exports = sinon.spy(ExampleController.create('example_watched'));
