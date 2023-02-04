modules.define(
    'select',
    ['i-bem__dom', 'BEMHTML', 'jquery'],
    function(provide, bemDom, bemHtml, $, select)
    {
        provide(select.decl({ modName : 'role', modVal : 'mark' }, {

            _$countryGroup: null,

            onSetMod : {
                js : {
                    inited : function()
                    {
                        this.__base.apply(this, arguments);

                        this._setDefaultText();
                    }
                }
            },

            _setDefaultText : function ()
            {
                if (this._isSelectedFirstItem()) {
                    this._button.setText(this.params.text);
                }
            },

            _getFirstItemVal : function ()
            {
                return this._menu
                    .getItems()[0]
                    .getVal();
            },

            _isSelectedFirstItem : function ()
            {
                return (this._getFirstItemVal() == this.getVal());
            },

            setCountyGroup : function ()
            {
                var markId = this.getVal();

                if (markId < 0) {
                    var countryGroupId = Math.abs(markId);

                    if (!this._$countryGroup) {
                        this._$countryGroup = $('<input>')
                            .attr('type', 'hidden')
                            .val(1);

                        bemDom.after(
                            this.domElem,
                            this._$countryGroup
                        );
                    }

                    this
                        ._$countryGroup
                        .attr('name', 'search[country_group_id][' + countryGroupId + ']');

                } else if (this._$countryGroup) {
                    this._$countryGroup.remove();
                    this._$countryGroup = null;
                }
            },

            getBemjson : function ()
            {
                var mmm          = filter_json.form.fields.mmm[0];
                var firstItemVal = 0;
                var options      = [
                    {
                        group : [
                            {
                                val  : firstItemVal,
                                text : 'Любая'
                            }
                        ]
                    },
                    {
                        group : []
                    }
                ];

                mmm.countries_groups.forEach(function (country) {
                    var text = country.name;
                    var val  = -country.id;

                    if ([-1, -2].indexOf(val) == -1) {
                        text = '&nbsp;&nbsp;&nbsp;&nbsp;' + text;
                    }

                    options[0].group.push({
                        val  : val,
                        text : text
                    })
                });

                mmm.marks.forEach(function (mark) {
                    options[1].group.push({
                        val  : mark.id,
                        text : mark.name
                    })
                });

                return {
                    block : 'select',
                    name  : 'search[mark][]',
                    val   : firstItemVal,
                    js    : {
                        text : 'Марка: любая',
                        placeholder: 'Поиск по марке'
                    },
                    options : options,
                    mods    : {
                        mode  : 'radio',
                        theme : 'islands',
                        size  : 's',
                        width : 'mmm',
                        role  : 'mark',
                        edit  : true
                    },
                    optionsMaxHeight : 292
                };
            },

            _updateButton: function() {
                this.__base.apply(this, arguments);

                if (0 != this.getVal()) {
                    var items = this._getCheckedItems();
                    var text  = items[0].getText().replace(/^(&nbsp;|[\s]+)/g, '');

                    this._button.setText(text);
                }
            }
        }));
    }
);