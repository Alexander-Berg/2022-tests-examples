module.exports = {
    count: {
        // Количественные лимиты
        maps: 9999999,
        geoObjects: 10000,
        vertexes: 1000,
        updateGeoObjectsPerRequest: 20
    },
    size: {
        // Лимиты на длину записи
        mapName: 256,
        mapDescription: 1000 * 32,
        iconCaption: 256,
        iconContent: 3,
        iconDescription: 1000 * 32
    }
};
