function startTest (params) {
    var tileSize = params.hasOwnProperty('tileSize') ? parseInt(params.tileSize) : (params.tileSize = 256),
        dragSpeed = params.hasOwnProperty('dragSpeed') ? parseFloat(params.dragSpeed) : (params.dragSpeed = 10),
        useRAF = params.hasOwnProperty('useRAF') ? !!params.useRAF : (params.useRAF = false),
        test = params.hasOwnProperty('test') ? params.test : (params.test = 'drag'),
        zoomSpeed = params.hasOwnProperty('zoomSpeed') ? parseFloat(params.zoomSpeed) : (params.zoomSpeed = 30), // "pixels"
        showFPSMeter = params.hasOwnProperty('fps') ? !!parseInt(params.fps) : true,
        hd = params.hasOwnProperty('hd') ? !!parseInt(params.hd) : false,
        xm = Math.ceil(params.containerWidth / tileSize),
        ym = Math.ceil(params.containerHeight / tileSize),
        images = {},
        offset = 0,
        coorOffset = 0,
        zoom = 0,
        urlPattern = [
            (params.hasOwnProperty('tileUrl') ? params.tileUrl : 'http://sat02.maps.yandex.net/tiles') + '?l=sat&v=3.167.0&x=',
            '&y=',
            '&z=16&lang=ru_RU'
        ];

    if (hd) {
        urlPattern.push('&scale=2');
        params.tileSize *= 2;
        tileSize *= 2;
    }

    if (test == 'drag') {
        zoomSpeed = 0;
    } else {
        dragSpeed = 0;
        zoomSpeed = zoomSpeed / 1000;
    }

    tick();

    function tick () {
        offset += dragSpeed;

        if (zoomSpeed != 0) {
            zoom += zoomSpeed;
            if (zoom >= 1) {
                zoomSpeed *= -1;
                zoom = 1;
            } else if (zoom <= 0) {
                zoomSpeed *= -1;
                zoom = 0;
            }
        }
        if (offset >= tileSize) {
            offset -= tileSize;
            --coorOffset;
        }

        if (params.beforeCallback) {
            params.beforeCallback(params, {
                localOffset: offset,
                globalOffset: offset - (tileSize * coorOffset)
            });
        }

        var showImages = {};

        for (var x = -1; x < xm; x++) {
            for (var y = -1; y < ym; y++) {
                var img,
                    curX = x + coorOffset,
                    curY = y + coorOffset;
                if (!images[curX]) {
                    images[curX] = {};
                }

                if (!images[curX][curY]) {
                    img = new Image();
                    img.src = urlPattern[0] + (39614 + curX) + urlPattern[1] + (20523 + curY) + urlPattern[2];
                    images[curX][curY] = {
                        img: img,
                        added: false
                    };
                }

                img = images[curX][curY].img;

                var data = {
                    img: img,
                    localOffset: offset,
                    globalOffset: offset - (tileSize * coorOffset),
                    localX: x,
                    localY: y,
                    globalX: curX,
                    globalY: curY,
                    scale: zoom + 1
                };

                if (img.complete) {
                    if (!images[curX][curY].added) {
                        if (params.tileWasAddedCallback) {
                            params.tileWasAddedCallback(params, data);
                        }
                    }
                    images[curX][curY].added = true;
                    if (params.eachCallback) {
                        params.eachCallback(params, data);
                    }
                }

                showImages[curX + '.' + curY] = true;
            }
        }

        for (var x in images) {
            for (var y in images[x]) {
                if (!showImages[x + '.' + y] && images[x] && images[x][y] && images[x][y].added) {
                    images[x][y].added = false;
                    var img = images[x][y].img;
                    if (params.tileWasRemovedCallback) {
                        params.tileWasRemovedCallback(params, {
                            img: img,
                            globalX: x,
                            globalY: y
                        });
                    }
                }
            }
        }

        if (params.afterCallback) {
            params.afterCallback(params);
        }

        if (fps) {
            fps.tick();
        }

        if (params.useRAF) {
            window.requestAnimationFrame(tick);
        } else {
            setTimeout(tick, 1);
        }
    }

    if (showFPSMeter) {
        // FPS meter
        var fpsMeterEl = document.createElement('div');
        fpsMeterEl.style.position = 'absolute';
        fpsMeterEl.style.right = '5px';
        fpsMeterEl.style.top = '5px';
        fpsMeterEl.style.backgroundColor = 'white';
        fpsMeterEl.style.width = '100px';
        fpsMeterEl.style.textAlign = 'center';
        document.body.appendChild(fpsMeterEl);

        var result,
            fps = {
                startTime: 0,
                frameNumber: 0,
                tick: function () {
                    this.frameNumber++;
                },
                _getFPS: function () {
                    var d = new Date().getTime(),
                        currentTime = (d - this.startTime) / 1000;
                    if (currentTime > 1) {
                        result = Math.floor((this.frameNumber / currentTime));
                        this.startTime = new Date().getTime();
                        this.frameNumber = 0;
                    }
                    return result;
                }
            };
        gameLoop();
        var st = +(new Date()), st_tick = 0, stTimeout = 18, st_val = 0;
        setInterval(function () {
            if (st_tick++ > 10) {
                var et = +(new Date()),
                    delta = et - st;

                st_val = Math.round(delta / st_tick) + '(' + stTimeout + ')';
                st_tick = 0;
                st = et;
            }
        }, stTimeout);
    }

    function gameLoop () {
        setTimeout(gameLoop, 3000);
        fpsMeterEl.innerHTML = fps._getFPS() + ' fps /' + st_val;
    }
}
