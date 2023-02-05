ymaps.modules.define(util.testfile(), [
    'Template',
    'template.filtersStorage',
    'data.Manager'
], function (provide, Template, filtersStorage, DataManager) {

    describe('Template', function () {

        describe('Старый формат шаблонов', function () {

            it('if', function () {
                var text = 'a*[if b]$[yes][endif][if b == 3]-no$[c][else]-yes[endif]/[if whtsp=="what\'s up"&&true&&b]yes[else][endif][ifdef b][else]no[endif]',
                    build = (new Template(text)).build(new DataManager({
                        yes: 'yes',
                        b: true,
                        whtsp: "what's up",
                        c: false
                    }));

                expect(build.text).to.equal('a*yes-yes/yes');
                expect(build.renderedValues).to.have.keys(['yes', 'b', 'whtsp']);
            });

            it('nested ifs', function () {
                var text = '[if a]yes-[if b == "test" && 0 === null || 2 > 2e6]no[ifdef c]no[else][endif][endif][endif]',
                    build = (new Template(text)).build(new DataManager({
                        a: true,
                        b: 'test',
                        c: true
                    }));

                expect(build.text).to.be('yes-');
                expect(build.renderedValues).to.have.keys(['a', 'b']);
                expect(build.renderedValues).not.to.have.key('c');
            });

            it('sublayouts', function () {
                var text = '<ymaps>$[[options.contentLayout observeSize name = "balloonContent" maxWidth = options.maxWidth maxHeight = options.maxHeight minWidth = options.minWidth  minHeight = options.minHeight]]</ymaps>[if options.closeButton]$[[options.closeButtonLayout]][endif]',
                    t = new Template(text),
                    build = t.build(new DataManager({
                        options: {
                            closeButton: true,
                            minHeight: 30
                        }
                    }));

                expect(build.sublayouts).to.have.length(2);
                expect(build.sublayouts[0].key).to.be('options.contentLayout');
                expect(build.sublayouts[1].key).to.be('options.closeButtonLayout');
                expect(build.sublayouts[0]).to.have.key('monitorValues');
                expect(build.sublayouts[0].monitorValues).to.have.length(4);
                expect(build.sublayouts[0]).to.have.key('observeSize');
                expect(build.sublayouts[0]).to.have.key('name');
                expect(build.sublayouts[0].name).to.be('balloonContent');
                expect(build.sublayouts[0]).to.have.key('minHeight');
                expect(build.sublayouts[0].minHeight).to.be(30);
            });

            it('matching', function () {
                var text = '[if var1]trololo-$[[sub1]]-$[var2]-tro[l]olo-[else][endif]text[if var3]-$[var4|trololo]-test-$[[sub2]]$[[sub2]]-test[endif]test',
                    t = new Template(text),
                    build = t.build(new DataManager({
                        var1: false,
                        var2: null,
                        var3: true,
                        var4: undefined
                    }));

                expect(build.text).to.match(/text\-trololo\-test\-(.+)\-testtest/);
                expect(build.sublayouts).to.have.length(2);
                expect(build.sublayouts[0].key).to.be('sub2');
                expect(build.sublayouts[1].key).to.be('sub2');
                expect(build.renderedValues).to.have.keys(['var1', 'var3', 'var4']);
                expect(build.renderedValues).not.to.have.keys(['var2']);
            });

        });

        describe('Новый формат шаблонов', function () {

            it('Условие', function () {
                var text = 'a*{% if  b %}{{ yes}}{% endif%}{%if b == 3%}-no{{c}}{%else%}-yes{% endif %}/{% if whtsp=="what\'s up"&&true&&b%}yes{%else %}{%endif%}{%if typeof e == "undefined"%}*yes{%else%}no{%endif%}',
                    build = (new Template(text)).build(new DataManager({
                        yes: 'yes',
                        b: true,
                        whtsp: "what's up",
                        c: false
                    }));

                expect(build.text).to.be('a*yes-yes/yes*yes');
                expect(build.renderedValues).to.have.keys(['yes', 'b', 'whtsp']);
            });

            it('Вычисления в if', function () {
                var text = '{% if (a + b) == 7 %}yes{% else %}no{% endif %}',
                    build = (new Template(text)).build(new DataManager({
                        a: 4,
                        b: 3
                    }));

                expect(build.text).to.be('yes');
                expect(build.renderedValues).to.have.keys(['a', 'b']);
            });

            it('Обращение к полям объектов в дате через точку', function () {
                var text = '{{ a.b.c.d }}',
                    build = (new Template(text)).build(new DataManager({
                        a: {
                            b: {
                                c: {
                                    d: '123'
                                }
                            }
                        }
                    }));

                expect(build.text).to.be('123');
                expect(build.renderedValues).to.have.keys(['a', 'a.b', 'a.b.c', 'a.b.c.d']);
            });

            it('Обращение к элементам в дате через скобки', function () {
                var text = '{{ testNum[0] }}{{ testNum[ 2  ] }}{{ testString["test"] }}{{ testString["tt"][0] }}',
                    build = (new Template(text)).build(new DataManager({
                        testNum: [5, 4, 3, 2, 1],
                        testString: {
                            test: "test",
                            tt: [1, 2, 3, 4]
                        }
                    }));

                expect(build.text).to.be('53test1');
                expect(build.renderedValues).to.have.keys([
                    'testNum', 'testString', 'testNum.0', 'testNum.2',
                    'testString.test', 'testString.tt', 'testString.tt.0'
                ]);
            });

            /*it('Должен корректно работать в смешанных конструкциях: скобки + точки при обращении к данным', function () {
                    debugger;
                var text = '{{ users.0.name }}-{{ users[1]["name"] }}-{{ users.0.stat[0].num }}-{{ users[1].stat.0["num"] }}',
                    build = (new Template(text)).build(new DataManager({
                        users: [
                            {
                                name: "n1",
                                age: 30,
                                stat: [{num: 1}, 2, 3]
                            }, {
                                name: "n2",
                                age: 10,
                                stat: [{num: 4}, 5, 6]
                            }, {
                                name: "n3",
                                age: 24,
                                stat: [{num: 7}, 8, 9]
                            }
                        ]
                    }));

                expect(build.text).to.be('n1-n2-1-4')
            });*/

            it('Стандартное значение', function () {
                var text = '{{ value1|default:value2|raw }} {{ value2|default:value3|raw }} {{ value2|default:"111"|raw }} {{ value2|default:\'Zzz\'|raw }} {{ value4|default:234|raw }} {{ value4|default:-100.5123|raw }} {{ value4|default:\'0\'|raw }} {{ value4|default:0|raw }} +++';

                var build = (new Template(text)).build(new DataManager({
                    value1: "1",
                    value3: "123"
                }));

                expect(build.text).to.be("1 123 111 Zzz 234 -100.5123 0 0 +++");
            });

            it('Вложенное условие', function () {
                var text = '{% if a %}yes-{%if b == "test" && 0 === null || 2 > 2e6%}no{%if typeof c != "undefined" %}no{%else%}{%endif%}{%endif%}{%endif%}',
                    build = (new Template(text)).build(new DataManager({
                        a: true,
                        b: 'test',
                        c: true
                    }));

                expect(build.text).to.be('yes-');
                expect(build.renderedValues).to.have.keys(['a', 'b']);
                expect(build.renderedValues).not.to.have.key('c');
            });

            it('Elseif условия', function () {
                var text = '{% if a %}yes{% elseif b %}elseif_b{% elseif c  == "test"  %}elseif_c{% else %}no{% endif %}',
                    build = (new Template(text)).build(new DataManager({
                        a: false,
                        b: null,
                        c: 'test',
                        d: true
                    }));

                expect(build.text).to.be('elseif_c');
                expect(build.renderedValues).to.have.keys(['a', 'b', 'c']);
                expect(build.renderedValues).not.to.have.key('d');
            });

            it('Elseif условие со сложенным if-elseif', function () {
                var text = '{% if a %}yes{% elseif !b %}' +
                        '{% if a %}depth_a{% elseif !a %}depth_elseif_a{% endif %}' +
                        '{% elseif c  == "test"  %}elseif_c' +
                        '{% else %}no{% endif %}',
                    build = (new Template(text)).build(new DataManager({
                        a: false,
                        b: null,
                        c: 'test',
                        d: true
                    }));

                expect(build.text).to.be('depth_elseif_a');
                expect(build.renderedValues).to.have.keys(['a', 'b']);
                expect(build.renderedValues).not.to.have.keys(['d', 'c']);
            });

            it('matching', function () {
                var text = '{% if var1 %}trololo-{% include sub1 %}-{{var2}}-tro[l]olo-{%else%}{%endif%}text{% if var3 %}-{{var4|default:"trololo"}}-test-{% include sub2 %}{% include sub2 %}-test{% endif %}{{ test }}{{ test|raw }}{{ var4|default:test|raw }}',
                    t = new Template(text),
                    build = t.build(new DataManager({
                        var1: false,
                        var2: null,
                        var3: true,
                        var4: undefined,
                        test: '<>\'"!'
                    }));

                expect(build.text).to.match(/text\-trololo\-test\-(.+)\-test&lt;&gt;&#39;&quot;!<>'"!<>'"!/);
                expect(build.sublayouts).to.have.length(2);
                expect(build.sublayouts[0].key).to.be('sub2');
                expect(build.sublayouts[1].key).to.be('sub2');
                expect(build.renderedValues).to.have.keys(['var1', 'var3', 'var4']);
                expect(build.renderedValues).not.to.have.keys(['var2']);
            });

            it('Подмакет', function () {
                var text = '<ymaps>{% include options.contentLayout observeSize name = "balloonContent" maxWidth = options.maxWidth maxHeight = options.maxHeight minWidth = options.minWidth minHeight = options.minHeight%}</ymaps>{%if options.closeButton%}{%include options.closeButtonLayout%}{% endif %}',
                    t = new Template(text),
                    build = t.build(new DataManager({
                        options: {
                            closeButton: true,
                            minHeight: 30
                        }
                    }));

                expect(build.sublayouts).to.have.length(2);
                expect(build.sublayouts[0].key).to.be('options.contentLayout');
                expect(build.sublayouts[1].key).to.be('options.closeButtonLayout');
                expect(build.sublayouts[0]).to.have.key('monitorValues');
                expect(build.sublayouts[0].monitorValues).to.have.length(4);
                expect(build.sublayouts[0]).to.have.key('observeSize');
                expect(build.sublayouts[0]).to.have.key('name');
                expect(build.sublayouts[0].name).to.be('balloonContent');
                expect(build.sublayouts[0]).to.have.key('minHeight');
                expect(build.sublayouts[0].minHeight).to.be(30);
            });

            describe('for', function () {

                it('Простой перебор элементов массива', function () {
                    var text = [
                        '{% for elem in elements %}',
                        '{{ elem }}!={{ someObj.first }},',
                        '{% endfor %}'
                    ].join('');

                    var build = (new Template(text)).build(new DataManager({
                        elements: ['1', '2', '3'],
                        someObj: {first: 'zzz'}
                    }));

                    expect(build.text).to.be('1!=zzz,2!=zzz,3!=zzz,');
                    expect(build.renderedValues).to.have.keys(['elements', 'elements.0',
                                                               'elements.1', 'elements.2',
                                                               'someObj', 'someObj.first']);
                    expect(build.renderedValues['elements.0'].value).to.be('1');
                    expect(build.renderedValues['elements.1'].value).to.be('2');
                    expect(build.renderedValues['elements.2'].value).to.be('3');
                    expect(build.renderedValues['someObj.first'].value).to.be('zzz');
                });

                it('Получение key, value', function () {
                    var text = [
                        '{% for key, value in array %}',
                        '{{ key }}={{ value }},',
                        '{% endfor %}',
                        '_',
                        '{% for key, value in objects %}',
                        '{{title}}={{ key }}={{ value.sub_property }},',
                        '{% endfor %}'
                    ].join('');

                    var build = (new Template(text)).build(new DataManager({
                        array: ['aaa', 'bbb', 'ccc'],
                        objects: {
                            key1: {sub_property: 'value1'},
                            key2: {sub_property: 'value2'},
                            key3: {sub_property: 'value3'}
                        },
                        title: 'title'
                    }));

                    expect(build.text).to.be('0=aaa,1=bbb,2=ccc,_title=key1=value1,title=key2=value2,title=key3=value3,');
                    expect(build.renderedValues).to.have.keys(['title',
                                                               'array', 'array.0', 'array.1',
                                                               'array.2',
                                                               'objects', 'objects.key1',
                                                               'objects.key2', 'objects.key3',
                                                               'objects.key1.sub_property',
                                                               'objects.key2.sub_property',
                                                               'objects.key3.sub_property']);

                    expect(build.renderedValues['title'].value).to.be('title');
                    expect(build.renderedValues['array.0'].value).to.be('aaa');
                    expect(build.renderedValues['array.1'].value).to.be('bbb');
                    expect(build.renderedValues['array.2'].value).to.be('ccc');
                    expect(build.renderedValues['objects.key1.sub_property'].value).to.be('value1');
                    expect(build.renderedValues['objects.key2.sub_property'].value).to.be('value2');
                    expect(build.renderedValues['objects.key3.sub_property'].value).to.be('value3');
                });

                it('Вложенный массив', function () {
                    var text = [
                        '{% for value in array %}',
                        '{% for subValue in value %}',
                        '{{ subValue }}={{ title }},',
                        '{% endfor %}',
                        '_',
                        '{% for value in objects %}',
                        '{{ value.subkey.subsubkey }},',
                        '{% endfor %}',
                        '{% endfor %}'
                    ].join('');

                    var build = (new Template(text)).build(new DataManager({
                        array: [
                            [1, 2],
                            [3, 4]
                        ],
                        objects: {
                            key1: {subkey: {subsubkey: "111"}},
                            key2: {subkey: {subsubkey: "222"}}
                        },
                        title: 'ZzZ'
                    }));

                    expect(build.text).to.be('1=ZzZ,2=ZzZ,_111,222,3=ZzZ,4=ZzZ,_111,222,');
                    expect(build.renderedValues).to.have.keys(
                        ['title',
                         'array', 'array.0', 'array.1', 'array.0.0', 'array.0.1', 'array.1.0',
                         'array.1.1',
                         'objects', 'objects.key1', 'objects.key2', 'objects.key1.subkey',
                         'objects.key2.subkey', 'objects.key1.subkey.subsubkey',
                         'objects.key2.subkey.subsubkey'
                        ]);

                    expect(build.renderedValues['title'].value).to.be('ZzZ');
                    expect(build.renderedValues['array.0.0'].value).to.be(1);
                    expect(build.renderedValues['array.0.1'].value).to.be(2);
                    expect(build.renderedValues['array.1.0'].value).to.be(3);
                    expect(build.renderedValues['array.1.1'].value).to.be(4);

                    expect(build.renderedValues['objects.key1.subkey.subsubkey'].value).to.be('111');
                    expect(build.renderedValues['objects.key2.subkey.subsubkey'].value).to.be('222');
                });

            });

            describe('empty', function () {

                var data;

                before(function () {
                    data = new DataManager({
                        data: {
                            title: '123',
                            bb: true,
                            elements: [1, 2, 3],
                            elements3: []
                        }
                    });
                });

                it('Подстановка по ключу', function () {
                    var t = new Template('{{ data.title }}'),
                        build = t.build(data);
                    expect(build.empty).to.be(false);
                });

                it('Подстановка по несуществующему ключу', function () {
                    var t = new Template('{{ data.title2 }}'),
                        build = t.build(data);
                    expect(build.empty).to.be(true);
                });

                it('Просто текст', function () {
                    var t = new Template('12фывфыв3'),
                        build = t.build(data);
                    expect(build.empty).to.be(false);
                });

                it('Пустая строка', function () {
                    var t = new Template(''),
                        build = t.build(data);
                    expect(build.empty).to.be(true);
                });

                it('Пробел', function () {
                    var t = new Template(' '),
                        build = t.build(data);
                    expect(build.empty).to.be(false); // TODO?
                });

                it('Положительное условие', function () {
                    var t = new Template('{% if data.bb %}{{ data.title }}{% endif %}'),
                        build = t.build(data);
                    expect(build.empty).to.be(false);
                });

                it('Отрицательное условие', function () {
                    var t = new Template('{% if data.cc %}123{% endif %}'),
                        build = t.build(data);
                    expect(build.empty).to.be(true);
                });

                it('Стандартное значение по ключу', function () {
                    var t = new Template('{{ data.title2|default:data.title }}'),
                        build = t.build(data);
                    expect(build.empty).to.be(false);
                });

                it('Стандартное значение - текст', function () {
                    var t = new Template('{{ data.title2|default:"123" }}'),
                        build = t.build(data);
                    expect(build.empty).to.be(false);
                });

                it('Обычный цикл', function () {
                    var t = new Template('{% for element in data.elements %}element{% endfor %}'),
                        build = t.build(data);
                    expect(build.empty).to.be(false);
                });

                it('Несуществующий цикл', function () {
                    var t = new Template('{% for element in data.elements2 %}element{% endfor %}'),
                        build = t.build(data);
                    expect(build.empty).to.be(true);
                });

                it('Пустой массив', function () {
                    var t = new Template('{% for element in data.elements3 %}element{% endfor %}'),
                        build = t.build(data);
                    expect(build.empty).to.be(true);
                });
            });

            describe('Фильтры', function () {
                it ('Фильтр raw', function () {
                    var data = new DataManager({
                            title: '<h1>!</h1>'
                        }),
                        t = new Template('{{ title|raw }}'),
                        build = t.build(data);
                    expect(build.text).to.be('<h1>!</h1>');
                });

                it ('Фильтр default', function () {
                    var data = new DataManager({}),
                        t = new Template('{{ title|default:"<h1>!</h1>"|raw }}'),
                        build = t.build(data);
                    expect(build.text).to.be('<h1>!</h1>');
                });

                it ('Пользовательские фильтры', function () {
                    var formatDate = function (data, value, filterValue) {
                        var date = value.split('.'),
                            months = ["january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"];

                        date[1] = months[parseInt(date[1], 10) - 1];
                        return date.join(' ');
                    };

                    filtersStorage.add('formatDate', formatDate);
                    var templateString = '{{ date|formatDate }}',
                        templateData = new DataManager({
                            date: '23.10.2014'
                        });

                    var build = (new Template(templateString)).build(templateData);

                    expect(build.text).to.be('23 october 2014');
                });
            });

            describe('CSP', function () {
                it('STYLE', function () {
                    //TODO: Прокинуть дырку для доступа в .env
                    //var cspValue = ymaps.env.server.params.csp;
                    //ymaps.env.server.params.csp=false;
                    var text = '<a {%style%}color:{{red}}{%endstyle%}></a>',
                        build = (new Template(text)).build(new DataManager({
                            red: 'red'
                        }));

                    expect(build.text).to.equal('<a style="color:red"></a>');

                    /*
                    ymaps.env.server.params.csp=true;
                    var text = '<a {%style%}color:{{red}}{%endstyle%}></a>',
                        build = (new Template(text)).build(new DataManager({
                            red: 'red'
                        }));

                    expect(build.text).to.equal('<a data-ymaps-style="color:red"></a>');

                    *                     ymaps.env.server.params.csp=cspValue;
                    */
                });
            });

            describe('sanitize', function () {
                it('should throw error if sanitize token is provided', function () {
                    var template = new Template('<!--sanitize--><div onerror="console.log(\'hello\')"></div>');
                    expect(function () { template.build(); }).to.throwException();
                });

                it('should print warning if sanitize token is not provided', function () {
                    var originalConsoleWarn;
                    var data;
                    if (console) {
                        originalConsoleWarn = console.warn;
                        console.warn = function (message) { data = message; };
                    }
                    var template = new Template('<div onload="console.log(\'world\')"></div>');
                    template.build();
                    expect(function () { template.build(); }).to.not.throwException();
                    if (console) {
                        expect(data).to.not.eql(undefined);
                        console.warn = originalConsoleWarn;
                    }
                });
            });
        });
    });

    provide();
});
