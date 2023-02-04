/**
 * Disable output from debug.finishTimer during test run.
 * @param {Controller} controller
 */
function disableDebugOutput(controller) {
    if (typeof controller.req.debug !== 'object') {
        return;
    }

    var _finishTimer = controller.req.debug.finishTimer;

    controller.req.debug.finishTimer = function() {
        var _log = console.log;

        console.log = function() {};
        _finishTimer.apply(this, arguments);
        console.log = _log;
    };

    return controller;
}

module.exports = disableDebugOutput;
