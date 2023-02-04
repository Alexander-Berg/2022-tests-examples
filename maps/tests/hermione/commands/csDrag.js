/**
 * Команда для драга
 *
 * @name browser.csDrag
 * @param {[number, number]} start - начальная точка драга
 * @param {[number, number]} end - конечная точка драга
 * @param {Number} inertia - задержка перед отпусканием кнопки мыши после окончания драга, по умолчанию 300мс
 * @param {String} button - left/middle/right, какую кнопку мыши зажимать
 */
module.exports = function (start, end, inertia, button) {
    return this
        .execute(function (start, end, inertia, button) {
            var buttons = {
                    left: 0,
                    middle: 1,
                    right: 2
                },
                buttonId = typeof button !== 'undefined' ? buttons[button] : 0,
                element = window.document.elementFromPoint(start[0], start[1]),
                timeout = inertia || 300;

            function getMouseEvent(type, coordinates) {
                return new window.MouseEvent(type, {
                    button: buttonId,
                    buttons: 1,
                    bubbles: true,
                    cancelable: true,
                    clientX: coordinates[0],
                    clientY: coordinates[1]
                });
            }

            element.dispatchEvent(getMouseEvent('mousedown', start));
            if (typeof button !== 'undefined' && button === 'right') {
                element.dispatchEvent(getMouseEvent('contextmenu', start));
            }
            element.dispatchEvent(getMouseEvent('mousemove', [start[0] + (start[0] - end[0])/2, start[1] + (start[1] - end[1]) / 2]));
            element.dispatchEvent(getMouseEvent('mousemove', end));

            setTimeout(function () {
                element.dispatchEvent(getMouseEvent('mouseup', end));
            }, timeout)
        }, start, end, inertia, button);
};