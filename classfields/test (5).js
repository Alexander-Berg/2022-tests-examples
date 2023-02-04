function printJSON(json, i)
{
    var string = '';
    var div = '';
    for (j = 0; j < i; j++) {
        div += '    ';
    }
    $.each(json, function(key, value) {
        if (value === null || typeof value !== 'object') {
            string += div + key + ' : ' + (value === null ? 'null' : '"' + value + '"') + ',<br/>';
        } else {
            var type = 'object';
            if (typeof value.length !== 'undefined') {
                type = 'array';
            }
            string += div + key + ' : ' + (type == 'array' ? '[' : '{') + '<br/>' + printJSON(value, i + 1) + div + (type == 'array' ? ']' : '}') + ',<br/>';
        }
    });
    return string;
}

(function($) {
    $(document).ready(function() {
        var $resultContainer = $('#api-response');
        $(':input[name=project]').val('');
        $(':input[name=project]').bind('change', function() {
            $(':input[name=controller]').html('<option value=""></option>');
            $(':input[name=method]').html('<option value=""></option>');
            $.get('/ajax/controllers/' + $(this).val() + '/', function(data) {
                $(':input[name=controller]').remove();
                $(':input[name=project]').after(data);
                $(':input[name=controller]').bind('change', function() {
                    $(':input[name=method]').html('<option value=""></option>');
                    $.get('/ajax/methods/' + $(this).val() + '/', function(data) {
                        $(':input[name=method]').remove();
                        $(':input[name=controller]').after(data);
                        $(':input[name=method]').bind('change', function() {
                            if (demo) {
                                var project = $('[name=project] [value=' + $('[name=project]').val() + ']').text();
                                if (demo[project]) {
                                    var controller = $('[name=controller] [value=' + $('[name=controller]').val() + ']').text();
                                    if (demo[project][controller]) {
                                        var method = $('[name=method] [value=' + $('[name=method]').val() + ']').text();
                                        if (demo[project][controller][method]) {
                                            $('[name=params]').val(demo[project][controller][method]);
                                        }
                                    }
                                }
                            }
                        });
                    });
                });
            });
        });

        var $methodType = $(document.getElementById('method_type'));
        var $tmplBlock = $(document.getElementById('file_row_tmpl'));
        var $rowsContainer = $(document.getElementById('file_rows'));

        $(document).on('keyup keypress paste cut', 'input:text.file_field_name', function() {
            var val = jQuery.trim($(this).val());

            var classSwitch = val === 'files' || val === 'files[]';
            $(this).toggleClass('errorInput', classSwitch);
        });

        var query;
        var formData;

        /**
         * Добавляет поля в запрос
         *
         * @param {string} name
         * @param {mixed}  value
         * @param {object} level
         */
        function addFieldForRequest(name, value, level)
        {
            if (level == undefined) {
                query[name] = value;
            } else {
                level[name] = value; // Ссылка на объект
            }

            formData.append(name, value);
        }

        /**
         * Сбрасываем стили для контейнера ответа API если нет ошибок
         */
        function resetResponseContainerStyle()
        {
            $resultContainer.css('background-color', '');
        }

        $('form[name=api_form]').submit(function() {
            var url = $(this).find('[name=api_url]').val() + $(this).find('[name=interface]').val() + '/';

            formData = new FormData();
            query = {};

            addFieldForRequest('key', $(this).find('[name=key_login]').val());
            addFieldForRequest('format', $(this).find('[name=format]').val());
            addFieldForRequest('version', $(this).find('[name=version]').val());
            addFieldForRequest('uuid', $(this).find('[name=uuid]').val());

            var project_name    = $(this).find('[name=project] [value=' + $(this).find('[name=project]').val() + ']').text();
            var controller_name = $(this).find('[name=controller] [value=' + $(this).find('[name=controller]').val() + ']').text();
            var method_name     = $(this).find('[name=method] [value=' + $(this).find('[name=method]').val() + ']').text();

            addFieldForRequest('method', project_name + '.' + controller_name + '.' + method_name);

            if ($(this).find('[name=sid]').val() != '') {
                addFieldForRequest('sid', $(this).find('[name=sid]').val());
            }

            var params = $(this).find('[name=params]').val().split(/&+/g);
            for (var i = 0; i < params.length; i++) {
                var param = params[i].split(/=/g);
                    if (param[0] && param[1]) {
                    addFieldForRequest(param[0], param[1]);
                }
            }

            query.files = [];

            // Отправка файлов
            if ($methodType.val() === 'POST') {
                $('input:file', $rowsContainer).filter(function() {
                    return $.trim($(this).val()); // Убираем незаполненные поля
                }).each(function() {
                    var matches;
                    var $currentElem  = $(this);
                    var customElemName = $currentElem.next('input:text').val();

                    if (customElemName != '') {
                        addFieldForRequest(customElemName, this.files[0]);
                    } else if ((matches = customElemName.match(/.*?\[[^\]]*\]/)) != null) {

                        // $_FILES[matches[0]][matches[1]]
                        if (matches.length > 1) {
                            var subArrayKey = matches[1];
                            if (!query[customElemName].hasOwnProperty(subArrayKey) ||
                                typeof query[customElemName][subArrayKey] != 'object') {
                                query[customElemName][subArrayKey] = {};
                            }

                            addFieldForRequest(customElemName + '[' + subArrayKey + ']', this.files[0], query[customElemName]);
                        }
                    } else {
                        addFieldForRequest('files[]', this.files[0], query.files);
                    }
                });
            }

            var cleanedQuery = {};

            for (var prop in query) {
                if (query.hasOwnProperty(prop) && !(typeof query[prop] === 'object')) {
                    cleanedQuery[prop] = query[prop];
                }
            }

            /* подпись */
            var query_secret;

            query_secret = jQuery.param(cleanedQuery); // http_build_query

            if ($('select[name=version]').val() == '2.2.1') {
                query_secret = query_secret.split('&').sort(function (a, b) {
                    return (a < b ? -1 : (a > b ? 1 : 0));
                }).join('');
            } else {
                query_secret = query_secret.replace(/&/g, '');
            }

            query.sig = Sha256.hash(query_secret + $(this).find('[name=key_secret]').val());
            formData.append('sig', query.sig);
            $resultContainer.empty();

            var requestData = $methodType.val() === 'GET' ? query : formData;

            $.ajax({
                type: $methodType.val(),
                url: url,
                dataType: 'json',
                data: requestData,
                processData: ($methodType.val() === 'GET'),
                contentType: false,
                success: function(data) {
                    if (!('LastResponseData' in window)) {
                        document.getElementById('LastResponseHint').style.display = '';
                    }

                    window.LastResponseData = data;
                    if (data && data.sid) {
                        $(':input[name=sid]').val(data.sid);
                    }

                    if (data && 'debug_errors' in data) {
                        $resultContainer.css('background-color', '#F0DFE3');
                    } else {
                        resetResponseContainerStyle();
                        console.info('В ответе API ошибок или нотайсов не найдено!');
                    }

                    $resultContainer.html('<pre>' + printJSON(data, 0) + '</pre>');
                },
                error: function(xhr) {
                    $resultContainer.html(xhr.responseText);
                }
            });

            return false;

        });

        var $FUHint = $(document.getElementById('fileuploadsHint'));

        $(document.getElementById('FUHintTrigger')).click(function() {
            $FUHint.slideToggle();
            return false;
        });

        $('#api_method').on('click', 'i.add_row', function() {
            $tmplBlock.tmpl({}).appendTo($rowsContainer);
        }).on('click', 'i.remove_row', function() {
            $(this).parent().remove();
        });
    });
})(jQuery);