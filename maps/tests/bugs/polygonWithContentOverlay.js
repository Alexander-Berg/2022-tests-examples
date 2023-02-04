function createOverlayClass (ymaps) {
    function PolygonWithContentOverlay (geometry, data, options) {
        PolygonWithContentOverlay.superclass.constructor.call(this, geometry, data, options);
    }

    ymaps.util.augment(PolygonWithContentOverlay, ymaps.overlay.interactiveGraphics.Polygon, {
        onAddToMap: function () {
            PolygonWithContentOverlay.superclass.onAddToMap.call(this);
            this._monitor = new ymaps.Monitor(this.options);
            this._monitor.add("contentLayout", this._onContentLayoutChange, this);
            this._createContent();
        },

        onRemoveFromMap: function () {
            this._destroyContent();
            this._monitor.destroy();
            PolygonWithContentOverlay.superclass.onRemoveFromMap.call(this);
        },

        applyGeometry: function () {
            PolygonWithContentOverlay.superclass.applyGeometry.call(this);
            this._applyPosition();
        },

        _createContent: function () {
            this._contentElement = ymaps.util.dom.element.create({
                css: {
                    position: "absolute",
                    zIndex: 1000
                }
            });
            this._contentPane = this.getMap().panes.get("graphics");
            this._contentPane.getElement().appendChild(this._contentElement);
            this._contentPane.events.add('actionend', this._applyPosition, this);

            this._applyPosition();

            var contentLayoutClass = ymaps.layout.storage.get(this._monitor.get("contentLayout"));
            this._contentLayout = new contentLayoutClass(ymaps.util.extend({}, this.getData(), {
                options: this.options
            }));
            this._contentLayout.setParentElement(this._contentElement);
        },

        _destroyContent: function () {
            this._contentLayout.setParentElement(null);
            ymaps.util.dom.element.remove(this._contentElement);
            this._contentPane.events.remove('actionend', this._applyPosition, this);
            this._contentPane = null;
            this._contentElement = null;
        },

        _onContentLayoutChange: function (contentLayoutClass) {
            if (this._contentLayout) {
                this._contentLayout.setParentElement(null);
            }
            this._contentLayout = new contentLayoutClass(this.getData());
            this._contentLayout.setParentElement(this._contentElement);
        },

        _applyPosition: function () {
            // Ищем середину.
            var coordinates = this.getGeometry().getCoordinates(),
                center = [0, 0],
                cnt = 0;
            for (var i = 0, l = coordinates.length; i < l; i++) {
                for (var j = 0, k = coordinates[i].length; j < k; j++) {
                    center[0] += coordinates[i][j][0];
                    center[1] += coordinates[i][j][1];
                    cnt++;
                }
            }
            center[0] /= cnt;
            center[1] /= cnt;
            ymaps.util.dom.style.setPosition(this._contentElement, this._contentPane.toClientPixels(center));
        }
    });

    return PolygonWithContentOverlay;
}