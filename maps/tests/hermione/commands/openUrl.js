/**
 * Команда для формирования урла теста на основе относительного и параметров
 *
 * @name browser.openUrl
 * @param {String} url - местоположение теста относительно корневой папки с тестируемыми страничками
 */
module.exports = function (url, options) {
    var hcRouter = '&host_config%5Binthosts%5D%5Brouter%5D=http%3A%2F%2Fpodrick.c.maps.yandex-team.ru%2Fmocks%2Froutes%2Fauto%2F';
    var hcRouterMass = '&host_config%5Binthosts%5D%5Bmasstransit%5D=http%3A%2F%2Fpodrick.c.maps.yandex-team.ru%2Fmocks%2Froutes%2Fmasstransit%2F';
    var hcRouterPed = '&host_config%5Binthosts%5D%5Bpedestrian%5D=http%3A%2F%2Fpodrick.c.maps.yandex-team.ru%2Fmocks%2Froutes%2Fpedestrian%2F';
    var hcTile = '&host_config[hosts][mapTiles]=https://podrick.c.maps.yandex-team.ru/mocks/layers/white/?%c';
    var hcTileBlue = '&host_config[hosts][mapTiles]=https://podrick.c.maps.yandex-team.ru/mocks/layers/blue/?%c';
    var hcTileWhite = '&host_config[hosts][mapTiles]=https://podrick.c.maps.yandex-team.ru/mocks/layers/white/';
    var hcSearch = '&host_config%5Binthosts%5D%5Bsearch%5D=http%3A%2F%2Fpodrick.c.maps.yandex-team.ru%2Fmocks%2Fmetasearch%2F';
    var hcSuggest = '&host_config[hosts][suggestExternalApi]=https://podrick.c.maps.yandex-team.ru/mocks/suggest/';
    var hcTileMock;
    if(options && options.tileMock === 'withParameters'){
        hcTileMock = hcTile;
    } else if(options && options.tileMock === 'blueWithParameters'){
        hcTileMock = hcTileBlue;
    } else if(options && options.tileMock === 'default'){
        hcTileMock = '';
    } else {
        hcTileMock = hcTileWhite;
    }
    var url = (testsPath ? testsPath : '') + 'hermione/pages/' + url + '?' + (apiPath ? 'apiPath=' + apiPath : '') + '&enterprise=' + !!(options && options.enterprise) + hcRouter + hcRouterMass + hcRouterPed + hcSearch + hcSuggest + hcTileMock;
    return this.url(url);
}
