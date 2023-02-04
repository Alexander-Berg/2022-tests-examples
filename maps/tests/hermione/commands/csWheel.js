/**
 * Команда для зума колёсиком
 *
 * @name browser.csWheel
 * @param {[number, number]} point - точка зума колёсиком
 * @param {Boolean} direction - true/false, Прямое/обратное направление зума колёсиком на карте
 */
module.exports = function (point, direction) {
    return this
        .execute(function (point, direction) {
            var element = window.document.elementFromPoint(point[0], point[1]);

            function getMouseEvent(type, coordinates) {
                return new window.MouseEvent(type, {
                    bubbles: true,
                    cancelable: true,
                    clientX: coordinates[0],
                    clientY: coordinates[1],
                    deltaX: 0,
                    deltaY: 4
                });
            }

            element.dispatchEvent(getMouseEvent('wheel', point));
        }, point, direction);
};