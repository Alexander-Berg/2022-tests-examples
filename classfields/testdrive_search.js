$(document).ready(function() {
    var category_alias = 'cars';

    $("#mark").live('change', function() {
        var search_button = $('#search-button');
        var group = $('#group');
        var mark_alias = $(this).val();
        if (mark_alias) {
            $.ajax({
                url: '/ajax/groups/filter/' + category_alias + '/' + mark_alias,
                dataType: 'json',
                success: function(data) {
                    data  = $.parseJSON(data);
                    var str = '<option value="">выберите модель...</option>';
                    for (var i in data) {
                        if (data[i]['label']) {
                            var optgroup = data[i];
                            str += '<optgroup label="'+ optgroup['label'] +'">';
                            var label = optgroup['options'];
                            for (var name in label ) {
                                str += '<option value="'+name+'">'+ label[name] +'</option>'

                            }
                            str += '</optgroup>';
                        } else {
                            str += '<option value="'+i+'">'+ data[i] +'</option>';
                        }
                    }
                    $('#search form').attr('action', '/testdrive/' + category_alias + '/' + mark_alias);
                    group.html(str).removeAttr('disabled');
                    search_button.removeAttr('disabled');

                    if (group.find('option').length > 1) {
                        group.removeAttr('disabled');
                    } else {
                        group.attr('disabled', 'disabled');
                    }
                }
            });
        } else {
            group.html('').attr('disabled', 'disabled');
            search_button.attr('disabled', 'disabled');
        }
    });

    $("#group").live('change', function() {
        var mark_alias = $("#mark").val();
        var group_alias = $(this).val();

        if (group_alias) {
            $('#search form').attr('action', '/testdrive/' + category_alias + '/' + mark_alias + '/' + group_alias);
        }
    });
});