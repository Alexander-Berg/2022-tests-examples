ymaps.modules.define('test.apiLoader', ['util.extend'], function (provide, extend) {
    var apiCounter = 0;
    provide({
        load: function loadAPI (modules, callback, context) {
            var params = {
                    mode: 'debug',
                    lang: 'ru-RU',
                    apikey: '86f7adc8-b86b-4d85-a48d-31ce3e44f592',
                    ns: 'anotherAPI.api' + (++apiCounter)
                };

            if (typeof modules == 'object') {
                params = extend(params, modules);
            } else {
                params.load = modules;
            }

            if (params.ns && params.ns.length) {
                mocha.globals(params.ns);
            }

            var script = document.createElement('script');
            // IE8
            if (document.all) {
                script.onreadystatechange = function() {
                    if (script.readyState == 'loaded' || script.readyState == 'complete') {
                        script.onreadystatechange = "";
                        onLoad();
                    }
                };
            } else {
                script.onload = onLoad;
            }

            var getParams = [];

            for (var key in params) {
                if (params.hasOwnProperty(key) && !(params[key] === null || typeof params[key] == 'undefined')) {
                    getParams.push(key + '=' + params[key]);
                }
            }

            script.src = "//" + window.location.host + "/init.js?" + getParams.join('&');
            document.documentElement.appendChild(script);

            function onLoad () {
                script.parentElement.removeChild(script);

                var obj;
                if (params.ns && params.ns.length) {
                    var nsParts = params.ns.split('.');
                    obj = window;
                    for (var i = 0, l = nsParts.length; i < l; i++) {
                        obj = obj[nsParts[i]];
                    }
                }

                callback && callback.call(context, obj);
            }
        }
    });
});
