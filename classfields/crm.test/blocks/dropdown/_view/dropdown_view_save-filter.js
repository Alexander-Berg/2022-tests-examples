modules.define(
    'dropdown',
    function(provide, dropdown)
    {
        provide(dropdown.decl({modName : 'view', modVal : 'save-filter'}, {
                onSetMod: {
                    'js': {
                        'inited': function () {
                            this.__base.apply(this, arguments);

                            var popup = this.findBlockInside('popup');
                            var button = popup.findBlockInside('button');
                            var input = popup.findBlockInside('input');
                            var self = this;

                            button.on('click', function() {
                                if (input.getVal()) {
                                    self.delMod('opened');

                                    // Метрика
                                    all7MarketingEvent('save_search_submit');
                                } else {
                                    input.setMod('focused');
                                }
                            });
                        }
                    },

                    'opened': {
                        'true': function () {

                            var version = (filter_json.is_catalog)
                                ? 'c_v1'
                                : 'v1';

                            // Метрика
                            all7MarketingFilterSet(version, 'save_search_button');
                        }
                    }
                }
            }
        ));
    }
);