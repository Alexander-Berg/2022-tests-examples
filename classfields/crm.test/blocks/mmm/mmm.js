var CHANNEL_NAMESPACE = 'mmm-item';

modules.define(
    'mmm',
    ['i-bem__dom', 'BEMHTML', 'events__channels'],
    function(provide, bemDom, bemHtml, channels)
    {
        provide(bemDom.decl(this.name,
        {
            _numItems : 0,

            onSetMod : {
                js : {
                    inited : function()
                    {
                        this.__base.apply(this, arguments);

                        channels(CHANNEL_NAMESPACE)
                            .on('inited', this._onItemInited, this)
                            .on('add', this._onItemAdd, this)
                            .on('delete', this._onItemDelete, this);
                    }
                }
            },

            _onItemInited : function ()
            {
                this._numItems++;
            },

            _onItemAdd : function ()
            {
                var version = (filter_json.is_catalog)
                    ? 'c_v1'
                    : 'v1';

                // Метрика
                all7MarketingFilterSet(version, 'mark_other_plus');

                var mmmItemBlock = this.findBlockInside('mmm-item');
                var mmmItemHtml  = bemHtml.apply(
                    mmmItemBlock.getBemjson()
                );

                bemDom.append(
                    this.domElem,
                    mmmItemHtml
                );

                if (this._numItems > 1) {
                    mmmItemBlock
                        .findBlockInside({
                            block   : 'button',
                            modName : 'role',
                            modVal  : 'delete'
                        })
                        .delMod('hidden')
                }
            },

            _onItemDelete : function ()
            {
                this._numItems--;

                var firstMmmItemBlock = this.findBlockInside('mmm-item');

                if (1 == this._numItems) {
                    firstMmmItemBlock
                        .findBlockInside({
                            block   : 'button',
                            modName : 'role',
                            modVal  : 'delete'
                        })
                        .setMod('hidden');
                }

                firstMmmItemBlock
                    .findBlockInside({
                        block   : 'button',
                        modName : 'role',
                        modVal  : 'add'
                    })
                    .delMod('hidden');
            }
        }));
    }
);
