function printJSON(json, i)
{
    var string = '';
    var div = '';
    for (j = 0; j < i; j++) {
        div += '    ';
    }
    $.each(json, function(key, value) {
        if (value === null || typeof value !== 'object') {
            string += div + '<strong>' + key + '</strong> : ' + (value === null ? '<del>null</del>' : '"<em>' + value + '</em>"') + '<br/>';
        } else {
            string += div + '<strong>' + key + '</strong> :<br/>' + printJSON(value, i + 1);
        }
    });
    return string;
}

(function($){
    $(document).ready(function () {
        $(':input[name=project]').val('');
        $(':input[name=project]').bind('change', function() {
            $(':input[name=controller]').html('<option value=""></option>');
            $(':input[name=method]').html('<option value=""></option>');
            $.get('/ajax/controllers/' + $(this).val(), function (data) {
                $(':input[name=controller]').remove();
                $(':input[name=project]').after(data);
                $(':input[name=controller]').bind('change', function () {
                    $(':input[name=method]').html('<option value=""></option>');
                    $.get('/ajax/methods/' + $(this).val(), function (data) {
                        $(':input[name=method]').remove();
                        $(':input[name=controller]').after(data);
                        $(':input[name=method]').bind('change', function () {
                            if (demo) {
                                var project = $('[name=project] [value=' + $('[name=project]').val() + ']').text();
                                if (demo[project]) {
                                    var controller = $('[name=controller] [value=' + $('[name=controller]').val() + ']').text();
                                    if (demo[project][controller]) {
                                        var method = $('[name=method] [value=' + $('[name=method]').val() + ']').text();
                                        if (demo[project][controller][method] && demo[project][controller][method].params) {
                                            $('[name=params]').val(demo[project][controller][method].params);
                                        }
                                    }
                                }
                            }
                        });
                    });
                });
            });
        });
        $('form[name=api_form]').submit(function () {
            var url = $(this).find('[name=api_url]').val() + $(this).find('[name=interface]').val() + '/';
            var query = {};
            query.key     = $(this).find('[name=key_login]').val();
            query.format  = $(this).find('[name=format]').val();
            query.version = $(this).find('[name=version]').val();
            query.method  = $(this).find('[name=project] [value=' + $(this).find('[name=project]').val() + ']').text()
                + '.' + $(this).find('[name=controller] [value=' + $(this).find('[name=controller]').val() + ']').text()
                + '.' + $(this).find('[name=method] [value=' + $(this).find('[name=method]').val() + ']').text();
            if ($(this).find('[name=sid]').val() != '') {
                query.sid = $(this).find('[name=sid]').val();
            }
            var params = $(this).find('[name=params]').val().split(/&+/g);
            for (var i = 0; i < params.length; i++) {
                var param = params[i].split(/=/g);
                if (param[0] && param[1]) {
                    query[param[0]] = param[1];
                }
            }
            /* подпись */
            var query_secret = '';
            query_secret = jQuery.param(query); // http_build_query
            query_secret = query_secret.replace(/&/g, '');
            query.sig = Sha256.hash(query_secret + $(this).find('[name=key_secret]').val());
            $.ajax({
                type : 'post',
                url : url,
                dataType : 'json',
                data : query,
                success : function (data) {
                    if (data && data.result && data.result.sid) {
                        $(':input[name=sid]').val(data.result.sid);
                    }
                    $('pre').html(printJSON(data, 0));
                }
            });
            return false;
        });
    });
})(jQuery);