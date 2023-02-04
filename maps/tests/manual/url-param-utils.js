/*Добавляем полифил для IE*/
if ('NodeList' in window && !NodeList.prototype.forEach) {
    console.info('polyfill for IE11');
    NodeList.prototype.forEach = function (callback, thisArg) {
      thisArg = thisArg || window;
      for (var i = 0; i < this.length; i++) {
        callback.call(thisArg, this[i], i, this);
      }
    };
  }
  
  if (!Object.keys) {
    Object.keys = (function () {
      var hasOwnProperty = Object.prototype.hasOwnProperty,
          hasDontEnumBug = !({toString: null}).propertyIsEnumerable('toString'),
          dontEnums = [
            'toString',
            'toLocaleString',
            'valueOf',
            'hasOwnProperty',
            'isPrototypeOf',
            'propertyIsEnumerable',
            'constructor'
          ],
          dontEnumsLength = dontEnums.length;
  
      return function (obj) {
        if (typeof obj !== 'object' && typeof obj !== 'function' || obj === null) throw new TypeError('Object.keys called on non-object');
  
        var result = [];
  
        for (var prop in obj) {
          if (hasOwnProperty.call(obj, prop)) result.push(prop);
        }
  
        if (hasDontEnumBug) {
          for (var i=0; i < dontEnumsLength; i++) {
            if (hasOwnProperty.call(obj, dontEnums[i])) result.push(dontEnums[i]);
          }
        }
        return result;
      };
    })();
  }

function getURLParameter(param) {
    let qs = queryStringToObject(location.search);
    return qs[param];
}

function setURLParameter(param, value) {
    var qs = queryStringToObject(location.search);
    qs[param] = value;
    return location.search = objectToQueryString(qs);
}

/**
 * Merges given parameters object with current URL parameters.
 * @param {Record} params
 */
function setURLParameters(params) {
    let qs = queryStringToObject(location.search);
    var paramKeys = Object.keys(params);
    for (var i = 0; i < paramKeys.length; i++) {
        var key = paramKeys[i];
        qs[key] = params[key];
    }
    return location.search = objectToQueryString(qs);
}

/**
 * Removes given list of parameters from current URL parameters.
 * @param {string[]} paramKeys
 */
function clearUrlParameters(paramKeys) {
    let qs = queryStringToObject(location.search);
    for (var i = 0; i < paramKeys.length; i++) {
        delete qs[paramKeys[i]];
    }
    return location.search = objectToQueryString(qs);
}

function queryStringToObject(qst) {
    var qs = qst || '';
    var result = {};
    qs.slice(1).split('&').forEach( function (pair) {
        pair = pair.split('=');
        result[pair[0]] = decodeURIComponent(pair[1] || '');
    });
    if (qs === '') {
        return {};
    }
    return result;
}

function objectToQueryString(obj) {
    var str = '?';
    Object.keys(obj).forEach(function (key) {
        str += key + '=' + obj[key] + '&';
    });
    return str.slice(0, -1);
}

