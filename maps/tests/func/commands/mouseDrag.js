/**
 * @name browser.mouseDrag
 * @param {[number, number]} start - Start drag point
 * @param {[number, number]} end - End drag point
 * @param {Number} [inertia] - Retention after mousemove and before mouseup, 300ms by default
 * @param {'left'|'middle'|'right'} [button='left'] - Button to be pressed on drag start
 */
module.exports = function (start, end, inertia = 300, button = 'left') {
    return this.execute(
        (start, end, timeout, button) => {
            const BUTTONS_FOR_BUTTON_ID = {left: 0, middle: 1, right: 2};
            const BUTTONS_FOR_DRAG = {left: 1, middle: 4, right: 2};
            const element = window.document.elementFromPoint(start[0], start[1]);

            function createMouseEvent(type, coordinates) {
                return new window.MouseEvent(type, {
                    button: BUTTONS_FOR_BUTTON_ID[button],
                    buttons: BUTTONS_FOR_DRAG[button],
                    bubbles: true,
                    cancelable: type !== 'mousemove',
                    clientX: coordinates[0],
                    clientY: coordinates[1]
                });
            }

            element.dispatchEvent(createMouseEvent('mousedown', start));

            if (button === 'right') {
                element.dispatchEvent(createMouseEvent('contextmenu', start));
            }

            setTimeout(() => {
                element.dispatchEvent(createMouseEvent('mousemove',
                    [start[0] + (start[0] - end[0]) / 2, start[1] + (start[1] - end[1]) / 2]));
            }, 100);

            setTimeout(() => {
                element.dispatchEvent(createMouseEvent('mousemove', end));
            }, 200);

            setTimeout(() => {
                element.dispatchEvent(createMouseEvent('mouseup', end));
            }, timeout);

            return true;
        },
        start, end, inertia, button)
        .catch((err) => {
            throw new Error(err);
        });
};
