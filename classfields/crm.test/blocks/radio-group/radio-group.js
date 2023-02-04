modules.define(
    'radio-group',
    function(provide, radio)
    {
        provide(radio.decl(this.name, {
            onSetMod: {
                'js': {
                    'inited': function() {
                        this.__base.apply(this, arguments);
                        this._setFocusedRadioChecked();
                    }
                }
            },

            _setFocusedRadioChecked: function() {
                var checkedRadio = this.findBlockInside({
                    block : 'radio',
                    modName : 'checked',
                    modVal : true
                });

                if (checkedRadio) {
                    $(checkedRadio.elem('control')).prop('checked', true);
                }
            }

        }));
    }
);