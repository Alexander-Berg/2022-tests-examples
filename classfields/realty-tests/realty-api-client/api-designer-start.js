var path = require('path');
var express = require('express');
var ramlStore = require('express-raml-store');
var open = require('open');

var app = express();

app.use('', ramlStore(path.join(__dirname, 'src', 'main', 'resources', 'api')));
var server = app.listen(3000, function () {
    console.log('Open http://localhost:%d/ to browse api-designer', server.address().port);
    open('http://localhost:' + server.address().port)
});