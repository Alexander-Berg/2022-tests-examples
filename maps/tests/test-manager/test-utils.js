TestUtils = {
    roundPoint: function (point, precision) {
        precision = precision || 0;
        point.setX((Math.round(point.getX() * Math.pow(10, precision))) / Math.pow(10, precision));
        point.setY((Math.round(point.getY() * Math.pow(10, precision))) / Math.pow(10, precision));
        return point;
    },

    bindContext: function (func, context) {
        return function() {
            return func.apply(context, arguments);
        };
    },

    createResumeHandler: function (callback) {
        return function () {
            var args = Array.prototype.slice.apply(arguments);
            this.resume( function () { callback.apply(this, args) } )
        }
    }
};

// Функция ожидающая загрузки SSG.
// ДОЛЖНА ВЫЗЫВАТЬСЯ В КОНТЕКСТЕ ТЕСТА.
waitWhileImagesLoads = function (imagesGetter, callback, waitLimit) {
    var flag = 1,
        images = imagesGetter.call(this);

    if (typeof waitLimit == "undefined") {
        waitLimit = Infinity;
    }

    for (var i = 0, l = images.length; i < l; i++) {
        flag = flag && images[i].parentNode;
        if (!flag) {
            break;
        }
    }

    if (flag) {
        callback.call(this);
    } else {
        if (waitLimit) {
            this.wait(function () {
                waitWhileImagesLoads.call(this, imagesGetter, callback, waitLimit - 1);
            }, 100);
        } else {
            Y.Assert.fail("Превышен лимит ожидания загрузки SSG.");
        }
    }
};

getGraphicsNodes = function (graphics) {
    var result = [];
    for (var i = 0, l = graphics._shape._shapes.length; i < l; i++) {
        result.push(graphics._shape._shapes[i].shape._htmlElement);
    }
    return result;
};

// Возвращает ноды на которых нужно генерить события.
getActiveNodes = function (graphics) {
    var result = [];
    for (var i = 0, l = graphics._shape._shapes.length; i < l; i++) {
        if (Graphics.SSG) {
            result.push(graphics._shape._shapes[i].shape._area._htmlElement);
        } else {
            result.push(graphics._shape._shapes[i].shape._htmlElement);
        }
    }
    return result;
};
