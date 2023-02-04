modules.define(
    'select',
    ['keyboard__codes'],
    function(provide, keyCodes, select)
    {
        provide(select.decl(this.name,
        {
            _selectedIndex: 0,

            _previousIndex: -1,

            _control: null,

            _clickItem: null,

            onSetMod: {
                'js': {
                    'inited': function() {
                        this.__base.apply(this, arguments);
                        this._menu.on('change', this._onSubmit, this);
                        this._menu.on('item-click', this._onSelectItem, this);
                    }
                },
                'opened': {
                    '': function() {
                        this.__base.apply(this, arguments);
                        this._updateSelectedIndex();
                    }
                },
                'focused': {
                    '': function() {
                        this.__base.apply(this, arguments);
                        this._updateSelectedIndex();
                    }
                }
                //если нужно передавать значения задисейбленых селектов
                /*'disabled' : {
                    '*' : function(modName, modVal) {
                        this._button.setMod(modName, modVal);
                        this._menu.setMod(modName, modVal);
                    }
                }*/
            },

            _onSelectItem: function(e, params) {
                this._clickItem = params.item;

                this._updateSelectedIndex();
            },

            _updateSelectedIndex: function(index) {
                index = index || 0;
                if (!index) {
                    var items = this._menu.getItems();
                    var checkedItems = items.filter(function(item) {
                        return item.hasMod('checked');
                    });

                    if (checkedItems.length) {

                        var firstChecked = items.indexOf(checkedItems[0]);

                        if (firstChecked > -1) {
                            index = firstChecked;
                        }
                    }
                }
                this._selectedIndex = this._previousIndex = index;
            },

            _onSubmit: function() {

                if (!this.hasMod('ignore')) {
                    var item = this._clickItem;

                    if (!item) {
                        item = this;
                    }

                    var val = item.getVal();

                    if (this.hasMod('mode', 'check')) {
                        val += '-';
                        val += (item.getMod('checked'))
                            ? 'on'
                            : 'off';
                    }

                    var params = {
                        name : this.params.name,
                        val  : val
                    };

                    $('#filter')
                        .trigger('update', params)
                        .trigger('change');
                }
            },

            _onKeyDown : function(e) {

                var validCode = !e.shiftKey && (e.keyCode === keyCodes.UP || e.keyCode === keyCodes.DOWN || e.keyCode === keyCodes.ENTER);

                if (validCode && this.hasMod('opened')) {
                    e.preventDefault();
                    e.stopPropagation();

                    var items = this._menu.getItems(),
                        len = items.length;

                    if (e.keyCode === keyCodes.UP) {
                        if (this._selectedIndex > 0) {
                            items[--this._selectedIndex].setMod('hovered');
                        }
                    } else if (e.keyCode === keyCodes.DOWN) {

                        if (this._selectedIndex < len - 1) {
                            items[++this._selectedIndex].setMod('hovered');
                        }
                    } else if (e.keyCode === keyCodes.ENTER) {

                        var currentItem = items[this._selectedIndex];
                        currentItem.emit('click', { item : currentItem, source : 'keyboard' });

                        this._updateSelectedIndex();
                    }
                } else {
                    this.__base.apply(this, arguments);
                }
            }
        }));
    }
);