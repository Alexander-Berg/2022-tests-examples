function toolbar() {
    var div = document.createElement('div');
    var singleControlWidth = detectMob() ? '120px' : 'auto';
    div.style.width = '100%';
    div.style.height = detectMob() ? '80px' : 'auto';
    window.document.body.appendChild(div);
    var langList = ['ru-RU', 'uk-UA', 'ru-UA', 'tr-TR', 'en-US'];
    var sizeStyle = 'width:' + singleControlWidth + ';height:25px;';
    var buttonStyleAndType = 'type="button" style="cursor:pointer;margin-left:5px;font-size:100%;' + sizeStyle;

    div.innerHTML = '' +
        buildVersionSelect(sizeStyle) +
        '<input ' + buttonStyleAndType + '" id="grid" value="grid" />' +
        '<select id="language" style="cursor:pointer;margin-left:5px;font-size:100%;' + sizeStyle + '">"' +

        langList.map(function (lang) {
            return '<option style="' + sizeStyle + '"' + ' value="' + lang + '">' + lang + '</option>';
        }).join('') + '</select>' +

        '<input ' + buttonStyleAndType + '" id="clearLog" value="Log" />' +

        '<select id="inception-ctrl" style="cursor:pointer;margin-left:5px;font-size:100%;' + sizeStyle + '">' +
        '<option value="">Default Inception</option>' +
        '<option value="true">Show Inception</option>' +
        '<option value="false">Hide Inception</option>' +
        '</select>';


    var langDropdownFromUrlIndex = langList.indexOf(getURLParameter('lang'));
    var langDropdownIndex = langDropdownFromUrlIndex > -1 ? langDropdownFromUrlIndex + 1 : 1;
    document.getElementById('language').querySelectorAll('option:nth-child(' + langDropdownIndex + ')')[0].selected = true;

    document.getElementById('apiVersion').addEventListener('change', function () {
        if (this.value === 'local') {
            clearUrlParameters(['server', 'apiVersion']);
            return;
        }
        var params = this.value.split('|');
        setURLParameters({server: params[0], apiVersion: params[1]});
    });

    document.getElementById('grid').addEventListener('click', function () {
        addGrid();
    });

    document.getElementById('clearLog').addEventListener('click', function () {
        document.getElementById('logger').innerHTML = '';
    });

    document.getElementById('language').addEventListener('change', function () {
        switchLang(this.value)
    });

    function switchLang(lang) {
        setURLParameter('lang', lang)
    }

    var inceptionControll = document.getElementById('inception-ctrl');
    inceptionControll.value = getURLParameter('inception') || '';
    inceptionControll.addEventListener('change', function (evt) {
        console.log(evt.target.value);
        if (evt.target.value === 'true') {
            setURLParameter('inception', true);
        } else if (evt.target.value === 'false') {
            setURLParameter('inception', false);
        } else {
            clearUrlParameters(['inception']);
        }
    });
}

function buildVersionSelect(sizeStyle) {
    var serverUrlParam = getURLParameter('server') || 'local';
    var versionUrlParam = getURLParameter('apiVersion');
    var localSelected = serverUrlParam === 'local' ? 'selected' : '';

    var optionTemplate = '<option style="' + sizeStyle + '" value="${value}" ${selected}>${text}</option>\n';

    function generateOptions(name) {
        return config.versions[name].map(function (version) {
            var selected = serverUrlParam === name && versionUrlParam === version ? 'selected' : '';
            var text = name.replace(/^./, function(l) {return l.toUpperCase()}).replace(/[A-Z]/, ' $&') + ' ' + version;
            return substitute(optionTemplate, {value: name + '|' + version, text: text, selected: selected});
        }).join('');
    }

    return '<select id="apiVersion" style="cursor:pointer;margin-left:5px;font-size:100%;' + sizeStyle + '">\n' +
    substitute(optionTemplate, {value: 'local', text: 'Local', selected: localSelected}) +
    generateOptions('testing') +
    generateOptions('production') +
    // generateOptions('enterpriseTesting') +
    // generateOptions('enterpriseProduction') +
    '</select>\n';
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
            '<h3 style="color: red;">' + (message || 'Обрати внимание. На странице ошибка') + '</h3>';
    }
}

var gridStatus = false;

function addGrid() {
    var paneNode = document.querySelectorAll('body div > ymaps')[0];
    if (!gridStatus) {
        paneNode.style.backgroundImage = "url('/cases/setka.png')";
    } else {
        paneNode.style.backgroundImage = '';
    }
    gridStatus = !gridStatus;
}

function getPrevNext() {
    var navigationElement = document.createElement('div');
    navigationElement.style.width = '100%';
    navigationElement.style.height = detectMob() ? '80px' : 'auto';
    var singleControlWidth = detectMob() ? '120px' : 'auto';
    window.document.body.appendChild(navigationElement);
    navigationElement.id = 'navigationElement';

    navigationElement.innerHTML = '' +
        '<a href="" id="prev"><input type="button" id="prevBt" style="cursor:pointer;margin:5px;font-size:100%;width:' + singleControlWidth + ';"' + ' value="prev" /></a>' +
        '<a href="" id="next"><input type="button" id="nextBt" style="cursor:pointer;margin:5px;font-size:100%;width:' + singleControlWidth + ';"' + ' value="next" /></a>' +
        '<a href="" id="home"><input type="button" style="cursor:pointer;margin-left:5px;font-size:100%;width:' + singleControlWidth + ';"' + ' value="home" /></a>' +
        '<input type="button" id="clear" style="cursor:pointer;margin-left:5px;font-size:100%;width:' + singleControlWidth + ';"' + ' value="clear" />';

    document.getElementById('home').href = window.location.href.replace(window.location.pathname.split('/manual/')[1], 'index.html');

    var currentPath = document.location.pathname.split('/cases/')[1];
    var currentFile;
        config.fileList.forEach(function(e, i) {
            if (e.split('/cases/')[1] == currentPath) {
            currentFile = i
            }
        });
    var prevLink = currentFile - 1;
    if (prevLink === -1) {
        document.getElementById('prevBt').setAttribute('value', 'none');
    } else {
        document.getElementById('prev').href = window.location.href.replace(config.fileList[currentFile].slice(1), config.fileList[prevLink].slice(1));
    }

    var nextLink = currentFile + 1;
    if (nextLink === config.fileList.length) {
        document.getElementById('nextBt').setAttribute('value', 'none');
    } else {
        document.getElementById('next').href = window.location.href.replace(config.fileList[currentFile].slice(1), config.fileList[nextLink].slice(1));
    }
    if (document.getElementById('clear').addEventListener) {
        document.getElementById('clear').addEventListener('click', function () {
            location.search = '';
        });
    } else {
        //ie8
        document.getElementById('clear').attachEvent('onclick', function () {
            location.search = '';
        });
    }
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
            console.error(err);
        }
    }
}

function detectMob() {
    if (navigator.userAgent.match(/Android/i) ||
        navigator.userAgent.match(/webOS/i) ||
        navigator.userAgent.match(/iPhone/i) ||
        navigator.userAgent.match(/iPad/i) ||
        navigator.userAgent.match(/iPod/i) ||
        navigator.userAgent.match(/BlackBerry/i) ||
        navigator.userAgent.match(/Windows Phone/i)
    ) {
        return true;
    } else {
        return false;
    }
}
