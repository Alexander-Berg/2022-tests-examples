function Api (callbackName, options) {
    window.locationSearch = location.search.substring(1);
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.charset = 'utf-8';

    var packages = options && options.packages || 'package.full';
    var lang = options && options.lang || 'ru-RU';
    var mode = options && options.mode || 'debug';
    var hashcoordorder = options && options.coordOrder || 'latlong';
    var apiPath = getURLParameter('apiPath');
    var key = 'b027f76e-cc66-f012-4f64-696c7961c395';
    var ns = options && options.ns;
    var params = options && options.params;
    var isEnterprise = getURLParameter('enterprise') === 'true';
    script.src =
        (apiPath ?
            ('https://' + (isEnterprise ? 'enterprise.' : '') + apiPath) :
            window.location.href.match(/^.*?(?=\/tests\/hermione\/)/) + '/'
        ) +
        '?lang=' + lang +
        '&mode=' + mode +
        '&load=' + packages +
        (ns ? ('&ns=' + ns) : '') +
        (callbackName ? ('&onload=' + callbackName) : '') +
        '&coordorder=' + hashcoordorder +
        (isEnterprise ? ('&apikey=' + key) : '') +
        (locationSearch ? '&' + locationSearch : '') +
        (params ? '&' + params : '');

    head.appendChild(script);
}
function getURLParameter (name) {
    var locationSearch = window.locationSearch;
    var parameter = decodeURI(
        (RegExp(name + '=(.+?)(&|$)').exec(locationSearch) || [, null])[1]
    );
    if (parameter != 'null') locationSearch = locationSearch.substring(0, locationSearch.indexOf(name)) + locationSearch.substring(locationSearch.indexOf(parameter) + parameter.length + 1, locationSearch.length);
    if (locationSearch == '?') locationSearch = '';
    return parameter == 'null' ? undefined : parameter;
}
var __helperUtilLog;
function __getHelperUtilLog__ () {
    if (!__helperUtilLog) {
        __helperUtilLog = new Log();
    }
    return __helperUtilLog;
}

function __log__ (message) {
    __getHelperUtilLog__().info(message);
    console.log(message);
}

function Log () {
    var currentLogger = document.getElementById('logger');
    if (currentLogger) {
        this._logDiv = currentLogger;
        return ;
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
        this._logDiv.innerHTML += '<br>';
    },
    clear: function () {
        this._logDiv.innerHTML = '';
    }
};
function validateValue(actual, expected, message) {
    if ((actual && expected && actual.toString() == expected.toString())
        || (!actual && !expected && actual == expected)) {
        __log__(message || 'OK');
    } else {
        __log__('FAILED: "' + actual + '" does NOT equal "' + expected + '"');
    }
}
function link (map) {
// Добавим проверку загрузки всех тайлов
    var body = document.getElementsByTagName('body')[0];
    map.layers.events.add('tileloadchange', function(e){
        body.classList.remove('tileload');
        if(e.get('readyTileNumber') == e.get('totalTileNumber')){
            body.classList.add('tileload');
        }
    });
    map.events.add('actionbegin', function () {
        body.classList.remove('tileload');
    });
    map.events.add('actionend', function () {
        var allTiles = true;
        map.layers.each(function(layer){
            layer.each(function(e){
                if (!e.getTileStatus) {
                    return;
                }

                var status = e.getTileStatus();
                if (status.readyTileNumber !== status.totalTileNumber){
                    allTiles = false;
                }
            });
        });
        if (allTiles) body.classList.add('tileload');
    });
    // Иногда нужно узнать координаты на экране, для этого служит ctrl+r
    document.onkeydown=handle;
    function handle(e) {
        if(e.ctrlKey && e.keyCode == 82){
            function onclick(event) {
                var mouse_x = y = 0;
                if (document.attachEvent != null) {
                    mouse_x = window.event.clientX;
                    mouse_y = window.event.clientY;
                } else if (!document.attachEvent && document.addEventListener) {
                    mouse_x = event.clientX;
                    mouse_y = event.clientY;
                }
                status="x = " + mouse_x + ", y = " + mouse_y;
                console.log(mouse_x + ", " + mouse_y);
            }
            document.onclick=onclick;
        }
    }
}

function __ready_for_act__ () {
    __getHelperUtilLog__().info('<span style="color: green;" id="ready_for_act">Выполнено</span>');
}
