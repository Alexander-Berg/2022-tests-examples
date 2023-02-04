// Object.getClassName: get the name of an object's type
/*Object.prototype.getClassName = function() {
 var funcNameRegex = /function (.{1,})\(/;
 var results = (funcNameRegex).exec((this).constructor.toString());
 return (results && results.length > 1) ? results[1] : "";
 };*/
//Текущая версия, при нажатии prevVers от неё будет браться предыдущая стабильная версия
var currentVersion2_0 = '2.0.48';

var currentVersion2_1 = '2.1.74';

//Текущая версия тестинга(чтобы захардкодить) версия 2.0 не хардкодится(обсуждаемо)
var currentTestingVersion2_1 = '2.1.74';

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

function Api(callbackName, packages, lang, coordorder, mode, ns, server) {
  window.onload = function () {

    getPrevNext();
    api(callbackName, packages, lang, coordorder, mode, ns, server);
    toolbar();
  }
}

function getInternetExplorerVersion()
// Returns the version of Internet Explorer or a -1 (indicating the use of another browser).
// Full condition for IE8: (getInternetExplorerVersion() == 8 || (document.documentMode == 8))
{
  var rv = -1; // Return value assumes failure.
  if (navigator.appName == 'Microsoft Internet Explorer') {
    var ua = navigator.userAgent;
    var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
    if (re.exec(ua) != null)
      rv = parseFloat(RegExp.$1);
  }
  return rv;
}

function addLinks() {
  window.onload = function () {
    getPrevNext();
    toolbar();
  }
}

function toolbar() {
  var div = document.createElement('div');
  div.style.width = "100%";
  div.style.height = "30px";
  window.document.body.appendChild(div);
  div.innerHTML = '' +
    '<input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;" id="version" value="prevVers" ' +
    'title= "IMPORTANT: update current versions before using this button" />' +
    '<input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;" id="production" value="prod" />' +
    '<input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;" id="enterprise" value="enterprise" />' +
    '<input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;" id="grid" value="grid" />' +
    '<select id="language" style="cursor:pointer;margin-left:5px;font-size:100%;">' +
    '<option value="ru-RU">RU</option>' +
    '<option value="uk-UA">uk-UA</option>' +
    '<option value="ru-UA">ru-UA</option>' +
    '<option value="tr-TR">TR</option>' +
    '<option value="en-US">US</option>' +
    '</select>' +
    '<input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;" id="clearLog" value="Log" />' +
    '<input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;" id="serg" value="serg" />' +
    '<input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;" id="luisa" value="luisa" />';

  if (document.getElementById('version').addEventListener) {
    document.getElementById('version').addEventListener("click", function () {
      changeLink('version')
    });
  } else {
    //ie8
    document.getElementById('version').attachEvent("onclick", function () {
      changeLink('version')
    });
  }

  if (document.getElementById('production').addEventListener) {
    document.getElementById('production').addEventListener("click", function () {
      changeLink('production')
    });
  } else {
    //ie8
    document.getElementById('production').attachEvent("onclick", function () {
      changeLink('production')
    });
  }

  if (document.getElementById('enterprise').addEventListener) {
    document.getElementById('enterprise').addEventListener("click", function () {
      changeLink('enterprise')
    });
  } else {
    //ie8
    document.getElementById('enterprise').attachEvent("onclick", function () {
      changeLink('enterprise')
    });
  }

  if (document.getElementById('grid').addEventListener) {
    document.getElementById('grid').addEventListener("click", function () {
      removeGrid();
    });
  } else {
    //ie8
    document.getElementById('grid').attachEvent("onclick", function () {
      removeGrid();
    });
  }
  if (document.getElementById('serg').addEventListener) {
    document.getElementById('serg').addEventListener("click", function () {
      changePathForSergeiiak()
    });
  } else {
    //ie8
    document.getElementById('serg').attachEvent("onclick", function () {
      changePathForSergeiiak()
    });
  }

  if (document.getElementById('luisa').addEventListener) {
    document.getElementById('luisa').addEventListener("click", function () {
      changePathForLuisa()
    });
  } else {
    //ie8
    document.getElementById('luisa').attachEvent("onclick", function () {
      changePathForLuisa()
    });
  }

  if (document.getElementById('clearLog').addEventListener) {
    document.getElementById('clearLog').addEventListener("click", function () {
      document.getElementById('logger').innerHTML = "";
    });
  } else {
    //ie8
    document.getElementById('clearLog').attachEvent("onclick", function () {
      document.getElementById('logger').innerHTML = "";
    });
  }

  if (document.getElementById('language').addEventListener) {
    document.getElementById('language').addEventListener("change", function () {
      switchLang(this.value)
    });
  } else {
    //ie8
    document.getElementById('language').attachEvent("change", function () {
      switchLang(this.value)
    });
  }

  function changeLink(link) {
    if (link == 'version') {
      var path = window.location.pathname;
      var hash = window.location.hash;
      var hashRemainder = "";
      var previousVersion;
      if (hash != '') {
        if (hash.indexOf('2.') != -1) {
          previousVersion = hash.slice(1, 7);
          hashRemainder = hash.slice(7);
        } else {
          if (path.indexOf('2.0') != -1) {
            previousVersion = currentVersion2_0;
          } else {
            previousVersion = currentVersion2_1;
          }
          hashRemainder = hash.slice(1);
        }
      } else {
        if (path.indexOf('2.0') != -1) {
          previousVersion = currentVersion2_0;
        } else {
          previousVersion = currentVersion2_1;
        }
      }

      var currentVersion = previousVersion.split('.');
      previousVersion = previousVersion.slice(0, 4) + (+currentVersion[2] - 1);
      window.location.hash = '#' + previousVersion + hashRemainder;
      location.reload();
    } else {
      if (window.location.hash.indexOf('2.') != -1) {
        window.location.hash = window.location.hash.slice(0, 7) + '///' + link;
      } else {
        window.location.hash = '#///' + link;
      }
      location.reload();
    }
  }

  function switchLang(lang) {
    window.location.hash = '#//' + lang;
    location.reload();
  }

  function changePathForSergeiiak() {
    var x = window.location.toString().substr(26);
    window.location = "http://jsapi.sergeiiak.alexa.maps.dev.yandex.net/api" + x;
  }

  function changePathForLuisa() {
    var x = window.location.toString().replace(/[\/:a-z1-9]*(?:jsapi-tests)/, 'api');
    window.location = "http://jsapi.luisa-s.alexa.maps.dev.yandex.net/" + x;
  }
}

// add alert to page
function setAlertPage(message) {
  if (!document.getElementById('alertPageElement')) {
    var alertPageElement = document.createElement('div');
    alertPageElement.id = 'alertPageElement';
    var parentElement = document.getElementById('navigationElement');

// Вставляем новый элемент перед первым дочерним элементом
    document.body.insertBefore(alertPageElement, parentElement);
    alertPageElement.innerHTML = '' +
      '<h3 style="color: red;">' + (message || "Обрати внимание. На странице ошибка") + '</h3>';
  }
}
// add links to previous and next cases and home link
function getPrevNext() {
  // create <div> with <input> elements
  // these elements will contain URLs to previous and next pages
  var navigationElement = document.createElement('div');
  window.document.body.appendChild(navigationElement);
  navigationElement.id = 'navigationElement';

  //buttons
  navigationElement.innerHTML = '' +
    '<input type="button" style="cursor:pointer;margin:5px;font-size:100%;" id="prev" value="prev" />' +
    '<input type="button" style="cursor:pointer;margin:5px;font-size:100%;" id="next" value="next" />' +
    '<input type="button" style="cursor:pointer;margin:5px;font-size:100%;" id="clear" value="hash" />' +
    '<input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;" id="home" value="home" />' +
    "<input type='button' style='cursor:pointer;margin-left:5px;font-size:100%;' id='https' value='ps' onclick='window.open(\"https://api-maps.tst.c.maps.yandex.ru\")'/>" +
    "<input type='button' style='cursor:pointer;margin-left:5px;font-size:100%;' id='tile' value='le' onclick='window.open(\"https://core-renderer-tilesgen.datatesting.maps.yandex.net\")'/>" +
    "<input type='button' style='cursor:pointer;margin-left:5px;font-size:100%;' id='traffic' value='tr' onclick='window.open(\"https://core-jams-rdr.testing.maps.n.yandex.ru/1.1/tiles?\")'/>" +
    "<input type='button' style='cursor:pointer;margin-left:5px;font-size:100%;' id='etile' value='ele' onclick='window.open(\"https://enterprise.vec-rdr01h.dtst.maps.yandex.ru\")'/>" +
    "<input type='button' style='cursor:pointer;margin-left:5px;font-size:100%;' id='ent' value='ent' onclick='window.open(\"https://enterprise.api-maps.tst.c.maps.yandex.ru\")'/>";

  if (document.getElementById('clear').addEventListener) {
    document.getElementById('clear').addEventListener("click", clearHash);
  } else {
    //ie8
    document.getElementById('clear').attachEvent("onclick", clearHash);
  }

  if (document.getElementById('home').addEventListener) {
    document.getElementById('home').addEventListener("click", goHome);
  } else {
    //ie8
    document.getElementById('home').attachEvent("onclick", goHome);
  }

  function clearHash() {
    window.location.hash = '';
    var newLocation = window.location.href.substring(0, window.location.href.indexOf('?')).replace('#', '');
    window.location.replace(newLocation);
  }

  function goHome() {

    var folder = "(^_^)",
      url = document.location.href,
      path = url.split(document.location.host)[1];
    var folders = getHelperPath().split("/");
    folders.pop();
    folder = folders.pop();

    var pos = path.indexOf(folder + "/");
    if (pos > -1) { // в папке
      path = path.slice(0, pos) + folder;
    }

    window.location = path;

  }

  // connect to server side (prevNext.js)

  var xhr = new XMLHttpRequest();
  xhr.open('GET', 'https://alexa.maps.dev.yandex.ru:4771', true);
  xhr.send('');

  // when response received, process responseText

  xhr.onreadystatechange = function () {
    try {
      if (xhr.readyState != 4) return;

      var prevNextJson = this.responseText;
      // create js-object from json
      var prevNext = JSON.parse(prevNextJson);

      // add listeners - buttons
      if (document.getElementById('prev').addEventListener) {
        document.getElementById('prev').addEventListener("click", goNext(prevNext.prev));
      } else {
        //ie8
        document.getElementById('prev').attachEvent("onclick", prev);
      }

      if (document.getElementById('next').addEventListener) {
        document.getElementById('next').addEventListener("click", goNext(prevNext.next));
      } else {
        //ie8
        document.getElementById('next').attachEvent("onclick", next);
      }

      // handlers
      function prev() {
        window.location = prevNext.prev
      }

      function next() {
        window.location = prevNext.next
      }

    } catch (err) {

    }
  };

  var goNext = function (nextLocation) {
    return function () {
      window.location = nextLocation;
    }
  };

}

// add links to previous and next cases
function getPrevNextIE8() {
  // create <div> with two <a> elements
  // these elements will contain URLs to previous and next pages
  var navigationElement = document.createElement('div');
  window.document.body.appendChild(navigationElement);

  navigationElement.innerHTML = '<a id="prev" href="">prev</a>   <a id="next" href="">next</a>';

  // connect to server side (prevNext.js)
  var xhr = new XMLHttpRequest();
  xhr.open('GET', window.location.origin + ':7777', true);
  xhr.send('');


  // when response received, process responseText
  xhr.onreadystatechange = function () {
    try {
      if (xhr.readyState != 4) return;

      var prevNextJson = this.responseText;
      // create js-object from json
      var prevNext = JSON.parse(prevNextJson);

      // change href attribute - links
      document.getElementById('prev').href = prevNext.prev;
      document.getElementById('next').href = prevNext.next;

    } catch (err) {

    }
  }
}

function getObjectClassName(obj) {
  if (obj.__proto__ && obj.__proto__.constructor) {
    return obj.__proto__.constructor.name
  } else {
    return obj.constructor.name
  }
}

function validateClass(obj, type) {
  if (obj instanceof type) {
    __logPass__('OK: object is an instance of class');
  } else {
    __logFail__('FAILED: object is NOT an instance of class');
  }
}

function validateValue(actual, expected, message) {
  if ((actual && expected && actual.toString() == expected.toString())
    || (!actual && !expected && actual == expected)) {
    __logPass__(message || 'OK');
  } else {
    __logFail__('FAILED: "' + actual + '" does NOT equal "' + expected + '"');
  }
}

function getHelperPath() {
  var scripts = document.getElementsByTagName('script'),
    i = 0;

  while (scripts[i]) {
    if (scripts[i].src.indexOf('helper.js') > -1) {
      return scripts[i].src;
    }
    i++;
  }

  return null;
}

function getUrlVersion() {
  var url = window.location.pathname;
  var startIndex = url.indexOf('api/') + 4;
  var endIndex = url.indexOf('/', startIndex);
  return url.slice(startIndex, endIndex);
}

function getLatestVersion() {
  var urlVersion = getUrlVersion();
  if (urlVersion == 'enterprise') {
    var url = window.location.pathname;
    var startIndex = url.indexOf('enterprise/') + 11;
    var endIndex = url.indexOf('/', startIndex);
    var urlVersion = url.slice(startIndex, endIndex);
    return (urlVersion == '2.1') ? (urlVersion + '-dev') : urlVersion
  }
  return (urlVersion == '2.1') ? (urlVersion + '-dev') : urlVersion
}

function getServer() {
  var url = window.location.pathname;
  var startIndex = url.indexOf('api/') + 4;
  var endIndex = url.indexOf('/', startIndex);
  var urlVersion = url.slice(startIndex, endIndex);
  return (urlVersion == 'enterprise') ? ('enterprise.api-maps.tst.c.maps.yandex.ru') : ('api-maps.tst.c.maps.yandex.ru')
}

function api(callbackName, packages, lang, coordOrder, mode, ns, server, counters) {
  window.locationSearch = location.search.substring(1);
  var head = document.getElementsByTagName('head')[0],
    script = document.createElement('script');
  script.type = 'text/javascript';
  script.charset = 'utf-8';

  // На время тестирования фиксируем версию тут, как показала практика если не фиксировать версию то потом очень обидно
  var version = getLatestVersion().substring(0, 3) == "2.1" ? currentTestingVersion2_1 : getLatestVersion();
  var packages = packages || 'package.full';
  var lang = lang || 'ru-RU';
  var mode = mode || 'debug';
  var hashcoordorder = coordOrder || 'latlong';
  var server = server || getServer();
  var callbackName = callbackName || 'init';
  var autotestsHostConfig = '';
  var key = 'b027f76e-cc66-f012-4f64-696c7961c395';
  if (location.search) {
    if(location.search.indexOf("=") == -1){
      script.src = 'https://' + location.search.substring(1) + '?lang=ru-RU&mode=' + (location.hash.substring(1) || 'debug') + '&load=package.full&ns=ym&onload=' + callbackName;
    } else{
      script.src = 'https://' + (getURLParameter("server") || server) + '/' + (getURLParameter("version") || version) +
        '/?lang=' + (getURLParameter("lang") || lang) + '&mode=' + (getURLParameter("mode") || mode) + '&load=' +
        (getURLParameter("load") || packages) + '&ns=ym&onload=' + callbackName + '&coordorder=' + (getURLParameter("coordorder") || hashcoordorder) + "&" + (locationSearch ? locationSearch : "");
    }
    if (server == 'enterprise.api-maps.tst.c.maps.yandex.ru' || server == 'enterprise.api-maps.yandex.ru') {
      script.src += '&apikey=' + key;
    }
  } else {
    var callbackName = callbackName || 'init';
    var ns = ns || 'ym';
    var hostConfig,
      //TODO: перейти на querystring
      hostConfigRoute,
      counters = counters;
    if (window.location.hash) {
      var parameters = window.location.hash.replace('#', '').split('/');
      if (typeof parameters[0] !== 'undefined' && parameters[0] !== null && parameters[0] !== '') {
        version = parameters[0];
      }
      if (typeof parameters[1] !== 'undefined' && parameters[1] !== null && parameters[1] !== '') {
        packages = parameters[1];
      }
      if (typeof parameters[2] !== 'undefined' && parameters[2] !== null && parameters[2] !== '') {
        lang = parameters[2];
      }
      if (typeof parameters[3] !== 'undefined' && parameters[3] !== null && parameters[3] !== '') {

        if (parameters[3] == 'production') {
          server = 'api-maps.yandex.ru';
        } else if (parameters[3] == 'enterprise') {
          server = 'enterprise.api-maps.tst.c.maps.yandex.ru';
        } else if (parameters[3] == 'prodEnt') {
          key = '86f7adc8-b86b-4d85-a48d-31ce3e44f592';
          server = 'enterprise.api-maps.yandex.ru';
        } else if (parameters[3] == 'poi') {
          server = 'content.test.api-maps.yandex.ru';
        } else if (parameters[3] == 'prest') {
          server = 'api-index05h.cloud.maps.yandex.net';
        } else {
          server = parameters[3];
          version = 'init.js';
        }
      }
      if (typeof parameters[4] !== 'undefined' && parameters[4] !== null && parameters[4] !== '') {
        hashcoordorder = 'longlat';
      }
      if (typeof parameters[5] !== 'undefined' && parameters[5] !== null && parameters[5] !== '') {
        mode = parameters[5];
      }
      //для инсепшна
      if (typeof parameters[6] !== 'undefined' && parameters[6] !== null && parameters[6] !== '') {
        hostConfig = parameters[6];
      }
      //для подключения счёттчиков
      if (typeof parameters[7] !== 'undefined' && parameters[7] !== null && parameters[7] !== '') {
        counters = parameters[7];
      }
      //для роутера
      if (typeof parameters[8] !== 'undefined' && parameters[8] !== null && parameters[8] !== '') {
        hostConfigRoute = parameters[8];
      }
    }

    script.src = 'https://' + server + '/' + version +
      '?ns=' + ns + '&load=' + packages + ',domEvent.overrideStorage&mode=' + mode + '&lang=' + lang + '&onload=' + callbackName + "&coordorder=" + hashcoordorder;

    if(hostConfig) script.src += '&host_config[hosts][apiInceptionService]=http://' + hostConfig;
    if(hostConfigRoute) script.src += '&host_config[hosts][apiRouteService]=https://api-maps.yandex.ru/services/route/';
    if(counters) script.src += '&counters=all';

    //Принудительно включаю маршрутиатор и коверейдж из продакшна для проверки регрессии и багов, выключить после 2.1.43

    //script.src += '&host_config[hosts][apiSearchService]=https://api-maps.yandex.ru/services/search/';
    //script.src += '&host_config[hosts][apiCoverageService]=https://api-maps.yandex.ru/services/coverage/';

    //script.src += '&host_config[hosts][maps_RU]=https://yandex.ru/maps/';
    //script.src += '&host_config[hosts][maps_UA]=https://yandex.ua/maps/';
    //script.src += '&host_config[hosts][maps_US]=https://yandex.com/maps/';
    //script.src += '&host_config[hosts][maps_TR]=https://yandex.com.tr/harita/';
    //script.src += '&host_config[inthosts][metaApi]=http://meta.maps.yandex.net/';
    //script.src += '&host_config[hosts][apiRouteService]=https://api-maps.yandex.ru/services/route/';
    //script.src += '&host_config[hosts][apiInceptionService]=http://node.mstepanova.teide.maps.dev.yandex.ru:8078';
    /**script.src += '&host_config[hosts][apiRouteService]=https://router-service.tst.qloud.maps.yandex.net/';

     script.src += '&host_config[hosts][apiSearchService]=https://api-maps.yandex.ru/services/search/';

     script.src += '&host_config[hosts][apiPanoramaService]=https://panoramas.api-maps.yandex.ru/panorama/';


     script.src += '&host_config[inthosts][metaApi]=http://meta.maps.yandex.net/';
     script.src += '&host_config[hosts][traffic]=https://jgo.maps.yandex.net/';
     script.src += '&host_config[hosts][maps_UA]=https://l7test.yandex.ua/maps-prod/';
     script.src += '&host_config[hosts][maps_US]=https://l7test.yandex.com/maps-prod/';
     script.src += '&host_config[hosts][maps_TR]=https://l7test.yandex.com.tr/harita-prod/';*/
    //Принудительно включаю маршрутиатор и коверейдж из продакшна для проверки регрессии и багов, выключить после 2.1.43


    if (window.location.hash) {
      var parameters = window.location.hash.replace('#', '').split('='), i = 2;
      if (parameters[0] == 'url') {
        script.src = parameters[1];
        while (i < parameters.length) {
          script.src += '=' + parameters[i++];
        }
        script.src += '&onload=' + callbackName;
      }
    }

    if (server == 'enterprise.api-maps.tst.c.maps.yandex.ru' || server == 'enterprise.api-maps.yandex.ru') {
      script.src += '&apikey=' + key;
    }
  }

  head.appendChild(script);
}

/**
 * логер
 */

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


function getJSONP(url, success) {
  var ud = '_' + +new Date,
    script = document.createElement('script'),
    head = document.getElementsByTagName('head')[0]
      || document.documentElement;

  window[ud] = function (data) {
    head.removeChild(script);
    success && success(data);
  };

  script.src = url.replace('callback=?', 'callback=' + ud);
  head.appendChild(script);

}

function getCurrentLayer(map) {
  var l;
  var t = map.getType();
  if (t == "yandex#satellite") {
    l = new ym.yandex.layer.Satellite();
  }
  else if (t == "yandex#map") {
    l = new ym.yandex.layer.Map();
  }
  else if (t == "yandex#publicMap") {
    l = new ym.yandex.layer.PublicMap();
  }
  else if (t == "yandex#publicMapHybrid") {
    l = new ym.yandex.layer.PublicMapSkeleton();
  }
  else if (t == "yandex#hybrid") {
    l = new ym.yandex.layer.Skeleton();
  }
  return l;
}

function getURLParameter(name) {
  var parameter = decodeURI(
    (RegExp(name + '=' + '(.+?)(&|$)').exec(locationSearch) || [, null])[1]
  );
  if(parameter != "null") locationSearch = locationSearch.substring(0, locationSearch.indexOf(name)) + locationSearch.substring(locationSearch.indexOf(parameter) + parameter.length + 1, locationSearch.length);
  if(locationSearch == "?") locationSearch = "";
  return parameter == "null" ? undefined : parameter;
}

function addControls(map, ym) {
  map.controls.add(new ym.control.MapTools());
  map.controls.add(new ym.control.MiniMap({
    expanded: false
  }));
  map.controls.add(new ym.control.ScaleLine());
  map.controls.add(new ym.control.ZoomControl());
  map.controls.add(new ym.control.TrafficControl());
  map.controls.add(new ym.control.TypeSelector());
}

// метод, удаляющий копирайты с карты
function removeCopyrights() {
  var copyrights = document.getElementsByClassName("ymaps-copyright")[0];
  copyrights.parentNode.removeChild(copyrights);
}

// метод, меняющий стиль элемента
function changeStyle() {
  var closeButton = document.getElementsByClassName("ymaps-balloon__close-button")[0];
  if (closeButton) {
    closeButton.setAttribute('style', '');
  }
}

/*
 методы для автоматического тестирования
 */

function click(n) {
  YUI().use('node-event-simulate', function (Y) {
    var coordinates = getCoordinates(n);
    var x = coordinates[0], y = coordinates[1];

    var node = Y.one(document.elementFromPoint(x, y));

    node.simulate("mouseover", { clientX: x, clientY: y });
    node.simulate("click", { clientX: x, clientY: y });
  });
}

function dblclick(n) {
  YUI().use('node-event-simulate', function (Y) {
    var coordinates = getCoordinates(n);
    var x = coordinates[0], y = coordinates[1];

    var node = Y.one(document.elementFromPoint(x, y));

    node.simulate("mouseover", { clientX: x, clientY: y });
    node.simulate("dblclick", { clientX: x, clientY: y });
  });
}

function contextmenu(n) {
  YUI().use('node-event-simulate', function (Y) {
    var coordinates = getCoordinates(n);
    var x = coordinates[0], y = coordinates[1];

    var node = Y.one(document.elementFromPoint(x, y));

    node.simulate("mouseover", { clientX: x, clientY: y });
    node.simulate("contextmenu", { clientX: x, clientY: y });
  });
}

function drag(from, to) {
  YUI().use('node-event-simulate', function (Y) {
    var coordinatesFrom = getCoordinates(from);
    var xFrom = coordinatesFrom[0], yFrom = coordinatesFrom[1];
    var coordinatesTo = getCoordinates(to);
    var xTo = coordinatesTo[0], yTo = coordinatesTo[1];

    var node = Y.one(document.elementFromPoint(xFrom, yFrom));

    node.simulate("mousedown", { clientX: xFrom, clientY: yFrom });
    node.simulate("mousemove", { clientX: xTo, clientY: yTo });
    node.simulate("mouseup", { clientX: xTo, clientY: yTo });
  });
}

function mousemove(to) {
  YUI().use('node-event-simulate', function (Y) {
    var coordinatesTo = getCoordinates(to);
    var xTo = coordinatesTo[0], yTo = coordinatesTo[1];

    var node = Y.one(document.elementFromPoint(xTo, yTo));

    node.simulate("mousemove", { clientX: xTo, clientY: yTo });
    node.simulate("mouseover", { clientX: xTo, clientY: yTo });
  });
}

function mouseover(n) {
  YUI().use('node-event-simulate', function (Y) {
    var coordinates = getCoordinates(n);
    var x = coordinates[0], y = coordinates[1];

    var node = Y.one(document.elementFromPoint(x, y));

    node.simulate("mouseover", { clientX: x, clientY: y });
  });
}

function mouseout(n) {
  YUI().use('node-event-simulate', function (Y) {
    var coordinates = getCoordinates(n);
    var x = coordinates[0], y = coordinates[1];

    var node = Y.one(document.elementFromPoint(x, y));

    node.simulate("mouseout", { clientX: x, clientY: y });
  });
}

function getCoordinates(n) {
  var x, y, k = n % 16;
  if (k == 0) {
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
function addGrid(map) {
  var paneNode = map.panes.get('events').getElement();
  url = getHelperPath().replace('helper.js', 'setka.png');
  //Надоела мне сетка и я решил её убрать, но можно её включить кнопкой под картой
  //paneNode.style.backgroundImage = "url('" + url + "')";
  globalGrid = paneNode;
  //removeCopyrights(); // temporary hack for autotesting
  window.locationSearch = location.search.substring(1);
  var mapType = getURLParameter("type");
  if(mapType != undefined){
    if(mapType == 'false'){
      map.setType(null);
    } else {
      map.setType('yandex#' + mapType);
    }
  }
  // Добавим проверку загрузки всех тайлов
  var body = document.getElementsByTagName('body')[0];
  map.layers.events.add('tileloadchange', function(e){
    body.classList.remove('tilesloaded');
    if(e.get('readyTileNumber') == e.get('totalTileNumber')){
      body.classList.add('tileload');
    }
  });
}

function removeGrid() {
  globalGrid.style.backgroundImage = "url('" + url + "')" ;
  //removeCopyrights(); // temporary hack for autotesting

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

function zoom(z) {
  window.myMap.setZoom(z);
}

function center(c) {
  window.myMap.setCenter(c);
}

function debug() {
  myMap.events.add("boundschange", function (e) {
    window.location.hash = "zoom/" + myMap.getZoom() + "/center/"
      + myMap.getCenter()[0].toFixed(4) + "," + myMap.getCenter()[1].toFixed(4);
  });
}
/**
 *   _0_
 *  3   1
 *  |_2_|
 *
 *  4 - точка в середине
 */
function Editor() {

  var cell = this._cell = [],
    pane = document.getElementsByClassName("ymaps-2-1-13-events-pane")[0],
    that = this,
    bounds = pane.getBoundingClientRect();

  this._cellSize = 32;
  this._offsetLeft = bounds.left;
  this._offsetTop = bounds.top;
  this._paneWidth = pane.clientWidth;
  this._paneHeight = pane.clientHeight;
  this._currentElement = pane;

  for (var i = 0; i < 5; i++) {
    this._cell.push(document.createElement("div"));
  }
  cell[0].style.width = cell[2].style.width = this._cellSize + "px";
  cell[0].style.height = cell[2].style.height = "1px";
  cell[1].style.width = cell[3].style.width = "1px";
  cell[1].style.height = cell[3].style.height = this._cellSize + "px";
  cell[4].style.height = cell[4].style.width = 2 + "px";

  this.each(function (cell) {
    cell.style.position = "absolute";
    cell.style.display = "none";
    cell.style.zIndex = 10000;
    cell.style.backgroundColor = "#000000";
    document.body.appendChild(cell);
  });

  document.body.onmousemove = function (e) {
    that.onmousemove(e);
  };
  document.body.onclick = function (e) {
    that.onclick(e);
  };

  window.onkeydown = function (e) {
    that.onkeydown(e);
  };

  this.createEditor();
}

Editor.prototype = {
  onmousemove: function (e) {
    var x = e.clientX,
      y = e.clientY,
      currentElement = document.elementFromPoint(x, y),
      inx = x > this._offsetLeft && x < (this._offsetLeft + this._paneWidth),
      iny = y > this._offsetTop && y < (this._offsetTop + this._paneHeight);

    if (currentElement != this._currentElement) {
      this._currentElement = currentElement;
    }

    if (inx && iny) {
      this.showCell(e);
    }
    else {
      this.hideCell();
    }
  },
  showCell: function (e) {
    var x = e.clientX - this._offsetLeft,
      y = e.clientY - this._offsetTop,
      left = this._offsetLeft + Math.ceil(x / this._cellSize - 1) * this._cellSize,
      top = this._offsetTop + Math.ceil(y / this._cellSize - 1) * this._cellSize;

    this.setPosition(left, top);
    this.each(function (e) {
      e.style.display = "";
    });
  },
  hideCell: function () {
    this.each(function (e) {
      e.style.display = "none";
    });
  },
  each: function (f, context) {
    for (var i = 0; i < this._cell.length; i++) {
      if (context) {
        f.call(context, this._cell[i]);
      } else {
        f(this._cell[i]);
      }
    }
  },
  setPosition: function (x, y) {
    this._cell[0].style.left = this._cell[3].style.left = this._cell[2].style.left = x + "px";
    this._cell[0].style.top = this._cell[3].style.top = this._cell[1].style.top = y + "px";
    this._cell[2].style.top = (y + this._cellSize) + "px";
    this._cell[1].style.left = (x + this._cellSize) + "px";
    this._cell[4].style.top = (y + this._cellSize / 2 - 1) + "px";
    this._cell[4].style.left = (x + this._cellSize / 2 - 1) + "px";
  },
  onclick: function (e) {
    var x = e.clientX - this._offsetLeft,
      y = e.clientY - this._offsetTop,
      left = Math.ceil(x / this._cellSize),
      top = Math.ceil(y / this._cellSize - 1);

    this._editor.innerHTML = this._editor.innerHTML.replace("_", top * 16 + left);
  },
  onkeydown: function (e) {
    var event = e || window.event,
      code = event.which || event.keyCode,
      key = String.fromCharCode(code).toUpperCase(),
      editor = this._editor,
      regExp = new RegExp("_");

    if (key === "E") {
      editor.innerHTML = "";
    }

    if (regExp.test(editor.innerHTML)) {
      return;
    }

    switch (key) {
      case "C" :
        editor.innerHTML += "click(_);<br>";
        break;
      case "M" :
        editor.innerHTML += "move(_);<br>";
        break;
      case "D" :
        editor.innerHTML += "drag(_, _);<br>";
        break;
      case "R" :
        editor.innerHTML += "rightclick(_);<br>";
        break;
      case "W" :
        editor.innerHTML += "dblclick(_);<br>";
        break;
      case "T" :
        editor.innerHTML += "test();<br>";
        break;
      case "S" :
        editor.innerHTML += "sleep(1000);<br>";
        break;
    }
  },
  createEditor: function () {
    var editor = this._editor = document.createElement("div");
    editor.style.position = "fixed";
    editor.style.width = "400px";
    editor.style.height = "600px";
    editor.style.right = "10px";
    editor.style.top = "10px";
    editor.style.border = "1px solid black";
    document.body.appendChild(editor);
  }
}