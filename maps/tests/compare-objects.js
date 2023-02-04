//  Рекурсивно сравнивает две переменные
function areSame (a, b) {

    if (typeof a != typeof b) {
        return false;
    }
    if (typeof a == 'function') {
        return a === b || a.toSource() == b.toSource();
    }
    if (typeof a != 'object') {
        if (typeof a == 'number') {
            return Math.floor(a * 100) == Math.floor(b * 100);
        }
        return a === b;
    }
    if (a instanceof Array) {
        return areSameArrays(a, b);
    }
    for (var k in a) {
        if (!areSame(a[k], b[k])) {
            //if (a[k] !== b[k]) console.log(a, b)
            return false;
        }
    }
    return true;
}

function areSameArrays (a, b) {
    for (var i = 0, l = a.length; i < l; i++) {
        if (!areSame(a[i], b[i])) {
            //console.log(a[i], b[i])
            return false;
        }
    }
    return true;
}
