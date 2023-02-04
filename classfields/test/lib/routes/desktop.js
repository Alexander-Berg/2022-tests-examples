var Susanin = require('susanin'),
    susanin = new Susanin();

susanin
    .addRoute({
        name : 'catalog',
        pattern : '/<mark>/<model>/<cid>(/)'
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
