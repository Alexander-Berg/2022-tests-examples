ymaps.modules.define(util.testfile(), [
    "util.AsyncStorage"
], function (provide, AsyncStorage) {
    describe('util.AsyncStorage', function () {

        var storage,
            modules = ymaps.modules,
            tmpModuleCounter = 0,
            getTmpModulesName = function () {
                return 'test_async_storage_' + (++tmpModuleCounter);
            },
            getTmpKey = function () {
                return 'triforce_' + (++tmpModuleCounter);
            };

        beforeEach(function () {
            storage = new AsyncStorage();
        });

        it('Должен получить значение', function (done) {
            storage.define('red', function (provide) {
                provide('red');
            });

            storage.require(['red'], function (red) {
                expect(red).to.be('red');
                done();
            });
        });

        it('Должен получить значение по единственному ключу', function (done) {
            storage.define('yellow', function (provide) {
                provide('yellow');
            });

            storage.require('yellow', function (yellow) {
                expect(yellow).to.be('yellow');
                done();
            });
        });

        it('Должен получить значение через несколько мс', function (done) {
            storage.define('red', function (provide) {
                setTimeout(function () {
                    provide({y: 'zz'});
                }, 100);
            });
            storage.require(['red'], function (redModule) {
                expect(redModule.y).to.be('zz');
                done();
            }, this);
        });

        describe('Функция обратного вызова', function () {

            // func
            it('Должен вызвать successCallback без контекста (success)', function (done) {
                storage.define('red', function (provide) {
                    provide('red');
                });

                storage.require(['red'], function (red) {
                    expect(red).to.be('red');
                    expect(this).to.be(window);
                    done();
                });
            });

            // func, cnt
            it('Должен вызвать successCallback с контекстом (success, cnt)', function (done) {
                storage.define('red', function (provide) {
                    provide('red');
                });

                storage.require(['red'], function (red) {
                    expect(red).to.be('red');
                    expect(this.a).to.be('123');
                    done();
                }, {
                    a: '123'
                });
            });

            // func, err
            it('Должен вызвать errorCallback без контекста (success, error)', function (done) {
                storage.define('red', function (provide) {
                    provide('red');
                });

                storage.require(['blue'], function (red) {
                    expect().fail('Был получен resolve');
                }, function (error) {
                    expect(error.message).to.be('The key "blue" isn\'t declared');
                    expect(this).to.be(window);
                    done();
                });
            });

            // func, err, cnt
            it('Должен вызвать errorCallback с контекстом (success, error, cnt)', function (done) {
                storage.define('red', function (provide) {
                    provide('red');
                });

                storage.require(['blue'], function (red) {
                    expect().fail('Был получен resolve');
                }, function (error) {
                    expect(error.message).to.be('The key "blue" isn\'t declared');
                    expect(this.a).to.be('123');
                    done();
                }, {
                    a: '123'
                });
            });
            // func, null, cnt
            it('Должен вызвать successCallback с контекстом при указании пустого errorCallback (success, null, cnt)', function (done) {
                storage.define('red', function (provide) {
                    provide('red');
                });

                storage.require(['red'], function (red) {
                    expect(red).to.be('red');
                    expect(this.a).to.be('123');
                    done();
                }, null, {
                    a: '123'
                });
            });
        });

        it('Множественные зависимости', function (done) {
            storage
                .define('red', function (provide) {
                    provide('red');
                })
                .define('blue', function (provide) {
                    provide('blue');
                })
                .define('green', function (provide) {
                    provide('green');
                })
                .define('black', function (provide) {
                    provide('black');
                });

            storage.require(['blue', 'black', 'blue'], function (blue, black, blue2) {
                expect(blue + black + blue2).to.be('blueblackblue');
                done();
            });
        });

        it('require должен отработать после вызова add', function (done) {
            storage.add('red', 'red');
            storage.require(['red'], function (red) {
                expect(red).to.be('red');
                expect(red == storage.get('red')).to.be.ok();
                done();
            });
        });

        describe('promise', function () {

            it('Должен получить значение используя объект-обещание', function (done) {
                storage
                    .define('red', function (provide) {
                        provide('red');
                    })
                    .define('blue', function (provide) {
                        provide('blue');
                    });

                storage.require(['blue', 'red'])
                    .spread(function (blue, red) {
                        expect(blue + red).to.be('bluered');
                    }).done(function () {
                        done();
                    }, function () {
                        expect().fail('Был получен reject');
                    });
            });

            it('Должна возратиться ошибка "ключ не найден"', function (done) {
                storage.require(['green'])
                    .done(
                    function (green) {
                        expect().fail('Был получен resolve');
                        done();
                    },
                    function (error) {
                        expect(error.message).to.be('The key "green" isn\'t declared');
                        done();
                    }
                );
            });

        });

        describe('Хранилище привязанное к модульной системе', function () {
            var tmpModulesSystemKey = 'asyncStorage_tmpKey';
            beforeEach(function () {
                storage = new AsyncStorage(tmpModulesSystemKey);
            });

            it('Должен вернуть ошибку отсутствия значения', function (done) {
                var key = getTmpKey();

                storage.require(key)
                    .done(function () {
                        expect().fail('Был получен resolve');
                    }, function (error) {
                        expect(error.message).to.be('The key "' + key + '" isn\'t declared');
                        done();
                    });
            });

            it('Должен вернуть значение из модульной системы через хранилище', function (done) {
                var key = getTmpKey();

                modules.define({
                    name: getTmpModulesName(),
                    storage: tmpModulesSystemKey,
                    key: key,
                    declaration: function (provide) {
                        provide({key: '123'});
                    }
                });

                storage.require(key)
                    .done(function (value) {
                        expect(value[0].key).to.be('123');
                        done();
                    }, function () {
                        expect().fail('Был получен reject');
                    });
            });

            it('Должен получить все три значения через модульную систему независимо от того, где оно было объявлено (в системе или в хранилище)', function (done) {
                var key1 = getTmpKey(),
                    key2 = getTmpKey(),
                    key3 = getTmpKey();

                modules.define({
                    name: getTmpModulesName(),
                    storage: tmpModulesSystemKey,
                    key: key1,
                    declaration: function (provide) {
                        provide({key: 'aaa'});
                    }
                });

                storage.define(key2, function (provide) {
                    provide({
                        key: 'zzz'
                    });
                });

                modules.define({
                    name: getTmpModulesName(),
                    storage: tmpModulesSystemKey,
                    key: key3,
                    declaration: function (provide) {
                        provide({key: 'bbb'});
                    }
                });

                storage.require([key1, key2, key3])
                    .done(function (value) {
                        expect(value[0].key).to.be('aaa');
                        expect(value[1].key).to.be('zzz');
                        expect(value[2].key).to.be('bbb');
                        done();
                    }, function () {
                        expect().fail('Был получен reject');
                    });
            });

            it('Должен вернуть сихронное значение их хранилища после инициализации модуля с расширенным синтаксисом', function (done) {
                var key = getTmpKey();
                modules.define({
                    name: getTmpModulesName(),
                    storage: tmpModulesSystemKey,
                    key: key,
                    declaration: function (provide) {
                        provide({key: 'VVVVVV'});
                    }
                });

                modules.require({
                    modules: {
                        key: key,
                        storage: tmpModulesSystemKey
                    }
                }).done(function (values) {
                    expect(storage.get(key)).to.eql(values[0]);
                    done();
                }, function () {
                    expect().fail('Был получен reject');
                });
            });

            it('Не должен получить из хранилища значение ', function (done) {
                var key1 = getTmpKey(),
                    key2 = getTmpKey();
                modules.define({
                    name: getTmpModulesName(),
                    storage: tmpModulesSystemKey,
                    key: key2,
                    declaration: function (provide) {
                        provide({key: 'VVVVVV'});
                    }
                });
                modules.define({
                    name: getTmpModulesName(),
                    storage: tmpModulesSystemKey,
                    key: key1,
                    dynamicDepends: {
                        depend1: function () {
                            return {
                                key: key2,
                                storage: tmpModulesSystemKey
                            };
                        }
                    },
                    declaration: function (provide) {
                        provide({key: 'VVVVVV'});
                    }
                });

                modules.require({
                    modules: {
                        key: key1,
                        storage: tmpModulesSystemKey
                    }
                }).done(function (values) {
                    expect(storage.get(key1)).to.eql(values[0]);
                    expect(storage.get(key1, {})).to.not.be.ok();

                    modules.require({
                        modules: {
                            key: key1,
                            storage: tmpModulesSystemKey
                        },
                        data: {}
                    }).done(function (values) {
                        expect(storage.get(key1, {})).to.eql(values[0]);
                        done();
                    }, function () {
                        expect().fail('Был получен reject при втором вызове require');
                    });
                }, function () {
                    expect().fail('Был получен reject при первом вызове require');
                });
            });

            it('Должен получить значение из хранилища с динамической зависимостью', function (done) {
                var key1 = getTmpKey(),
                    key2 = getTmpKey(),
                    checkValue = "";
                modules.define({
                    name: getTmpModulesName(),
                    storage: tmpModulesSystemKey,
                    key: key1,
                    dynamicDepends: {
                        layout: function (data) {
                            if (data.z == 1) {
                                checkValue += "1";
                            }
                            return {
                                key: key2,
                                storage: tmpModulesSystemKey
                            };
                        }
                    },
                    declaration: function (provide) {
                        provide(true);
                    }
                });

                modules.define({
                    name: getTmpModulesName(),
                    storage: tmpModulesSystemKey,
                    key: key2,
                    declaration: function (provide) {
                        checkValue += "2";
                        provide(true);
                    }
                });

                storage.require({
                    keys: [key1],
                    data: {z: 1}
                }).done(function (values) {
                    expect(checkValue).to.be("12");
                    done();
                }, function () {
                    expect().fail('Был получен reject');
                });
            });

            it('Должен получить значение из модульной системы, который был задан в хранилище синхронно.', function () {
                var key = getTmpKey(),
                    tmpValue = "tmp_value";

                storage.add(key, tmpValue);

                var moduleSystemValue = modules.requireSync({
                    key: key,
                    storage: tmpModulesSystemKey
                });

                expect(moduleSystemValue).to.be(tmpValue);
                expect(moduleSystemValue).to.be(storage.get(key));
            });

            it('Модуль должен получить в зависимости значение из хранилища. Значение задано синхронно', function (done) {
                var key = getTmpKey(),
                    moduleName = getTmpKey(),
                    tmpValue = "tmp_value";


                storage.add(key, tmpValue);

                modules.define(moduleName, [
                    {
                        key: key,
                        storage: tmpModulesSystemKey
                    }
                ], function (provide, storageValue) {
                    provide(storageValue);
                });


                modules.require(moduleName, function (module) {
                    expect(module).to.be(tmpValue);
                    done();
                }, function (e) {
                    done(e);
                });
            });

            it('Модуль должен получить в зависимости значение из хранилища. Значение задано асинхронно', function (done) {
                var key = getTmpKey(),
                    moduleName = getTmpKey(),
                    tmpValue = "tmp_value";

                storage.define(key, function (provide) {
                    provide(tmpValue);
                });

                modules.define(moduleName, [
                    {
                        key: key,
                        storage: tmpModulesSystemKey
                    }
                ], function (provide, storageValue) {
                    provide(storageValue);
                });


                modules.require(moduleName, function (module) {
                    expect(module).to.be(tmpValue);
                    done();
                }, function (e) {
                    done(e);
                });
            });

            it('Должен получить значение из модульной системы, который был задан в хранилище асинхронно.', function (done) {
                var key = getTmpKey(),
                    tmpValue = "tmp_value";

                storage.define(key, function (provide) {
                    provide(tmpValue);
                });

                modules.require({
                    key: key,
                    storage: tmpModulesSystemKey
                }).done(function (values) {
                    expect(values[0]).to.be(tmpValue);
                    expect(values[0]).to.be(storage.get(key));
                    done();
                }, function () {
                    expect().fail('Был получен reject');
                });
            });
        });

        it('Должен получить собственные модули', function (done) {
            var result;
            storage.define('red', function (provide) {
                provide('#FF0000');
            });

            storage.define('green', ['red'], function (provide, red) {
                result = "red=" + red;
                setTimeout(function () {
                    provide('#008000');
                }, 50);
            });

            storage.require(['green'], function (green) {
                result += "green=" + green;
                expect(result).to.be("red=#FF0000green=#008000");
                done();
            }, this);
        });

        it('Должны сработать вложенные зависимости на другие собственыне модули', function (done) {
            var result = '';
            storage.define('green', ['grey', 'orange'], function (provide) {
                setTimeout(function () {
                    result += "+green";
                    provide("green");
                }, 100);
            });

            storage.define('grey', [], function (provide) {
                setTimeout(function () {
                    result += "+grey";
                    provide("grey");
                }, 50);
            });

            storage.define('orange', ['red'], function (provide) {
                result += "+orange";
                provide("orange");
            });

            storage.define('red', function (provide) {
                setTimeout(function () {
                    result += "+red";
                    provide("red");
                }, 10);
            });

            storage.require(['green'])
                .spread(function (green) {
                    expect(green).to.be('green');
                    expect(result).to.be('+red+orange+grey+green');
                }).done(function () {
                    done();
                }, function (error) {
                    expect().fail('Был получен reject ' + error.message);
                    done();
                });
        });

        it('Должен сработал get после require', function (done) {
            storage
                .define('red', function (provide) {
                    provide({property: "---"});
                })
                .define('purple', ['red'], function (provide) {
                    setTimeout(function () {
                        provide('purple');
                    }, 50);
                });

            storage.require(['purple'])
                .done(function (green) {
                    expect(storage.get('purple')).to.be.ok();
                    expect(storage.get('red')).to.be.ok();
                    done();
                }, function (error) {
                    expect().fail('Был получен reject ' + error.message);
                    done();
                });

        });
    });

    provide({});
});
