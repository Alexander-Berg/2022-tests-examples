function Api(callbackName, packages, lang, coordorder, mode, ns) {
  var currentVersion2_0 = '2.0.48';
    var api = window.location.href.match(/^https?:\/\/front-jsapi\.crowdtest\.maps\.yandex\.ru\//) ?
        'https://front-jsapi.crowdtest.maps.yandex.ru/2.0' :
        'https://api-maps.tst.c.maps.yandex.ru/2.0';

  var key = 'b027f76e-cc66-f012-4f64-696c7961c395';
  var packages = packages || 'package.full';
  var lang = lang || 'ru-RU';
  var mode =  mode || 'debug';
  var ns = ns || 'ymaps';
  var coordorder = coordorder || 'latlong';
  var onScriptLoad = callbackName === 'none' ? '' : '&onload=' + callbackName;

  window.onload = function () {
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.src = api + '?load=' + packages + '&lang=' + lang + '&coororder=' + coordorder + onScriptLoad + '&apikey=' + key + '&ns=' + ns;
    head.appendChild(script);
  }
}
