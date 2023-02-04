modules.define(
    'select',
    ['tick', 'idle', 'keyboard__codes'],
    function(provide, tick, idle, keyCodes, select)
    {
        provide(select.decl({modName : 'edit'}, {

            _hidePopupOnBlur: true,

            translateKeyboard: [
                'йцукенгшщзфывапролдячсмить',
                'qwertyuiopasdfghjklzxcvbnm'
            ],

            translateMap: {
                'бмв'       : 'bmw',
                'ауди'      : 'audi',
                'тайота'    : 'toyota',
                'тойота'    : 'toyota',
                'форд'      : 'ford',
                'опель'     : 'opel',
                'ниссан'    : 'nissan',
                'хундай'    : 'hyundai',
                'хундаи'    : 'hyundai',
                'хонда'     : 'honda',
                'киа'       : 'kia'
            },

            inputPlaceholder: 'Введите название',

            onSetMod : {
                js : {
                    inited : function()
                    {
                        this.__base.apply(this, arguments);

                        this._inputFocused = false;
                        this._checkedItems = this._getCheckedItems();

                        this._initInput();

                        var ie = this._getIeVersion();

                        if (ie) {
                            this.delMod('edit');
                        }
                    }
                },

                opened: {
                    'true': function() {
                        this.__base.apply(this, arguments);
                        this.enableInput();
                    },
                    '': function() {
                        this.__base.apply(this, arguments);
                        this.disableInput();
                    }
                },

                'disabled': {
                    '': function() {
                        this.delMod('focused');
                    }
                }
            },

            _getIeVersion: function () {
                var ua = navigator.userAgent;
                var re;
                var result;

                if (navigator.appName == 'Microsoft Internet Explorer') {
                    re = new RegExp('MSIE ([0-9]{1,}[\\.0-9]{0,})');

                    if (re.exec(ua) != null) {
                        result = parseFloat(RegExp.$1);
                    }
                } else if (navigator.appName == 'Netscape') {
                    re  = new RegExp('Trident/.*rv:([0-9]{1,}[\\.0-9]{0,})');

                    if (re.exec(ua) != null) {
                        result = parseFloat(RegExp.$1);
                    }
                }
                return result;
            },

            _updateButton: function() {
                if (this._inputFocused) {
                    return;
                }

                var checkedItems = this._getCheckedItems();
                if (checkedItems.length) {
                    this._previousText = checkedItems[0].getText();
                    this.__base.apply(this, arguments);
                } else {
                    var checkedItem = this._checkedItems.length ? this._checkedItems[0] : this._defaultItems[0];
                    this._previousText = checkedItem.getText();
                    this._button.setText(this._previousText);
                }
            },

            _initInput: function() {
                var button = this.findBlockInside('button');
                var checkedItems = this._getCheckedItems();
                var placeholder = this.params.placeholder || this.inputPlaceholder;

                this._defaultItems = this._menu.getItems();
                this._previousText = checkedItems.length ? checkedItems[0].getText() : '';
                this._input = $('<input class="select__input" placeholder="' + placeholder + '" />');

                this.domElem.css('position', 'relative');
                this._input.css('display', 'none');

                this.bindTo(this._input, 'keydown', this._onInputKeyDown)
                    .bindTo(this._input, 'blur', function() {
                        this.delMod('opened');
                    });

                this.domElem.append(this._input);
            },

            _onInputKeyDown: function(e) {
                if (e.keyCode === keyCodes.ENTER) {
                    e.preventDefault();
                    e.stopPropagation();

                } else if (e.keyCode === keyCodes.ESC) {
                    this.delMod('opened');
                    this.setMod('focused');
                }
            },

            updateByText: function(text) {
                var self = this;
                var selected = 0;

                this._defaultItems.forEach(function(item) {
                    if (!text || self.isValidText(item.getText(), text)) {
                        if (text && !item.getVal()) {
                            item.domElem.hide();
                        } else {
                            item.domElem.show();
                        }
                        selected++;
                    } else {
                        item.domElem.hide();
                    }
                });

                if (!selected) {
                    this.updateByText('');
                } else {
                    this._menu.elem('group').each(function(i, item) {
                        var elem = $(this);
                        if (elem.height() < 10) {
                            elem.hide();
                        } else {
                            elem.show();
                        }
                    });
                }

                $(window).trigger('scroll');
            },

            _updateItems: function() {
                var text = this._input.val();

                if (text == this._previousText) {
                    return;
                }

                this.updateByText(text);
                this._previousText = text;
            },

            enableInput: function() {
                var checkedItems = this._getCheckedItems();
                var checked = checkedItems.length && (checkedItems[0].getVal() != this._defaultItems[0].getVal());
                var text = checked ? this.clearText( checkedItems[0].getText() ) : '';

                tick.on('tick', this._updateItems, this).start();

                this.findBlockInside('button').setText('');
                this.updateByText('');

                this._input.css('display', 'inline-block');
                this._input.val(text);
                this._hidePopupOnBlur = false;
                this._input.focus();

                this._inputFocused = true;
                this._previousText = text;
            },

            _blur : function() {

                if (this._hidePopupOnBlur) {
                    this.__base.apply(this, arguments);
                } else {
                    this._hidePopupOnBlur = true;
                }
            },

            disableInput: function() {
                this._input.css('display', 'none');
                this._input.val('');
                this._input.blur();
                this._inputFocused = false;

                tick.un('tick', this._updateItems, this);

                this._updateButton();
            },

            clearText: function(text) {
                return text.replace(/&nbsp;/, '');
            },

            isValidText: function(a, b) {
                a = this.clearText(a).toLowerCase().replace(/[\s]+/, '');
                b = this.clearText(b).toLowerCase().replace(/[\s]+/, '');

                return a.indexOf(b) === 0 || a.indexOf(this.translateToEnglish(b)) === 0 || a.indexOf(this.translateFromMap(b)) === 0;
            },

            translateToEnglish: function(t) {
                t = t.toLowerCase();

                var ru = this.translateKeyboard[0].split('');
                var en = this.translateKeyboard[1].split('');
                var k;

                for (var i = 0; i < t.length; i++) {
                    k = ru.indexOf(t[i]);
                    if (k > -1) {
                        t = t.replace(ru[k], en[k]);
                    }
                }

                return t;
            },

            translateFromMap: function(t) {
                return this.translateMap[t.toLowerCase()] || t;
            }

        }));
    }
);