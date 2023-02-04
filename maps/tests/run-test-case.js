// Подключается в конец файла теста. Вызывает функцию getTestCase которая должна вернуть хеш с тестами (содержимое Y.Test.Case)

// Ссылки на документацию по YUI Test можно найти здесь:
// https://wiki.yandex-team.ru/JandeksKarty/development/fordevelopers/jsapi/testing

var host = getHost();

function runTestCase() {
    YUI({
        // Для подключения не пожатых файлов.
        filter: 'RAW',
        timeout: 10000
    }).use("node", "console", "console-filters", "test", "event-simulate", "dump", function (Y) {
        // Добавляем тесты
        Y.Test.Runner.add(new Y.Test.Case(getTestCase(Y)));

        if (parent && parent.TestManager) {
            // Если страница запущена менеджером тестов, то вызываем .load() у менеджера.
            parent.TestManager.onPageLoad(Y.Test.Runner);
        } else {
            // Если страница запущена отдельно, создаем логгер ...
            new Y.Console({
                height: '450px',
                width: '400px',
                newestOnTop : false,
                plugins: [ Y.Plugin.ConsoleFilters ],
                strings: {
                    collapse: "Свернуть",
                    title: document.title,
                    clear: "Очистить",
                    pause: "Пауза"
                }
            }).render('#testLogger');

            // ... и запускаем тесты.
            Y.Test.Runner.run();
        }
    })
}

function initAPI (params) {
        // Параметры подключения апи по умолчанию.
    var apiParams = {
            load: 'package.full',
            lang: 'ru-RU'
        },
        urlAliases = {
            testing: 'api01e.tst.maps.yandex.ru',
            production: 'api-maps.yandex.ru'
        },
        // Параметры по умолчанию.
        defaultParams = {
            version: '2.0'
        },
        location = window.location,
        name, i, l,
        // url = host + '/build/index.xml';
        url = 'http://localhost:8080/2.0';

    // Расширяем параметрами переданными в адресной строкe.
    if (location.search) {
        var urlParams = location.search.slice(1).split('&');
        for (i = 0, l = urlParams.length; i < l; i++) {
            var param = urlParams[i].split('=');
            params[param[0]] = param[1];
        }
    }

    for (var k in defaultParams) {
        if (!params[k] && defaultParams.hasOwnProperty(k)) {
            params[k] = defaultParams[k];
        }
    }

    if (params.load) {
        apiParams.load = params.load;
    }
    if (params.lang) {
        apiParams.lang = params.lang;
    }
    if (params.mode) {
        apiParams.mode = params.mode;
    }
    if (params.ns) {
        apiParams.ns = params.ns;
    }
    if (params.key) {
        apiParams.key = params.key;
    }
    if (params.coordorder) {
        apiParams.coordorder = params.coordorder;
    }

    if (params.noCache) {
        apiParams.nocache = Math.round(Math.random() * 10e5)
    }

    if (params.url) {
        if (urlAliases[params.url]) {
            if (params.mode == 'dev') {
                apiParams.mode = 'debug';
            }
            url = location.protocol + '//' + urlAliases[params.url] + '/' + params.version + '/';
        } else {
            url = params.url;
        }
    }

    // Формируем строку подключения API.
    var srcParams = [];
    for (name in apiParams) {
        if (apiParams.hasOwnProperty(name)) {
            srcParams.push(name + '=' + apiParams[name]);
        }
    }

    var src = url + '?' + srcParams.join('&');

    document.write('<script type="text/javascript" src="' + src + '"></script>');
}

function getHost () {
    var host = location.protocol + '//' + location.host,
        dirs = location.pathname.split('/'),
        pathDirs = [];
    for (i = 0, l = dirs.length; i < l; i++) {
        var dir = dirs[i];
        if (dir == "test" || dir == "src") {
            break;
        } else {
            pathDirs.push(dir);
        }
    }
    return host + pathDirs.join('/');
}
