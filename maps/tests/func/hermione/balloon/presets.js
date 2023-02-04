require('../helper.js')(afterEach);

describe.skip('Балун / Метки', () => {
    // TODO сделать запуск как будут скриншоты
    describe.skip('Создание меток со всевозможными иконками и цветами', () => {
        // Всего цветов – 16, иконок – 58
        const colors = 0;
        const icons = 0;
        let c = 0;
        let i = 0;

        /**
         *
         * @param {Number} color – 1-16
         * @param {Number} icon – 1-58
         */
        function addPlacemark(color, icon) {
            it('Create placemark ' + color + ' ' + icon, function () {
                return this.browser
                    .keys('\uE007')
                    .leftClick(PO.ymaps.map(), 200, 200)
                    .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                    .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка выбора цвета')
                    .pause(200)
                    .click(PO.balloon.selectColor())
                    .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не открылось меню выбора цвета')
                    .catch(() => {
                        this.browser
                            .click(PO.balloon.selectColor())
                            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не открылось меню выбора цвета');
                    })
                    .click(PO.balloonColorMenu.item() + color)
                    .pause(300)
                    .click(PO.balloon.selectIcon())
                    .crWaitForVisible(PO.balloonIconsMenu(), 500, 'Не открылось меню выбора иконки')
                    .catch(() => {
                        this.browser
                            .click(PO.balloon.selectIcon())
                            .crWaitForVisible(PO.balloonIconsMenu(), 500, 'Не открылось меню выбора иконки');
                    })
                    .click(PO.balloonIconsMenu.item() + icon)
                    .pause(300)
                    .keys('\uE007')
                    .pause(200);
            });
        }

        before(function () {
            return this.browser
                .crInit('MANY_MAPS')
                .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карте')
                .crSetValue(PO.sidebar.mapName(), 'Пресеты меток')
                .click(PO.ymaps.addPlacemark())
                .pause(500);
        });

        after(function () {
            return this.browser
                .crSaveMap();
        });

        while (i <= icons) {
            while (c <= colors && i <= icons) {
                addPlacemark(c, i);
                c++;
                i++;
            }
            c = 0;
        }
    });
});
