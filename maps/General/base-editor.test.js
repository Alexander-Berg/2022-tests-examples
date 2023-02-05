ymaps.modules.define(util.testfile(), [
    "util.defineClass",
    "Map",
    "geometry.LineString",
    "geometryEditor.Base",
    "util.eventEye",
    "vow",

    "geoObject.addon.editor"
], function (provide, defineClass, Map, LineStringGeometry, GeometryEditorBase, eventEye, vow) {
    describe("geometryEditor.Base", function () {
        var geoMap,
            geometry,
            editor;

        before(function () {
            geoMap = new Map("map", {
                center: [55, -120],
                zoom: 1,
                type: null,
                behaviors: ["scrollZoom", "drag"]
            });

            geometry = new LineStringGeometry([[0, 0], [1, 1]]);
            geometry.options.setParent(geoMap.options);
            geometry.setMap(geoMap);
        });

        beforeEach(function () {
            editor = new EditorMock(geometry);
            editor.options.setParent(geometry.options);
        });

        afterEach(function () {
            editor.destroy();
        });

        after(function () {
            geoMap.destroy();
        });

        it("Проверка цепочки событий editingstart, drawingstart, drawingstop, editingstop", function (done) {
            this.timeout(10000);

            eventEye.observe(editor,
                ["editingstart", "editingstop", "drawingstart", "drawingstop"]);

            var listeners = editor.events.group();
            listeners
                .add("drawingstop", function () { editor.stopEditing(); })
                .add("drawingstart", function () { editor.stopDrawing(); })
                .add("drawingstart", function () {
                    expect(eventEye.length()).to.be(4);
                    expect(eventEye.valuesOf("type")).to.be.eql([
                        "editingstart", "drawingstart", "drawingstop", "editingstop"
                    ]);
                    listeners.removeAll();
                    done();
                });

            editor.startDrawing();
        });

        it("Проверка событий при включении framing режима снуля", function (done) {
            this.timeout(10000);

            eventEye.observe(editor, [
                "editingstart", "editingstop",
                "drawingstart", "drawingstop",
                "framingstart", "framingstop",
                "statechange"
            ]);

            var listeners = editor.events.group();
            listeners
                .add("framingstart", function () {
                    editor.stopFraming();
                })
                .add("framingstop", function () {
                    expect(eventEye.length()).to.be(4);
                    expect(eventEye.valuesOf("type")).to.be.eql([
                        "statechange", "framingstart", "statechange", "framingstop"
                    ]);
                    listeners.removeAll();
                    done();
                });

            editor.startFraming();
        });
    });

    /******************************* EditorMock *******************************/

    function EditorMock (geometry, options) {
        EditorMock.superclass.constructor.call(this, geometry, options);
    }

    defineClass(EditorMock, GeometryEditorBase, {
        getModelClass: function () {
            function MockModel () {}

            defineClass(MockModel, {
                destroy: function () {}
            });

            return vow.resolve(MockModel);
        },

        getViewClass: function () {
            function MockView () {}

            defineClass(MockView, {
                setController: function () {},
                destroy: function () {}
            });

            return vow.resolve(MockView);
        },

        getControllerClass: function () {
            function MockController () {}

            defineClass(MockController, {
                destroy: function () {}
            });

            return vow.resolve(MockController);
        }
    });

    provide();
});
