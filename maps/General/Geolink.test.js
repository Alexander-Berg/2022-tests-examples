ymaps.modules.define(util.testfile(), [
    "Geolink",
    "util.dom.element",
    "util.dom.style",
    "util.css",
    "graphics.util.color"
], function (provide, geolink, utilDomElement, utilStyle, utilCss, graphicsUtilColor) {

    var geolinks, links;

    describe('geolink', function () {

        before(function (done) {
            this.timeout(4000);

            links = document.createElement('div');
            links.style.display = 'none';
            links.innerHTML = '<div style="background-color: #000; width: 400px; height: 20px; margin-left: 200px; font-size: 15px;">' +
                '<span class="ymaps-geolink" data-description="Вот это ирония!" data-bounds="[[55.6673,37.0959],[55.8585,38.0847]]">' +
                'ул. Строительная</span></div><br><div style="background-color: #555; width: 200px; height: 20px;">' +
                '<span class="ymaps-geolink" data-description="Москва">Москва</span>' +
                '</div><div style="background-color: #555; width: 200px; height: 20px;"><span class="ymaps-geolink" ' +
                'data-description="Москва" data-bounds="[[55.6673,37.0959],[55.8585,38.0847]]">Неверныйадрес</span></div>' +
                '<div style="background-color: #999; width: 200px; height: 20px; margin-left:60px;">' +
                '<span class="ymaps-geolink" data-bounds="[[55.6673,37.0959],[55.8585,38.0847]]">Москва</span>' +
                '</div><div style="background-color: #fff; width: 200px; margin-left:1240px;">' +
                '<span class="ymaps-geolink" data-description="Мое очень-очень-очень-очень-очень-очень-очень-очень-очень' +
                '-очень-очень-очень-очень-очень-очень длинное описание" data-bounds="[[55.6673,37.0959],[55.8585,38.0847]]"' +
                '>Москва</span></div><div><span class="ymaps-geolink" data-description="Мое не очень длинное описание" ' +
                'data-bounds="[[55.6673,37.0959],[55.8585,38.0847]]">Москва</span></div><div style="font-size: 10px;">' +
                '<span class="ymaps-geolink" data-description="Мое очень-не очень-очень-не очень-очень-не очень длинное ' +
                'описание" data-bounds="[[55.6673,37.0959],[55.8585,38.0847]]">Москва</span></div><div style="font-size: 20px;">' +
                '<span class="ymaps-geolink" data-description="Мое очень-не очень-очень-не очень-очень-не очень длинное описание" ' +
                'data-bounds="[[55.6673,37.0959],[55.8585,38.0847]]">Москва</span></div><div style="width: 250px; height: 200px;">' +
                'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa ' +
                '<span class="ymaps-geolink" data-description="Мое очень-не очень-очень-не очень-очень-не очень длинное описание" ' +
                'data-bounds="[[55.6673,37.0959],[55.8585,38.0847]]">Москва</span>. Cum sociis natoque penatibus et magnis dis ' +
                'parturient montes,</div><div style="font-size: initial; margin-top: 620px;"><span class="ymaps-geolink" ' +
                'data-description="Мое очень-не очень-очень-не очень-очень-не очень длинное описание" data-bounds="' +
                '[[55.6673,37.0959],[55.8585,38.0847]]">Москва</span></div>';
            document.body.appendChild(links);

            setTimeout(function () {
                geolinks = utilDomElement.findByClassName(document.body, utilCss.addPrefix("geolink-processed"), true);
                done();
            }, 2000)
        });

        after(function () {
            document.body.removeChild(links);
            links = null;
        });

        describe("geolinks", function () {
            it("Геоссылки должны быть добавлены на страницу с обновленным className", function (done) {
                expect(geolinks.length).not.to.be(0);
                done();
            });
            it("Геоссылки должны иметь атрибут title \"Показать на карте\"", function (done) {
                for (var i = 0; i < geolinks.length; i++) {
                    expect(geolinks[i].getAttribute("title")).to.be("Показать на карте");
                }
                done();
            });
        });
        describe("geolink color", function () {
            it("Геоссылка на черном фоне должна быть белого цвета", function () {
                expect(graphicsUtilColor.decode(utilStyle.value(geolinks[0], "color", true))).to.eql(['255', ' 255', ' 255', 1]);
            });
            it("Геоссылка на темном фоне должна быть белого цвета", function () {
                expect(graphicsUtilColor.decode(utilStyle.value(geolinks[1], "color", true))).to.eql(['255', ' 255', ' 255', 1]);
            });
            it("Геоссылка на светлом фоне должна быть синего цвета", function () {
                expect(graphicsUtilColor.decode(utilStyle.value(geolinks[3], "color", true))).to.eql(['0', ' 68', ' 187', 1]);
            });
            it("Геоссылка без фона должна быть синего цвета", function () {
                expect(graphicsUtilColor.decode(utilStyle.value(geolinks[4], "color", true))).to.eql(['0', ' 68', ' 187', 1]);
            });
        });
        describe("geolink size", function () {
            it("Геоссылка c размером шрифта 15px должна иметь иконку с отступом 18px", function () {
                expect(utilStyle.value(geolinks[0], "marginLeft", true)).to.be("18px");
            });
            it("Геоссылка c размером шрифта 10px должна иметь иконку с отступом 12px", function () {
                expect(utilStyle.value(geolinks[6], "marginLeft", true)).to.be("12px");
            });
            it("Геоссылка c размером шрифта 20px должна иметь иконку с отступом 24px", function () {
                expect(utilStyle.value(geolinks[7], "marginLeft", true)).to.be("24px");
            });
        });
    });

    provide({});
});
