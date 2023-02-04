module.exports = function(bh) {
    bh.lib.global = bh.lib.global || {};
    bh.lib.global.id = 'realty-partner';
    bh.lib.global.version = '';
    bh.lib.global.isDebug = false;
    bh.lib.global.environment = 'testing';
    bh.lib.global.staticHost = '//yastatic.net/s3/vertis-frontend/vertis-front-partner/{{VERSION}}';
};
