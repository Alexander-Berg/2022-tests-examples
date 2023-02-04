(function() {
    if (!('head' in document)) {
        Object.defineProperty(document, 'head', {
            get: function () {
                return document.getElementsByTagName('head')[0];
            }
        })
    }

    if (!('currentScript' in document)) {
        Object.defineProperty(document, 'currentScript', {
            get: function () {
                var scripts = document.head.getElementsByTagName('script');
                return scripts[scripts.length - 1];
            }
        });
    }
})();