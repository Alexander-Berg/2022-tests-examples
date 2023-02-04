function Api(load){
    if(load == null) load = "package.full";
    var apiScript = window.document.createElement('script');
    var head = window.document.getElementsByTagName('head')[0] || document.documentElement;
    apiScript.src = "http://localhost:8080/2.0/?lang=ru-RU&ns=ym&load=" + load;
    apiScript.src += "&mode=debug";
    head.appendChild(apiScript);
    window.onload = function () {
        return ym;
    }
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
    if(map.getType() == "yandex#satellite"){
        l = new ym.yandex.layer.Sat();
    }
    else if(map.getType() == "yandex#map"){
        l = new ym.yandex.layer.Map();
    }
    else if(map.getType() == "yandex#peoplesMap"){
        l = new ym.yandex.layer.PeoplesMap();
    }
    else if(map.getType() == "yandex#peoplesHybrid"){
        l = new ym.yandex.layer.PeoplesSkeleton();
    }
    else if(map.getType() == "yandex#hybrid"){
        l = new ym.yandex.layer.Skl();
    }
    return l;
}

function getURLParameter(name) {
    return decodeURI(
        (RegExp(name + '=' + '(.+?)(&|$)').exec(location.search)||[,null])[1]
    );
}

