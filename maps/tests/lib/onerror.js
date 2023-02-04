window.onerror = function (message, source, lineno, colno, error) {
    console.log('window.onerror', JSON.stringify({
        message: message,
        source: source,
        lineno: lineno,
        colno: colno,
        stack: error && error.stack
    }, null, 4));
};