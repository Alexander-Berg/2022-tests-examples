var Susanin = require('susanin'),
    susanin = new Susanin();

susanin
    .addRoute({
        name : 'default',
        pattern : '/(index.xml(/))(<controller>(/))'
    });

susanin
    .addRoute({
        name : 'advertisement',
        pattern : '/advertisement/<offer_id>',
        conditions : {
            offer_id : '\\d+'
        }
    });

susanin
    .addRoute({
        name : 'offers',
        pattern : '/<mark>/<model>/<conf_id>/offers',
        conditions : {
            conf_id : '\\d+'
        }
    });

module.exports = susanin;
