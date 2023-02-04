modules.define(
    'select',
    function(provide, select)
    {
        provide(select.decl({modName : 'mode', modVal : 'radio'}, {

            _isResetItemClick: false,

            onSetMod : {
                'js': {
                    'inited': function () {
                        this.__base.apply(this, arguments);

                        var clearOption = this.findBlockInside({
                            block: 'menu-item', modName: 'role', modVal: 'reset'
                        });

                        if (clearOption) {
                            clearOption.on('click', this._resetItemClick, this);
                        }
                    }
                }
            },

            _resetItemClick: function() {
                this._isResetItemClick = true;
            },

            _updateButton : function() {

                // Когда выбран пункт "Сбросить", то выбранных элементов нет.
                if (this._getCheckedItems().length) {
                    this.__base.apply(this, arguments);
                }

                if (this.params.text
                    && (this._isResetItemClick || !this.getVal())
                ) {
                    var text = this.params.text;

                    if (this.params.label) {
                        text += ', ' + this.params.label;
                    }

                    this._button.setText(text);
                }

                var defaultVal = this.params['default_val'] || 0;

                if (this._isResetItemClick || defaultVal == this.getVal()) {
                    this._button.delMod('checked');
                } else {
                    this._button.setMod('checked', true);
                }

                this._isResetItemClick = false;
            },

            _updateControl : function() {
                var val = this.getVal(),
                    control = this.elem('control');

                if(!control.length) {
                    control = $(select._createControlHTML(this.getName(), val));
                    this.dropElemCache('control');
                }

                if(typeof val === 'undefined') {
                    control.remove();
                } else {
                    control.parent().length || this.domElem.prepend(control);
                    control.val(val);
                }
            }
        }));
    }
);