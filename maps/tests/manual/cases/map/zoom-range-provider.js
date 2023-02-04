/**
 * @module nk-zoom-range-provider
 */

ymaps.modules.define(
    'nk-zoom-range-provider',
    [
    ],
    function(
        provide) {

        var MIN_ZOOM = 0,
            MAX_ZOOM = 21;

        /**
         * @class ZoomRangeProvider
         * @implements ymaps:IZoomRangeProvider
         * @exports
         */
            console.log('суывдловдыла');
        var ZoomRangeProvider = inherit(/** @lends ZoomRangeProvider.prototype */{
            __constructor : function() {
                this.events = new ymaps.event.Manager();
            },

            getZoomRange : function() {
                return ymaps.vow.resolve([
                    MIN_ZOOM,
                    MAX_ZOOM
                ]);
            }
        });

        provide(ZoomRangeProvider);

    });
