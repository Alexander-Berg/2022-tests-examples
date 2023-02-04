function loadTestCaseTree(successCallback, errorCallback) {
    get(
        location.href.split('manual')[0] + 'get-manual-cases',
        function (data) {
            successCallback(JSON.parse(data));
        },
        errorCallback
    );

    function get(url, successCallback, errorCallback) {
        var xhr = new XMLHttpRequest();
        xhr.open('get', url, true);
        xhr.onload = function () {
            if (xhr.status === 200) {
                successCallback(xhr.responseText);
            } else {
                errorCallback(new Error('Bad status code: ' + xhr.status));
            }
        };
        xhr.onerror = errorCallback;
        xhr.send();
    }
}

/**
 * Silently tries to loads available jsapi versions lists from production and testing servers.
 * Passes loaded data (or [], if loading failed) to @param successCallback.
 */
function getAllAvailableApiVersions(successCallback) {
    successCallback({
        production: ['2.1.oldie', '2.1'],
        testing: ['2.1.oldie', '2.1']
    });
}

function substitute(template, params) {
    var result = template;
    var paramKeys = Object.keys(params);
    for (var i = 0; i < paramKeys.length; i++) {
        var key = paramKeys[i];
        result = result.replace('${' + key + '}', params[key]);
    }
    return result;
}

function getObjectClassName(obj) {
    if (obj.__proto__ && obj.__proto__.constructor) {
        return obj.__proto__.constructor.name
    } else {
        return obj.constructor.name
    }
}
