modules.define(
    'select',
    ['next-tick'],
    function(provide, nextTick, select)
    {
        provide(select.decl({modName : 'type', modVal : 'redirect'}, {

            onSetMod : {
                'js': {
                    'inited': function () {
                        this.__base.apply(this, arguments);
                        this.on('change', this._onChange, this);
                    }
                }
            },

            _onChange : function ()
            {
                var cbResult = {};
		        var params   = this._getCheckedItems()[0].params;

                if (this.hasMod('role', 'sorting')) {
                    this._metric(params);
                }

                $(this.domElem).trigger('change', [ this.getVal(), cbResult ]);

                if (!cbResult.result) {
			var url = params.url;

			nextTick(function() {
				window.location = url;
			});
                }
            },

            _metric: function (params)
            {
                var version = (window.filter_json && filter_json.is_catalog)
                    ? 'sort-c'
                    : 'sort';

                var target = version + '-' + params.val;

                all7MarketingEvent(target);
            }
        }));
    }
);
