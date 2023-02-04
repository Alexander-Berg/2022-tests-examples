/**
 * Поинтер клик по координатам или элементу
 *
 * @name browser.pointerClick
 * @param {Array} args - селектор или координаты или селектор и сдвиг относительно угла селектора
 */
module.exports = function (...args) {
    let selector,
        x = 0,
        y = 0,
        addX = 0,
        addY = 0;
    if (args.length === 1) {
        selector = args[0];
    } else if (args.length === 2) {
        selector = 'body';
        x = args[0];
        y = args[1];
    } else {
        selector = args[0];
        addX = args[1];
        addY = args[2];
        x = args[1];
        y = args[2];
    }
    if (this.desiredCapabilities.browserName === 'firefox') {
        this.leftClickActions = function () {
            return this
                .actions([{
                    type: 'pointer',
                    id: 'mouse1',
                    parameters: {pointerType: 'mouse'},
                    actions: [
                        {type: 'pointerMove', duration: 0, x: x + addX, y: y + addY},
                        {type: 'pointerDown', button: 0},
                        {type: 'pointerUp', button: 0}
                    ]
                }])
        };

        if (args.length === 2) {
            return this.leftClickActions();
        } else {
            return this
                .getLocation(selector)
                .then((e) => {
                    x = Math.round(e.length ? e[e.length - 1].x : e.x);
                    y = Math.round(e.length ? e[e.length - 1].y : e.y);
                    return this.leftClickActions();
                });
        }
    }

    return this
        .moveToObject(selector, x, y)
        .buttonDown()
        .pause(10)
        .buttonUp();
};