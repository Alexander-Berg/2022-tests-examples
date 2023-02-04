<?php

use \api2\interfaces\iApiMethods;
?>

<script type="text/javascript">
    var developer_name = 'alexandrov';
    var hostMatches = location.host.match(/api2\.([a-z]*)\.auto$/);

    if (hostMatches && hostMatches.length == 2) {
        developer_name = hostMatches[1];
    }

    var demo = {
        all : {
            group : {
                getlist : 'category_id=15&section_id=1&mark_id=15',
            },
            sale : {
                search : 'category_id=15&section_id=1&limit=10',
                get : 'category_id=15&section_id=1&sale_id=11422388-5ae5f',
                getphones : 'category_id=15&section_id=1&sale_id=11422388-5ae5f',
                getsearchform : 'category_id=15',
                geteditform: 'category_id=15'
            }
        },
        catalog : {
            category : {
                getlist : '',
            },
            mark : {
                getlist : 'category_id=15',
                get : 'mark_id=1',
            },
            folder : {
                getlist : 'category_id=15&mark_id=30&folder_id=45983&level=2',
                getmodels : 'category_id=15&mark_id=30',
                getgenerations : 'category_id=15&mark_id=30&model_id=365&year=1990',
            },
            group : {
                getlist : 'category_id=15&mark_id=15',
                get : 'group_id=14313',
            },
            modification : {
                getlist : 'folder_id=3',
            }
        },
        comments : {
            comment : {
                getlist : 'project=review&object_id=9971&object=0',
                add : 'project=review&object_id=9971&object=0&text=12',
            }
        },
        geobase : {
            country : {
                getlist : '',
            },
            region : {
                getlist : 'country_id=1',
            },
            city : {
                getlist : 'region_id=47',
                getnearest : 'latitude=55.7558&longitude=37.6176',
            }
        },
        helpdesk : {
            task : {
                add : 'from_email=dfs@sdf.dw&question=dsffgdfg',
            }
        },
        my : {
            review : {
                getlist : '',
                get : 'review_id=9971',
            }
        },
        news : {
            category : {
                getlist :  '',
            },
            news : {
                getlist : '',
                get : 'news_id=19938',
            },
            testdrive : {
                getlist : '',
                get : 'testdrive_id=506',
            }
        },
        nomer : {
            category : {
                getlist : '',
            },
            story : {
                getlist : '',
                getinfo : 'id=605',
                get : 'id=605',
                add : 'category_id=15',
            }
        },
        office : {
            office : {
              getcontractor : 'date_start=2010-07-24&date_stop=2013-09-24&client_id=1200',
            },
        },
        stat : {
            items : {
                average : 'mark_id=15',
                get : 'mark_id=15&model_id=74',
                list : 'mark_id=15&model_id=74',
                dictionary : 'tech_field=body_key',
            }
        },
        users : {
            auth : {
                login : 'login=' + developer_name + '@auto.ru&pass=autoru&client_os=ios',
                logout : '',
                validsession : '',
                registration : '',
            },
            profile : {
                me : '',
            },
            message : {
                send : 'to_user_id=2977812&subject=привет&text=медвед',
            }
        }
    };

    var $uuid;
    var $form;

    $(function() {
        $uuid = $('input[name=uuid]');
        $form = $('form[name=api_form]');

        $form.find('[name=version]').val('2.2.2'); // Текущая рабочая версия API
    });

    function setUUID()
    {
        var query = {};

        query.key = $form.find('[name=key_login]').val();
        query.format  = $form.find('[name=format]').val();
        query.version = 2;//$form.find('[name=version]').val();
        query.uuid    = $form.find('[name=uuid]').val();
        query.method = '<?=iApiMethods::METHOD_API_SERVICE_GET_UUID?>';

        var query_secret = '';
        query_secret = jQuery.param(query); // http_build_query
        query_secret = query_secret.replace(/&/g, '');
        query.sig = Sha256.hash(query_secret + $form.find('[name=key_secret]').val());

        $.ajax({
            url: $form.find('[name=api_url]').val() +
                $form.find('[name=interface]').val() + '/',
            data: query
        }).done(function(result) {
            result = $.parseJSON(result);
            if ('error' in result) {
                alert(result.error.message);
            } else {
                $form.find('[name=uuid]').val(result.result.uuid);
            }
        }).fail(function() {
            alert($.parseJSON(arguments));
        });
    }

    function searchKeyForForms(a)
    {
        if (!a.hasOwnProperty(this.key)) {
            console.log(a)
        }

        if ('params' in a && 'items' in a.params &&  'forEach' in a.params.items) {
            a.params.items.forEach(searchKeyForForms, this);
        }
    }

    function searchKeyForFormsInParams(a)
    {
        if (!('params' in a) || !a.params.hasOwnProperty(this.key)) {
            console.log(a)
        }

        if ('params' in a && 'items' in a.params &&  'forEach' in a.params.items) {
            a.params.items.forEach(searchKeyForForms, this);
        }
    }


    function isFieldsKeyExist()
    {
        var s = ('LastResponseData' in window && 'result' in LastResponseData && 'fields' in LastResponseData.result);

        if (s) {
            return true;
        } else {
            console.warn('Ключа "LastResponseData.result.fields" не существует в объекте Window');
            return false;
        }
    }

    function searchProcess(command)
    {
        var key = $(this).next().val();

        if (!key.length) {
            console.warn('пустой ключ');
            return;
        }

        var thisArg = {};
        thisArg.key = key;

        switch (command) {
            case 'search_value':
                LastResponseData.result.fields.forEach(searchKeyForForms, thisArg);
            break;
            case 'search_value_in_params':
                LastResponseData.result.fields.forEach(searchKeyForFormsInParams, thisArg);
            break;
            default:
                console.error('Пыщ-пыщ-ололо: я водитель НЛО!' + "\n" + 'Ничего не работает!');
                break;
        }

        console.info('Завершено!');
    }

</script>

<style>
    .add_row, .remove_row {
        cursor: pointer;
        margin: 0 3px;
    }

    .errorInput {
        border: 1px solid red;
    }

    .file_row {
        margin: 3px auto;
        border: 1px solid #FAFAFA;
        display: inline-block;
        padding: 5px;
    }

    .file_row:nth-child(odd) {
        background-color: #EEE;
    }

    .file_field_name {
        outline: none;
    }

    #FUHintTrigger {
        border-bottom: 1px dashed #06C;
        text-decoration: none;
        margin-top: 10px;
    }

    #fileuploadsHint {
        background-color: #F3F3F3;
        border: 1px dashed #ccc;
        padding: 10px 25px;
    }
</style>

<script id="file_row_tmpl" type="text/x-jquery-tmpl">
    <div class="file_row">
    <input type="file" name="files[]">
&lt;input type="file" name="<input type="text" class="file_field_name" name="file_field_name[]" value="">
"&gt;  <i class="ico i-add-2 add_row"></i>   <i class="ico i-remove-b remove_row"></i> </div>
</script>

<?php echo $this->form->getFormStartTag(); ?>

<fieldset>
    <label><strong>Доступ к API</strong></label>
    <?php echo $this->form->getElement('api_url', array('size' => 96)); ?>
    <br/>
    <?php echo $this->form->key_login->getLabel(); ?><br/><?php echo $this->form->getElement('key_login', array('size' => 96)); ?><br/>
    <?php echo $this->form->key_secret->getLabel(); ?><br/><?php echo $this->form->getElement('key_secret', array('size' => 96)); ?><br/>
    <?php echo $this->form->uuid->getLabel(); ?><br/>
    <input type="button" onclick="setUUID();" value="Задать"> <?php echo $this->form->getElement('uuid', array('size' => 80)); ?><br/>
    <br/>
    <?php echo $this->form->version->getLabel(); ?> <?php echo $this->form->getElement('version'); ?>
    <?php echo $this->form->interface->getLabel(); ?> <?php echo $this->form->getElement('interface'); ?>
    <?php echo $this->form->format->getLabel(); ?> <?php echo $this->form->getElement('format'); ?>
    <?php echo $this->form->getElement('method_type')->getLabel()?> <?= $this->form->getElement('method_type')?><br>
</fieldset>
<fieldset>
    <label><strong>Метод API</strong></label><br>
    Проект / Контроллер / Метод<br/>
    <?php echo $this->form->getElement('project', array('style'=> 'min-width:20%;')); ?>
    <?php echo $this->form->getElement('controller', array('style'=> 'min-width:20%;')); ?>
    <?php echo $this->form->getElement('method', array('style'=> 'min-width:20%;')); ?>
    <br/>
    <?php echo $this->form->sid->getLabel(); ?><br/><?php echo $this->form->getElement('sid', array('size' => 96)); ?>
    <br/>
    <?php echo $this->form->params->getLabel(); ?><br/><?php echo $this->form->getElement('params', array('size' => 96)); ?>
    <br/>
</fieldset>
<br/>
<fieldset id="api_method">
    <label>Добавить файлы</label><br/>
    <div id="file_rows">
        <div class="file_row">
        <?php
        $fileGroup = $this->form->getGroup('files');

        echo $fileGroup->getElements()[0];
        printf('%s%s%s  <i class="ico i-add-2 add_row"></i>',
                htmlspecialchars('<input type="file" name="'),
                $this->form->getGroup('file_field_name')->getElements()[0],
                htmlspecialchars('">')
        );
        ?>
        </div>
    </div>
    <br>
    <a href="#" id="FUHintTrigger">Подсказка по загрузке файлов.</a>
    <div id="fileuploadsHint" style="display: none">
        <strong>Note:</strong>
        <p>По умолчанию файлы помещаются в массив $_FILES[files]</p>
        <p>Соответственно не стоит вручную назначать названия files или files[]</p>
        <p>Можно составить двумерные массивы:</p>
        <p>Для проверки нужно отключать проверку сигнатуры или допилить JS чтобы она учитывала файлы</p>
        <pre>
            $_FILES = [
                'custom_array' => [     // custom_array[custom_file1], custom_array[file_custom2]
                    'custom_file1' => {file},
                    'file_custom2' => {file}
                ],
                'files' => [            // Массив из файлов с неназначенным именем
                    {file},
                    {file},
                    {file}
                ],
                'another_array' => [    // another_array[], another_array[], another_array[]
                    {file},
                    {file},
                    {file}
                ]
            ];
        </pre>
    </div>
    <br/>
    <br/>
</fieldset>
<fieldset>
    <a href="" onclick="$(this).next().slideToggle(); return false;" style="border-bottom: 1px dashed; text-decoration: none;">Помощники для форм.</a>
    <div id="forms_felpers_block" style="display: none; margin-left: 15px;">
        <div>
            <a href="" onclick="isFieldsKeyExist() && searchProcess.call(this, 'search_value'); return false;" style="border-bottom: 1px dashed; text-decoration: none;">
                Вывести в консоль элементы у которых нет определенного ключа. params.items учитываются
            </a>
            <input placeholder="Ключ">
            <br>
            <a href="" onclick="isFieldsKeyExist() && searchProcess.call(this, 'search_value_in_params'); return false;" style="border-bottom: 1px dashed; text-decoration: none;">
                Вывести в консоль элементы у которых В PARAMS нет определенного ключа. params.items учитываются
            </a>
            <input placeholder="Ключ">
        </div>
    </div>
    <br><br><br>
</fieldset>

<fieldset>
    <?php echo $this->form->getElement('send'); ?>
    <span id="LastResponseHint" style="display: none; margin-left: 1em;">&mdash; Данные последнего запроса также доступны в JS через глобальную переменную LastResponseData</span>
</fieldset>

<?php echo $this->form->getFormEndTag(); ?>
<pre id="api-response"></pre>
