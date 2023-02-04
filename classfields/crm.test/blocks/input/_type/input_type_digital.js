modules.define(
    'input',
    function(provide, input)
    {
        provide(input.decl({modName: 'type', modVal: 'digital'}, {
            onSetMod: {
                'js': {
                    'inited': function () {
                        this.__base.apply(this, arguments);

                        this.on('change', function() {
                            if (!this.hasMod('focused')) {
                                return;
                            }
                            this._formatInput();
                        });

                        this._addDimensions();
                    }
                },
                'focused': {
                    'true': function() {
                        this._formatInput();
                    },
                    '': function() {
                        this._formatInput();
                        this._addDimensions();
                    }
                }
            },

            _addDimensions: function() {
                var val = this.getVal();
                if (val && this.params.label) {
                    val += ' ' + this.params.label;
                }
                this.setVal(val);
            },

            _formatInput: function () {

                var val = String(this.getVal()) .replace(/[^0-9\.\,]+/g, '')
                                                .replace(',', '.')
                                                .replace(/\.+/, '.');

                if (!val) {
                    this.setVal(val);
                    return;
                }

                if (this.params.fractional) {
                    var decPos = val.indexOf('.');
                    var dec;

                    if (decPos > -1) {
                        dec = val.substring(decPos + 1).replace('.', '');
                        val = val.substring(0, decPos);
                    }

                    val = this._format(val, 0, '', ' ');

                    if (undefined !== dec) {
                        val += '.' + dec;
                    }
                } else {
                    val = this._format(val, 0, '', ' ');
                }

                this.setVal(val);
            },

            _format: function (n, c, d, t) {
                var c = isNaN(c = Math.abs(c)) ? 2 : c,
                    d = d == undefined ? "." : d,
                    t = t == undefined ? "," : t,
                    s = n < 0 ? "-" : "",
                    i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "",
                    j = (j = i.length) > 3 ? j % 3 : 0;
                return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
            }
        }));
    }
);
