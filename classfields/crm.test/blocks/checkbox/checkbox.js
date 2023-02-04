modules.define('checkbox', ['i-bem__dom', 'jquery', 'dom'], function(provide, BEMDOM, $, dom) {

    provide(BEMDOM.decl(this.name, {

        onSetMod: {
            'checked': function() {
                this.__base.apply(this, arguments);
                this._onSubmit();
            }
        },

        /** @override */
        _onChange : function() {
            this.elem('control').prop('checked', this.getMod('checked'));
        },

        _onPointerPress : function() {
            if(!this.hasMod('disabled')) {
                this.bindToDoc('pointerrelease', this._onPointerRelease);
            }
        },

        _onPointerRelease : function(e) {
            this.unbindFromDoc('pointerrelease', this._onPointerRelease);
            dom.contains(this.domElem, $(e.target)) && this._updateChecked();
        },

        _updateChecked : function() {
            this.toggleMod('checked');
        },

        _onSubmit: function() {

            if (!this.hasMod('ignore')) {
                var control = this.elem('control');
                var val     = (this.getMod('checked'))
                    ? 'on'
                    : 'off';

                $('#filter')
                    .trigger('update', {
                        name : control.attr('name'),
                        val  : val
                    });
            }
        }

    }, {
        live : function() {
            this.liveBindTo('pointerpress', this.prototype._onPointerPress);
            return this.__base.apply(this, arguments);
        }
    }));

});
