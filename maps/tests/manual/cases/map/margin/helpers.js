function setupFakeMarginManager(parentElement, ns) {
    var ymaps = ns ? ns : window.ymaps;
    var originalMethod = ymaps.map.margin.Manager.prototype.addArea;
    ymaps.map.margin.Manager.prototype.addArea = function (area) {
        var result = originalMethod.call(this, area);
        var markElement = document.createElement('div');
        markElement.className = "rect";

        applyArea(area);

        parentElement.appendChild(markElement);

        var eventsGroup = result.events.group();

        eventsGroup.add('change', function () {
            applyArea(result.getArea());
        });

        result.events.once('remove', function () {
            eventsGroup.removeAll();
            parentElement.removeChild(markElement);
            markElement = null;
        });

        function applyArea(area) {
            markElement.style.cssText = '';
            for (var key in area) {
                if (area.hasOwnProperty(key)) {
                    var value = String(area[key]);
                    if (!isNaN(Number(value[value.length - 1]))) {
                        value += 'px';
                    }
                    markElement.style[key] = value;
                }
            }
        }

        return result;
    };
}

var marginRects = [];

function redrawMargins(map, parentElement) {
    for (var i = 0, j = marginRects.length; i < j; i++) {
        var elem = marginRects[i];
        elem.parentElement.removeChild(elem);
    }
    marginRects.length = 0;

    var margin = map.margin.getMargin();
    if (margin[0]) {
        createPanel({
            left: 0,
            right: 0,
            top: 0,
            height: margin[0]
        });
    }
    if (margin[1]) {
        createPanel({
            right: 0,
            top: 0,
            bottom: 0,
            width: margin[1]
        });
    }
    if (margin[2]) {
        createPanel({
            left: 0,
            right: 0,
            bottom: 0,
            height: margin[2]
        });
    }
    if (margin[3]) {
        createPanel({
            left: 0,
            top: 0,
            bottom: 0,
            width: margin[3]
        });
    }

    function createPanel(css) {
        var markElement = document.createElement('div');
        markElement.className = "panel";
        for (var key in css) {
            if (css.hasOwnProperty(key)) {
                markElement.style[key] = css[key] + 'px';
            }
        }
        parentElement.appendChild(markElement);
        marginRects.push(markElement);
    }
}