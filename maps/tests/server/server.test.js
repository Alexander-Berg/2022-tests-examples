'use strict';

const request = require('supertest');
const app = require('../../server/app');

describe('Проверка ответов сервера', () => {
    it('Должен вернуть код 200 по запросу /ping', (done) => {
        request(app)
            .get('/ping')
            .expect(200, done);
    });

    it('Должен вернуть код 404 на несуществующий адрес', (done) => {
        request(app)
            .get('/cat_pictures')
            .expect(404, done);
    });

    describe('js', () => {

    });

    describe('show', () => {
        it('Должен установить кастомный namespace', (done) => {
            request(app)
                .get('/1.0/show/?ns=customNS')
                .end((err, res) => {
                    check(
                        done,
                        res.text.indexOf('global[\'customNS\'].modules') > -1
                    );
                });
        });

        it('Должен установить кастомный namespace с символами - и _', (done) => {
            request(app)
                .get('/1.0/show/?ns=custom-NS_')
                .end((err, res) => {
                    check(
                        done,
                        res.text.indexOf('global[\'custom-NS_\'].modules') > -1
                    );
                });
        });

        it('Должен установить стандартный namespace, если GET-параметр ns невалиден', (done) => {
            request(app)
                .get('/1.0/show/?ns=asd:Asd')
                .end((err, res) => {
                    check(
                        done,
                        res.text.indexOf('global[\'ym\'].modules') > -1
                    );
                });
        });

        it('Должен установить стандартный namespace, если GET-параметр ns не был указан', (done) => {
            request(app)
                .get('/1.0/show/')
                .end((err, res) => {
                    check(
                        done,
                        res.text.indexOf('global[\'ym\'].modules') > -1
                    );
                });
        });
    });

    /*
    TODO Надо использовать мок данных
    describe('error-cap', () => {
        it('Должен вернуть код error-cap, если карты нет', (done) => {
            request(app)
                .get('/1.0/js/?id=asd&amp;' +
                'um=constructor%3A0750b56825675e8d2ad21ac6e5e3678e32902c9eb3534b29d210b6c9c15e8436&amp;' +
                'width=800&amp;height=800&amp;lang=ru_RU&amp;scroll=true')
                .end((err, res) => {
                    let arg;
                    if (JSON.stringify(res).search(/ym.modules.require\(\'cnst.error-cap.main\'/i) === -1) {
                        arg = new Error('Нет модуля cnst.error-cap.main');
                    }
                    done(arg);
                });
        });
    });
    */

    describe('static', () => {
        it('Должен вернуть код 400 по запросу /1.0/static без sid', (done) => {
            request(app)
                .get('/1.0/static')
                .expect(400, done);
        });

        // TODO подумать про мок данных?

        // it('Должен вернуть код 302 по запросу /1.0/static с sid', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=PUVBivGSYPIC8t92J4JGY8azJr7Ylxos')
        //         .expect(302, done);
        // });

        // it('Должен вернуть код 302 по запросу /1.0/static с sid', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=PUVBivGSYPIC8t92J4JGY8azJr7Ylxos')
        //         .end((err, res) => {
        //             check(
        //                 done,
        //                 res.headers.location !== null,
        //                 'Отсутствует заголовок location'
        //             );
        //         });
        // });

        // it('Должен проборосить параметр width', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=PUVBivGSYPIC8t92J4JGY8azJr7Ylxos&width=333')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'size=333');
        //         });
        // });

        // it('Должен проборосить параметр height', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=PUVBivGSYPIC8t92J4JGY8azJr7Ylxos&height=251')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, '%2C251');
        //         });
        // });

        // it('Должен проборосить параметр lang', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=PUVBivGSYPIC8t92J4JGY8azJr7Ylxos&lang=tr_TR')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'tr_TR');
        //         });
        // });

        // it('Должен использовать стандартное значение lang', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=PUVBivGSYPIC8t92J4JGY8azJr7Ylxos')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'ru_RU');
        //         });
        // });

        // it('Должен выставить корректный центр', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=8uj2O2SBHO62zYBlZlqqDePzeJJgwIBc')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'll=37.731330974609264%2C55.722713078215385');
        //         });
        // });

        // it('Должен выставить корректный зум', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=8uj2O2SBHO62zYBlZlqqDePzeJJgwIBc')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'z=8');
        //         });
        // });

        // it('Должен добавить параметр pt', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=PUVBivGSYPIC8t92J4JGY8azJr7Ylxos')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'pt=37.431247887695314%2C55.73963150705164');
        //         });
        // });

        // it('Должен добавить параметр тип карты "Схема"', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=PUVBivGSYPIC8t92J4JGY8azJr7Ylxos')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'l=map');
        //         });
        // });

        // it('Должен добавить параметр тип карты "Спутник"', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=hftJ8LtbWMWLIe45PjUsP23kie63I-lM')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'l=sat');
        //         });
        // });

        // it('Должен добавить параметр тип карты "Гибрид"', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=WELyI-iM8YDSw_FcNNxsizYfY2no72Br')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'l=sat%2Cskl');
        //         });
        // });

        // it('Должен добавить параметр тип карты "Пробки"', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=WELyI-iM8YDSw_FcNNxsizYfY2no72Br')
        //         .end((err, res) => {
        //             checkLocationHeader(res, done, 'l=sat%2Cskl%2Ctrf');
        //         });
        // });

        // it('Должен добавить параметры для отображения зеленой и толстой линии', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=OPSCAetbFAxVKhzpcrelHMjHtY1Yr1YU')
        //         .end((err, res) => {
        //             checkLocationHeader(
        //                 res,
        //                 done,
        //                 'pl=c%3A1bad03e6%2Cw%3A15%2C37' +
        //                 '.685325725585905%2C55.81156673' +
        //                 '335981%2C37.49718485644528%2C55.666686507674'
        //             );
        //         });
        // });

        // it('Должен добавить параметры для отображения полигона', (done) => {
        //     request(app)
        //         .get('/1.0/static?sid=oodoHej4YAwgsJXSr0BCDAILvXU_mfBY')
        //         .end((err, res) => {
        //             checkLocationHeader(
        //                 res,
        //                 done,
        //                 '&pl=c%3Ab51effe6%2Cf%3Af371d' +
        //                 '199%2Cw%3A5%2C37.418907268554655%2C55.806926'
        //             );
        //         });
        // });
        // TODO перенапревление с учетом enterprise
    });
});

function check(done, condition, errorMessage) {
    errorMessage = errorMessage || 'oops!';
    if (condition) {
        done();
    } else {
        done(new Error(errorMessage));
    }
}

// function checkLocationHeader(res, done, substr) {
//     check(
//         done,
//         res.headers.location.indexOf(substr) > -1,
//         'oops!'
//     );
// }
