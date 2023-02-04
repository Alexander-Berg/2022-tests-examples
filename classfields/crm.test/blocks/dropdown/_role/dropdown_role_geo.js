var BUTTON_TEXT_DEFAULT = 'Любой регион';

modules.define(
    'dropdown',
    function(provide, dropdown)
    {
        provide(dropdown.decl({modName : 'role', modVal : 'geo'}, {

            emptyVal: '',

            requestUrl: '/ajax/get_geo_suggest/',

            defaultRegions: [
                {
                    add_id: [],
                    id: 87,
                    name: "Москва",
                    region_id: null,
                    type: "region"
                },
                {
                    add_id: [38],
                    id: 87,
                    name: "Москва и область",
                    region_id: null,
                    type: "region"
                },
                {
                    add_id: [],
                    id: 89,
                    name: "Санкт-Петербург",
                    region_id: null,
                    type: "region"
                },
                {
                    add_id: [32],
                    id: 89,
                    name: "Санкт-Петербург и область",
                    region_id: null,
                    type: "region"
                }
            ],

            defaultCacheQuery: '',

            selectedRegions: [],

            cache: {},

            _ac: null,

            _outerPopup: null,

            onSetMod: {
                'js': {
                    'inited': function () {
                        this.__base.apply(this, arguments);
                        this._initRegions();
                        this._initAutocomplete();
                    }
                },

                opened : {
                    'true': function () {
                        this.__base.apply(this, arguments);
                        this._setAutocompleteFocus();
                    }
                }
            },

            getSelectedRegionsId: function() {
                return this.selectedRegions.map(function(item) {
                    return item.id ? item.id: null;
                });
            },

            getOuterPopup: function() {
                if (!this._outerPopup) {
                    this._outerPopup = this.findBlockInside('popup');
                }
                return this._outerPopup;
            },

            _setAutocompleteFocus : function () {
                var popup = this.getOuterPopup();
                var input = popup.findBlockInside('input');

                setTimeout(function () {
                    input.setMod('focused', true);
                }, 200);
            },

            _initRegions: function() {
                var selected = this.params.geo;
                if (selected && selected.length) {
                    this.selectedRegions = selected;
                    this._updateRegions();
                }
            },

            _initAutocomplete: function() {

                var popup = this.getOuterPopup();
                var input = popup.findBlockInside('input');

                var self = this;

                var suggestPopup = popup.findBlockInside('dropdown')
                                        .findBlockInside('popup');

                suggestPopup.setAnchor(input);

                popup.domElem.on('click', '.badge__remove', function(e) {
                    e.preventDefault();
                    self.deleteRegion($(e.currentTarget));
                });

                popup.domElem.jAutocomplete({
                    selector: this,

                    url: this.requestUrl,

                    init: function() {
                        var self = this.options.selector;
                        var data = self.getDefaultRegions();

                        this.result = data;

                        this.setItems(data);

                        this.input.on('click', function() {
                           $(this).trigger('focus');
                        });
                    },

                    html: function(data) {
                        return '<div class="menu-item menu-item_theme_islands i-bem js-control-item" data-bem="{&quot;menu-item&quot;:{&quot;val&quot;:' + data.id + '}}" role="menuitem">' + data.name + '</div>';
                    },

                    input: function() {
                        if (!this.isOpened) {
                            this.open();
                        }
                    },

                    focus: function() {
                        if (!this.isOpened) {
                            var self = this.options.selector;
                            this.result = self.getDefaultRegions();

                            if (this.result.length) {
                                this.setItems(this.result);
                                this.open();
                            }
                        }
                    },

                    hover: function(items, item) {
                        items.not(item).removeClass('menu-item_hovered');
                        item.addClass('menu-item_hovered');
                    },

                    open: function() {
                        if (!suggestPopup.hasMod('visible')) {

                            var input = popup.findBlockInside('input');
                            var inputWidth = $(input.domElem).width();

                            suggestPopup.setMod('visible');
                            $(suggestPopup.domElem).css('min-width', inputWidth + 'px');
                        }
                    },

                    close: function() {
                        if (suggestPopup.hasMod('visible')) {
                            suggestPopup.delMod('visible');
                            this.input.val(self.emptyVal);
                        }
                    },

                    request: function() {
                        this.query = core.util.convertLatToCyr(this.query);
                    },

                    success: function(json) {
                        var self = this.options.selector;
                        var query = this.query;
                        var data;

                        if ('' === query && (!json.data || !json.data.length)) {
                            json.data = self.defaultRegions;
                        }

                        json.data = json.data.slice(0, 8);

                        self.cache[query] = json.data;
                        data = self.filterData(self.cache[query]);

                        if (data.length > self.limit) {
                            data = data.slice(0, self.limit);
                        }

                        this.result = data;
                    },

                    change: function(name, item) {
                        var self = this.options.selector;

                        self.addRegion(item);

                        suggestPopup.delMod('visible');

                        this.input.val(self.emptyVal).trigger('focus');
                        this.close();
                    }
                });
            },

            _updateText: function() {
                var button = this.findBlockInside('button');

                var list = this.selectedRegions.map(function(item) {
                    return item.name;
                });

                var text = '';

                if (!list.length) {
                    text = BUTTON_TEXT_DEFAULT;
                } else if (list.length <= 2) {
                    text = list.join(', ');
                } else {
                    text = list.slice(0, 2).join(', ');

                    var more = list.slice(2).length;
                    if (more > 0) {
                        text += ', ...';
                    }
                }

                button.setText(text);
            },

            _updateRegions: function() {
                var buttonContainer = $(this.getOuterPopup().domElem).find('.geoselector');

                if (!this.selectedRegions.length) {
                    buttonContainer.remove();
                    return;
                }

                if (buttonContainer.length) {
                    buttonContainer.empty();
                } else {
                    buttonContainer = $('<div class="geoselector"/>');
                    $(this.getOuterPopup().domElem).children().first().after(buttonContainer);
                }

                var html = '<div class="button button_theme_islands button_size_s button__control button_role_badge i-bem" data-bem="{&quot;button&quot;:{}}" role="button" type="button"><span class="button__text"></span><span class="badge__remove"></span></div>';

                this.selectedRegions.forEach(function(item) {
                    if (!item.id) {
                        return;
                    }

                    var elem = $(html);

                    elem.find('.button__text').text(item.name);
                    elem.data('region', item);

                    buttonContainer.append(elem);
                });
            },

            _updateControls: function() {
                var citysId = [],
                    regionsId = [],
                    countriesId = [],
                    similarCitiesId = [];

                this.selectedRegions.forEach(function(sel) {
                    if (sel.id > 0) {
                        if (sel.similar) {
                            similarCitiesId.push(sel.id);
                        }
                        if (sel.type == "country") {
                            countriesId.push(sel.id);
                        }
                        if (sel.type == "city") {
                            citysId.push(sel.id);
                        }
                        if (sel.type == "region") {

                            if (sel.add_id instanceof Array && sel.add_id.length) {
                                for (var i = 0; i < sel.add_id.length; i++) {
                                    regionsId.push(sel.add_id[i]);
                                }
                            }

                            if (regionsId.indexOf(sel.id) == -1) {
                                regionsId.push(sel.id);
                            }
                        }
                    }
                });

                var html = '<input type="hidden" id="geo_region" name="search[geo_region]" value="' + regionsId.join(",") + '">' +
                '<input type="hidden" id="geo_city" name="search[geo_city]" value="' + citysId.join(",") + '">' +
                '<input type="hidden" id="geo_country" name="search[geo_country]" value="' + countriesId.join(",") + '">' +
                '<input type="hidden" id="geo_similar_cities" name="search[geo_similar_cities]" value="' + similarCitiesId.join(",") + '">';

                this.elem('control').empty().append(html);
            },

            addRegion: function(region) {
                if (region.id) {

                    if (this.selectedRegions) {
                        if (_.find(this.selectedRegions, function(item) {
                            return (item.id == region.id && item.name == region.name);
                        })) {
                            return;
                        }
                    }
                } else {
                    this.selectedRegions = [];
                }

                this.selectedRegions.push(region);

                this._updateRegions();
                this._updateText();
                this._updateControls();

                var $geo   = $('#geo_region');
                var params = {
                    name: $geo.attr('name'),
                    val:  region.id + '-on'

                };

                $('#filter')
                    .trigger('update', params)
                    .trigger('change');

                this.emit('change');

            },

            deleteRegion: function(elem) {
                var elemButton = elem.closest('.button');
                var region = elemButton.data('region');

                this.selectedRegions = this.selectedRegions.filter(function(item) {
                    return !(item.id == region.id && item.name == region.name);
                });

                this._updateRegions();
                this._updateText();
                this._updateControls();

                var $geo   = $('#geo_region');
                var params = {
                    name: $geo.attr('name'),
                    val:  region.id + '-off'

                };

                $('#filter')
                    .trigger('update', params)
                    .trigger('change');

                this.emit('change');
            },

            filterData: function(data) {
                var selectedRegionsId = [];

                this.selectedRegions.forEach(function(item) {
                    var ids = [item.id];

                    if (item.add_id && item.add_id.length) {
                        ids = ids.concat(item.add_id);
                    }

                    selectedRegionsId.push(ids.join('-'));
                });

                return _.filter(data, function(item) {
                    var result = true;

                    if (item.add_id && item.add_id.length) {
                        var ids = [item.id].concat(item.add_id);

                        result = selectedRegionsId.indexOf(ids.join('-')) == -1;

                        if (result) {
                            result = selectedRegionsId.indexOf(String(item.id)) == -1;

                            if (!result) {
                                result = selectedRegionsId.indexOf(String(item.add_id[0])) == -1;
                            }
                        }

                    } else {
                        result = selectedRegionsId.indexOf(String(item.id)) == -1;
                    }

                    return result;
                });
            },

            getDefaultRegions: function() {
                return this.filterData(this.defaultRegions);
            },

            updateQueryCache: function() {
                this.cache[this.defaultCacheQuery] = this.getDefaultRegions();
            }

        }));
    }
);
