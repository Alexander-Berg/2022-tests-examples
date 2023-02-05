/**
 * @fileOverview
 * Noticed copy-pasting controls for state and options from stand to stand over and over.
 */
(function(currentScript) {
ymaps.modules.define('flapenguin.TestStandControls', [
    'system.browser',
    'util.array',
    'util.throttle',
    'Monitor'
], function (provide, browser, array, throttle, Monitor) {
    // Append CSS.
    var link = document.createElement('link');
    link.rel = 'stylesheet';
    link.type = 'text/css';
    link.href = currentScript.src.replace(/\.js$/, '.css');
    document.head.appendChild(link);

    // Helpers.
    function nop() {}
    function error(text) { alert(text); throw new Error(text); }
    function preventDefault(e) { e && (e.preventDefault ? e.preventDefault() : (e.returnValue = false)); }
    function arrayFlatten(arr) {
        return array.reduce(arr, function (flat, x) {
            return flat.concat(array.isArray(x) ? arrayFlatten(x) : x);
        }, []);
    }

    var arrayFrom = Array.from || function arrayFrom(source) {
        var result = new Array(source.length);
        for (var i = 0, l = source.length; i < l; i++) {
            result[i] = source[i];
        }

        return result;
    };

    // Search parameters.
    var searchParams = TestStandControls.searchParams = new function SearchParams() {
        var params = location.search.replace(/^\?/, '').split('&');
        params = array.filter(params, function (x) { return !!x; });
        params = array.map(params, function (x) { return array.map(x.split('='), decodeURIComponent); });
        params = array.reduce(params, function (m, x) { m[x[0]] = x[1]; return m; }, {});
        this._params = params;

        this.toString = function () {
            var s = [];
            for (var name in params) {
                if (params[name]) {
                    s.push(encodeURIComponent(name) + '=' + encodeURIComponent(params[name]));
                }
            }

            return '?' + s.join('&');
        };

        this.set = function (key, value) {
            if (params[key] !== value) {
                params[key] = value;
                window.location.search = this.toString();
            }
        };

        this.get = function (key, type) {
            type = type || 'str';

            var value = params[key];
            if (value === undefined || /^(json\??|str|enum\??)$/.test(type)) {
                return value;
            }

            if (/^bool\??$/.test(type)) {
                return value === 'true' ? true :
                    value === 'false' ? false :
                    type === 'bool?' && value === 'undefined' ? undefined :
                    error('Expected true/false' + (type === 'bool?' ? '/undefined' : '') + ' in query string: ' + key);
            }
            if (/^(hash|list)\??$/.test(type)) {
                return JSON.parse(value);
            }
            if (type === 'number') {
                return parseInt(value, 10);
            }
            error('Unknown type for query string: ' + type);
        };
    }();

    // Discount hyperscript.
    TestStandControls.hyperscript = h;
    function h(name, attrs, children) {
        var el = document.createElement(name);
        for (var key in attrs) {
            if (key in el) {
                el[key] = attrs[key];
            } else {
                el.setAttribute(key, attrs[key]);
            }
        }

        array.each(arrayFlatten(arrayFrom(arguments).slice(2)), function (child) {
            if (child !== undefined) {
                el.appendChild(typeof child === 'string' ? document.createTextNode(child) : child);
            }
        });

        return el;
    }

    // Monitor helpers.
    var DUMMY = {dummy:1};
    function getNative(manager, name, value) {
        if (manager.getNative) {
            value = manager.getNative(name);
            value = value === undefined ? DUMMY : value;
        }
        return value;
    }

    /**
     * @ignore
     * @param {String} [config.className]
     * @param {String|HTMLElement} config.node
     * @param {Array<Group>} groups Group is
     *  interface Group {
     *      label: string;                          // Label to show.
     *      manager?: IDataManager|IOptionManager;  // Manager to use.
     *      className?: string;                     // Custom class name.
     *      fields: {
     *          type: 'bool'|'bool?'|'enum'|'enum?'|'str';
     *          name: string;                       // Name of option in manager.
     *          className?: string;                 // Custom class name.
     *          initial?: any;                      // Initial value.
     *          values?: string[];                  // List of available values for 'enum' type.
     *          hint?: string;                      // Hint to show near input.
     *          static?: string;                    // Name of query-string parameters for field.
     *          onchange?: () => void;              // Custom onchange.
     *      }[];
     *  };
     */
    function TestStandControls(config, groups) {
        if (typeof config.node === 'string') {
            config.node = document.getElementById(config.node);
        }

        var nullManager = { set: nop, unset: nop };
        var nullMonitor = { add: function () {}, destroy: function() {} };

        groups = array.map(groups, function (group) {
            var manager = group.manager || nullManager;
            var monitor = group.manager ? new Monitor(group.manager) : nullMonitor;
            var staticManager = {
                set: function (key, value) {
                    manager.set(key, value);
                    searchParams.set(key, value);
                },
                unset: function (key) {
                    manager.unset(key);
                    searchParams.set(key, undefined);
                }
            };

            var fields = array.map(group.fields, function (field) {
                if (!field) return;

                field.staticValue = field['static'] ? searchParams.get(field['static']) : undefined;

                var staticInitial = searchParams.get(field.initialStatic || field.name, field.type);
                field.initial = staticInitial === undefined ? field.initial : staticInitial;

                return field['static']
                    ? createField(field, group, staticManager, nullMonitor)
                    : createField(field, group, manager, monitor);
            });

            return h('div', { 'class': 'test-stand-controls__group ' + (group.className || '') },
                h('div', { 'class': 'test-stand-controls__group-title' }, group.label),
                fields);
        });

        var element = h('div', { 'class': 'test-stand-controls ' + (config.className || '') }, groups);
        config.node.appendChild(element);
    }

    function createField(field, group, manager, monitor) {
        var special = createField.specials[field.type];
        if (special) {
            return special.apply(null, arguments);
        }

        var control = createField.creators[field.type].apply(null, arguments);
        var container = control;
        if (array.isArray(control)) {
            container = control[1];
            control = control[0];
        }

        if (browser.isIE) {
            var fixer = createField.ieFixers[field.type];
            if (fixer) {
                fixer(control);
            }
        }

        control.onchange();

        return h('div', { 'class': 'test-stand-controls__field ' + (field.className || '') },
            h('label', { 'class': 'test-stand-controls__label' },
                (field.label || field.name) + ': ', container),
            !field.hint ? undefined
                : h('span', { 'class': 'test-stand-controls__hint', title: field.hint }, '(?)'));
    }

    createField.specials = {
        'action': function (field, group, manager, monitor) {
            return h('div', { 'class': 'test-stand-controls__field ' + (field.className || '') },
                h('button', { 'class': 'test-stand-controls__field-action', onclick: field.onclick },
                    field.label),
                !field.hint ? undefined
                    : h('span', { 'class': 'test-stand-controls__hint', title: field.hint }, '(?)')
            );
        },

        'mapsize': function (field, group, manager, monitor) {
            var min = field.min || [100, 100];
            var max = field.max || [1366, 768];
            var fieldInitial = field.initial || [];
            var initial = searchParams.get('mapsize', 'list') || fieldInitial;
            initial[0] = initial[0] || fieldInitial[0] || 640;
            initial[1] = initial[1] || fieldInitial[1] || 480;

            var width = h('input', { type: 'range', min: min[0], max: max[0] });
            var height = h('input', { type: 'range', min: min[1], max: max[1] });
            var widthText = h('span');
            var heightText = h('span');
            var map = field.map;
            var event = 'oninput' in width ? 'oninput' : 'onchange';

            function refresh() {
                var p = map.container.getParentElement();
                if (width.value) {
                    widthText.textContent = p.style.width = width.value + 'px';
                }
                if (height.value) {
                    heightText.textContent = p.style.height = height.value + 'px';
                }
                map.container.fitToViewport();
            }

            width.oninput = width.onchange = height.oninput = height.onchange = throttle(15, refresh);
            width.oncontextmenu = function (e) { width.value = initial[0]; refresh(); preventDefault(e); };
            height.oncontextmenu = function (e) { height.value = initial[1]; refresh(); preventDefault(e); };

            width.onkeydown = height.onkeydown = function (e) {
                e = e || window.event;
                var code = window.event.keyCode || e.which;
                if (code === 13) {
                    refresh();
                    return;
                }

                if (!e.shiftKey) {
                    return;
                }
                if (code === 37) {
                    this.value = +this.value - 9;
                }
                if (code === 39) {
                    this.value = +this.value + 9;
                }
            };

            width.oncontextmenu();
            height.oncontextmenu();
            refresh();

            return [
                h('div', { 'class': 'test-stand-controls__field ' + (field.className || '') },
                    h('label', {}, 'width: ', width, widthText)),
                h('div', { 'class': 'test-stand-controls__field ' + (field.className || '') },
                    h('label', {}, 'height: ', height, heightText))
            ];
        },

        'custom': function (field, group, manager, monitor) {
            var node = h('div');
            field.ref(node);

            return h('div', { 'class': field.className || '' },
                !field.name ? undefined : h('label', {}, field.name + ': '),
                node
            );
        },

        '-': function () {
            return h('hr');
        },

        'last-event': function (field, group, manager, monitor) {
            var state = h('input', { disabled: true, type: 'text', value: '-------' });
            field.eventManager.add(field.events, function (e) {
                state.value = e.get('type');
            });

            return h('label', {}, field.label + ': ', state);
        }
    };

    createField.creators = {
        'enum': function (field, group, manager, monitor) {
            var options = /\?$/.test(field.type) ? [h('option', { value: '' })] : [];
            options = options.concat(array.map(field.values, function(value) {
                return h('option', { value: value }, value);
            }));

            var element = h('select', { 'class': 'test-stand-controls__field-enum' }, options);
            element.value = field.staticValue || field.initial || '';

            element.onchange = field.onchange || function () {
                if (element.value) {
                    manager.set(field.name, element.value);
                } else {
                    manager.unset(field.name);
                }
            };

            monitor.add(field.name, function (value) {
                value = getNative(manager, field.name, value);
                element.value = value;
            }, null, { defaultValue: '' });

            return element;
        },

        'number': function (field, group, manager, monitor) {
            var format = field.format || function(x) { return x; };
            var unformat = field.unformat || function(x) { return x; };

            var element = input('number', field, format(field.staticValue || field.initial || ''));
            element.min = field.min;
            element.max = field.max;
            element.step = field.step;

            element.onchange = field.onchange || function () {
                if (element.value) {
                    manager.set(field.name, unformat(element.value));
                } else {
                    manager.unset(field.name);
                }
            };

            monitor.add(field.name, function (value) {
                value = getNative(manager, field.name, value);
                element.value = value === DUMMY ? '' : format(value);
            }, null, { defaultValue: '' });

            return element;
        },

        'str': function (field, group, manager, monitor) {
            var element = input('text', field, field.staticValue || field.initial || '');

            element.onchange = field.onchange || function () {
                if (element.value) {
                    manager.set(field.name, element.value);
                } else {
                    manager.unset(field.name);
                }
            };

            monitor.add(field.name, function (value) {
                value = getNative(manager, field.name, value);
                element.value = value;
            }, null, { defaultValue: '' });

            return element;
        },

        'bool': function (field, group, manager, monitor) {
            var element = h('input', { 'class': 'test-stand-controls__field-bool', type: 'checkbox' });
            element.checked = field.initial !== undefined ? field.initial : false;

            element.onchange = field.onchange || function () {
                if (element.indeterminate) {
                    manager.unset(field.name);
                } else {
                    manager.set(field.name, element.checked);
                }
            };

            if (/\?$/.test(field.type)) {
                element.indeterminate = field.initial === undefined;
                element.oncontextmenu = function (e) {
                    element.checked = false;
                    element.indeterminate = true;
                    element.onchange();
                    preventDefault(e);
                };

                monitor.add(field.name, function (value) {
                    value = getNative(manager, field.name, value);

                    if (value === DUMMY) {
                        element.oncontextmenu();
                    } else {
                        element.checked = value;
                        element.indeterminate = false;
                    }
                }, null, { defaultValue: DUMMY });
            }
            else  {
                monitor.add(field.name, function (value) {
                    element.checked = getNative(manager, field.name, value);
                });
            }

            if (field['static']) {
                element.checked = field.staticValue === 'true' ? true : false;
                element.indeterminate = !/^(true|false)$/.test(field.staticValue);
            }

            return element;
        },

        'hash': function (field, group, manager, monitor) {
            var isOptional = /\?$/.test(field.type);

            var undefinedOption = h('option', { value: '__UNDEFINED__' }, 'undefined');
            var options = array.map(field.values, function(value) {
                return h('option', { value: value }, value);
            });

            var allOptions = isOptional ? [].concat([undefinedOption], options) : options;

            var element = h('select',
                { 'class': 'test-stand-controls__field-hash', multiple: true, size: allOptions.length },
                allOptions);

            var jsonEl = h('span', {'class': 'test-stand-controls__field-hash-json'}, '(json)')

            element.onchange = function() {
                if (undefinedOption.selected) {
                    element.value = '__UNDEFINED__';
                    manager.unset(field.name);
                } else {
                    manager.set(field.name, array.reduce(options, function(m, o) {
                        m[o.value] = o.selected;
                        return m;
                    }, {}));
                }

                jsonEl.title = JSON.stringify(manager.get(field.name));
            };

            monitor.add(field.name, function(value) {
                value = getNative(manager, field.name, value);
                if (value === DUMMY) {
                    element.value = '__UNDEFINED__';
                } else {
                    array.each(options, function(opt) { opt.selected = value[opt.value]; });
                }
            });

            var initial = field.initial || (isOptional ? undefined : {});
            if (initial === undefined) {
                element.value = '__UNDEFINED__';
            } else {
                array.each(options, function(opt) {
                    opt.selected = initial[opt.value];
                });
            }

            return [element, h('span', {}, element, jsonEl)];
        },

        'json': function (field, group, manager, monitor) {
            var element = input('text', field, field.staticValue || field.initial || '');

            element.onchange = field.onchange || function () {
                if (element.value) {
                    manager.set(field.name, JSON.parse(element.value));
                } else {
                    manager.unset(field.name);
                }
            };

            monitor.add(field.name, function (value) {
                value = getNative(manager, field.name, value);
                element.value = JSON.stringify(value);
            }, null, { defaultValue: '' });

            return element;
        }
    };

    createField.creators['enum?'] = createField.creators['enum'];
    createField.creators['bool?'] = createField.creators['bool'];
    createField.creators['hash?'] = createField.creators['hash'];
    createField.creators['json?'] = createField.creators['json'];

    createField.ieFixers = {
        'bool': function (input) {
            input.onclick = input.onchange;
        },
        'str': function (input) {
            input.onkeydown = function (e) {
                if ((window.event.keyCode || e.which) === 13) {
                    input.onchange();
                }
            };
        }
    };

    createField.ieFixers['bool?'] = createField.ieFixers['bool'];

    function input(type, field, value) {
        return h('input', {
            type: type,
            value: value,
            'class': 'test-stand-controls__field-' + field.type,
            placeholder: field.placeholder || ''
        });
    }

    provide(TestStandControls);
});
})(document.currentScript);
