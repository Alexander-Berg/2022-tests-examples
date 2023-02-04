/**
 * @fileOverview
 * Map margin visualizer: draws rectangles over map to show where are margins and
 * active area.
 */
ymaps.modules.define('tools.MapMarginVisualizer', [
    'util.extend',
    'util.bind',
    'util.array',
    'util.dom.element',
    'util.dom.style'
], function (provide, extend, bind, array, domElement, domStyle) {
    function Visualizer(map, toggle) {
        this._map = map;
        this._containerEvents = map.container.events.group()
            .add('sizechange', this._update, this);
        this._marginEvents = map.margin.events.group()
            .add('change', this._update, this);

        patchMarginManager(this._map.margin, bind(this._update, this));

        this.toggle(toggle);
    }

    extend(Visualizer.prototype, {
        destroy: function () {
            this._marginEvents.removeAll();
            this._containerEvents.removeAll();
        },

        toggle: function (state) {
            this._state = state;
            if (state) {
                this.draw();
            } else if (this._overlay) {
                this._overlay.parentNode.removeChild(this._overlay);
                this._overlay = null;
            }
        },

        draw: function () {
            if (this._overlay) {
                this._overlay.parentNode.removeChild(this._overlay);
                this._overlay = null;
            }

            this._overlay = domElement.create();
            domStyle.css(this._overlay, {
                position: 'absolute',
                left: '0',
                top: '0',
                width: '100%',
                height: '100%',
                pointerEvents: 'none'
            });

            this._map.container.getContainerElement().appendChild(this._overlay);

            var areas = getMarginAccessors(this._map.margin);
            areas = array.map(areas, function (x) { return x.getArea(); });
            for (var i = 0, l = areas.length; i < l; i++) {
                this._overlay.appendChild(makeAreaElement(areas[i], 'gray', 'lightgray'));
            }

            var margin = this._map.margin.getMargin();
            var marginArea = {
                top: margin[0],
                right: margin[1],
                bottom: margin[2],
                left: margin[3]
            };

            this._overlay.appendChild(makeAreaElement(marginArea, 'blue', 'lightblue'));
        },

        _update: function () {
            if (this._state) {
                this.draw();
            }
        }
    });

    function makeAreaElement(area, stroke, fill) {
        var el = domElement.create();

        function px(x) {
            return typeof x === 'undefined' ? null : x + 'px';
        }

        domStyle.css(el, {
            display: 'block',
            position: 'absolute',
            boxSizing: 'border-box',
            left: px(area.left),
            top: px(area.top),
            right: px(area.right),
            bottom: px(area.bottom),
            width: px(area.width),
            height: px(area.height),
            border: '1px solid ' + stroke,
            backgroundColor: fill,
            opacity: '0.5'
        });

        return el;
    }

    function getMarginAccessors(marginManager) {
        return marginManager._accessors.slice();
    }

    function patchMarginManager(marginManager, recalculateCallback) {
        var original = marginManager._recalculateMargin;
        marginManager._recalculateMargin = function () {
            recalculateCallback();
            original.apply(this, arguments);
        };
    }

    provide(Visualizer);
});