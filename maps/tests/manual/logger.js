var __helperUtilLog;

function __getHelperUtilLog__() {
    if (!__helperUtilLog) {
        __helperUtilLog = new Log();
    }
    return __helperUtilLog;
}

function __log__(message) {
    __getHelperUtilLog__().info(message);
    console.log(message);
}

function __logFail__(message) {
    __getHelperUtilLog__().info('<span style="color: red;" class="log_fail">' + message + '</span>');
    console.warn(message);
    setAlertPage();
}

function __logPass__(message) {
    __getHelperUtilLog__().info('<span style="color: green;">' + message + '</span>');
    console.info(message);
}

function __ready_for_act__() {
    __getHelperUtilLog__().info('<span style="color: green;" id="ready_for_act">Выполнено</span>');
}

function Log() {
    var currentLogger = document.getElementById('logger');
    if (currentLogger) {
        this._logDiv = currentLogger;
        return
    }

    this._logDiv = document.createElement('div');
    this._logDiv.id = 'logger';
    var body = document.getElementsByTagName('body')[0];
    body.appendChild(this._logDiv);

}

Log.prototype = {
    info: function (str) {
        for (var i = 0; i < arguments.length; i++) {
            this._logDiv.innerHTML += arguments[i];
        }
        this._logDiv.innerHTML += "<br>";
    },
    clear: function () {
        this._logDiv.innerHTML = "";
    }
};

function validateClass(obj, type) {
    if (obj instanceof type) {
        __logPass__('OK: object is an instance of class');
    } else {
        __logFail__('FAILED: object is NOT an instance of class');
    }
}

function validateValue(actual, expected, message) {
    if ((actual && expected && actual.toString() == expected.toString()) ||
        (!actual && !expected && actual == expected)) {
        __logPass__(message || 'OK');
    } else {
        __logFail__('FAILED: "' + actual + '" does NOT equal "' + expected + '"');
    }
}