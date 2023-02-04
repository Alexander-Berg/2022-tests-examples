export function arraysAreDeepEqual(arr1, arr2, delta) {
    if (!Array.isArray(arr1) || !Array.isArray(arr2)) {
        return false;
    }

    delta = delta || 1e-10;

    var equal = arr1.length === arr2.length;

    equal && arr1.forEach(function(elem, index) {
        equal = equal && (Array.isArray(elem)? arraysAreDeepEqual(elem, arr2[index]) : Math.abs(elem - arr2[index]) < delta);
    });

    return equal;
}
