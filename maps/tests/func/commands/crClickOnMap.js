/**
 * Команда для клика по карте по заданным координатам и закрывает балун
 *
 * @name browser.crClickOnMap
 * @param {Object[]} [coords] - [xoffset, yoffset]
 */
module.exports = function (coords) {
    coords = coords ? coords : [100, 500];
    return this
        .leftClick(PO.ymaps.map(), coords[0], coords[1])
        .pause(150)
        .keys('\uE007') // Enter
        .pause(150);
};
