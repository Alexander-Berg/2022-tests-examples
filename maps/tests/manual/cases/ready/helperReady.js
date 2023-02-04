function Api(load, lang, mode, callback) {
    var load = load || '';
    var lang = lang || 'ru-RU';
    var mode = mode || 'debug';
    var src = 'https://api-maps.tst.c.maps.yandex.ru/2.1.79/' + '?load=' + load + '&lang=' + lang + '&mode=' + mode;
    window.onload = function () {
      var head = document.getElementsByTagName('head')[0];
      var script = document.createElement('script');
      script.onload = callback;   
      script.src = src;
      head.appendChild(script);
    }
  }