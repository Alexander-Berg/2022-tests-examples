modules.define(
    'button',
    function(provide, button)
    {
        provide(button.decl(this.name, {
            onSetMod: {
                'js': {
                    'inited': function() {
                        this.__base.apply(this, arguments);
                        this.on('click', this._toggleFilter, this);
                    }
                }
            },

            _toggleFilter: function(e) {

            },

            _onKeyDown : function(e) {
                e.stopImmediatePropagation();
                this.__base.apply(this, arguments);
            }
        }));
    }
);
