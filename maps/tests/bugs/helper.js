function Api(callbackName, coordorder){
    window.onload = function(){
        var version = '2.0';
        var packages = 'package.full';
        var lang = 'ru-RU';
        var server = 'api01e.tst.maps.yandex.ru';
        if(window.location.hash) {
            var parameters = window.location.hash.replace('#', '').split('/');
            version = parameters[0];
            if(typeof parameters[1] !== 'undefined' && parameters[1] !== null && parameters[1] !==''){
                packages = parameters[1];
            }
            if(typeof parameters[2] !== 'undefined' && parameters[2] !== null && parameters[2] !==''){
                lang = parameters[2];
            }
            if(typeof parameters[3] !== 'undefined' && parameters[3] !== null && parameters[3] !==''){
                if(parameters[3] == 'production') server = 'api-maps.yandex.ru';
            }
        }
        var head = document.getElementsByTagName('head')[0];
        var script= document.createElement('script');
        script.type= 'text/javascript';
        script.src= "http://localhost:8080/2.0/" +
                '?ns=ym&load=' + packages + '&mode=dev&lang=' + lang + '&onload=' + callbackName;
//        script.src= 'http://' + server + '/' + version +
//                '/?ns=ym&load=' + packages + '&mode=debug&lang=' + lang + '&onload=' + callbackName;
        if(typeof coordorder !== 'undefined' && coordorder !== null && coordorder !==''){
            script.src += "&coordorder=" + coordorder;
        }
        head.appendChild(script);
    }
}

function ApiSecond(callbackName, coordorder){
    var version = '2.0';
    var packages = 'package.full';
    var lang = 'ru-RU';
    var server = 'api01e.tst.maps.yandex.ru';
    if(window.location.hash) {
        var parameters = window.location.hash.replace('#', '').split('/');
        version = parameters[0];
        if(typeof parameters[1] !== 'undefined' && parameters[1] !== null && parameters[1] !==''){
            packages = parameters[1];
        }
        if(typeof parameters[2] !== 'undefined' && parameters[2] !== null && parameters[2] !==''){
            lang = parameters[2];
        }
        if(typeof parameters[3] !== 'undefined' && parameters[3] !== null && parameters[3] !==''){
            if(parameters[3] == 'production') server = 'api-maps.yandex.ru';
        }
    }
    var head = document.getElementsByTagName('head')[0];
    var script= document.createElement('script');
    script.type= 'text/javascript';
    script.src= 'http://' + server + '/' + version +
            '/?ns=ym&load=' + packages + '&mode=debug&lang=' + lang + '&onload=' + callbackName;
    if(typeof coordorder !== 'undefined' && coordorder !== null && coordorder !==''){
        script.src += "&coordorder=" + coordorder;
    }
    head.appendChild(script);
}


/**
 * логер
 */

function Log(){
    this._logDiv = document.createElement('div');
    this._logDiv.id = 'logger';
    var body = document.getElementsByTagName('body')[0];
    body.appendChild(this._logDiv);
}

Log.prototype = {
    info: function(str){
        this._logDiv.innerHTML += str + "<br>";
    },
    clear: function(){
        this._logDiv.innerHTML = "";
    }
}


function getJSONP(url, success) {
    var ud = '_' + +new Date,
        script = document.createElement('script'),
        head = document.getElementsByTagName('head')[0]
               || document.documentElement;

    window[ud] = function(data) {
        head.removeChild(script);
        success && success(data);
    };

    script.src = url.replace('callback=?', 'callback=' + ud);
    head.appendChild(script);

}

function getCurentLayer(map){
    var l;
    var t = map.getType();
    if(t == "yandex#satellite"){
        l = new ym.yandex.layer.Satellite();
    }
    else if(t == "yandex#map"){
        l = new ym.yandex.layer.Map();
    }
    else if(t == "yandex#publicMap"){
        l = new ym.yandex.layer.PublicMap();
    }
    else if(t == "yandex#publicMapHybrid"){
        l = new ym.yandex.layer.PublicMapSkeleton();
    }
    else if(t == "yandex#hybrid"){
        l = new ym.yandex.layer.Skeleton();
    }
    return l;
}

function getURLParameter(name) {
    return decodeURI(
        (RegExp(name + '=' + '(.+?)(&|$)').exec(location.search)||[,null])[1]
    );
}

function addControls(map, ym){
    map.controls.add(new ym.control.MapTools());
    map.controls.add(new ym.control.MiniMap({
        expanded: false
    }));
    map.controls.add(new ym.control.ScaleLine());
    map.controls.add(new ym.control.ZoomControl());
    map.controls.add(new ym.control.TrafficControl());
    map.controls.add(new ym.control.TypeSelector());
}

/*
    методы для автоматического тестирования 
 */

function click(n){
    YUI().use('node-event-simulate', function(Y) {
        var coordinates = getCoordinates(n);
        var x = coordinates[0], y = coordinates[1];

        var node = Y.one(document.elementFromPoint(x, y));
        
        node.simulate("mouseover",   { clientX: x, clientY: y });
        node.simulate("click",   { clientX: x, clientY: y });
    });
}

function dblclick(n){
    YUI().use('node-event-simulate', function(Y) {
        var coordinates = getCoordinates(n);
        var x = coordinates[0], y = coordinates[1];

        var node = Y.one(document.elementFromPoint(x, y));

        node.simulate("mouseover",   { clientX: x, clientY: y });
        node.simulate("dblclick",   { clientX: x, clientY: y });
    });
}

function drag(from, to){
    YUI().use('node-event-simulate', function(Y) {
        var coordinatesFrom  = getCoordinates(from);
        var xFrom = coordinatesFrom[0], yFrom = coordinatesFrom[1];
        var coordinatesTo  = getCoordinates(to);
        var xTo = coordinatesTo[0], yTo = coordinatesTo[1];

        var node = Y.one(document.elementFromPoint(xFrom, yFrom));

        node.simulate("mousedown", { clientX: xFrom, clientY: yFrom });
        node.simulate("mousemove", { clientX: xTo, clientY: yTo });
        node.simulate("mouseup",   { clientX: xTo, clientY: yTo });
    });
}

function mousemove(to){
    YUI().use('node-event-simulate', function(Y) {
        var coordinatesTo  = getCoordinates(to);
        var xTo = coordinatesTo[0], yTo = coordinatesTo[1];

        var node = Y.one(document.elementFromPoint(xTo, yTo));

        node.simulate("mousemove", { clientX: xTo, clientY: yTo });
        node.simulate("mouseover",   { clientX: xTo, clientY: yTo });
    });
}

function mouseover(n){
    YUI().use('node-event-simulate', function(Y) {
        var coordinates = getCoordinates(n);
        var x = coordinates[0], y = coordinates[1];

        var node = Y.one(document.elementFromPoint(x, y));

        node.simulate("mouseover",   { clientX: x, clientY: y });
    });
}

function mouseout(n){
    YUI().use('node-event-simulate', function(Y) {
        var coordinates = getCoordinates(n);
        var x = coordinates[0], y = coordinates[1];

        var node = Y.one(document.elementFromPoint(x, y));

        node.simulate("mouseout",   { clientX: x, clientY: y });
    });
}

function getCoordinates(n){
    var x, y, k = n % 16;
    if(k == 0){
        x = 16 * 32 - 32 / 2;
        y = (Math.floor(n / 16) - 1) * 32 + 32 / 2;
    }
    else {
        x = k * 32 - 32 / 2;
        y = Math.floor(n / 16) * 32 + 32 / 2;
    }
    return [x, y];
}

// метод добавляет сетку на карту, по которой удобно кликать и сторить тесты
function addGrid(map){
    var paneNode = map.panes.get('events').getElement();
    paneNode.style.backgroundImage = "url('/setka.png')";
}

function zoom(z){
    window.myMap.setZoom(z);
}

function center(c){
    window.myMap.setCenter(c);
}

function debug(){
    myMap.events.add("boundschange", function(e){
        window.location.hash = "zoom/" + myMap.getZoom() + "/center/"
            + myMap.getCenter()[0].toFixed(4) + "," + myMap.getCenter()[1].toFixed(4);
    });
}
