    	$('#insertImageButton').click(function (){
            var objImages = $('#thumbnails .selected .imageHTML a img');
    		if(objImages.length == 0){
    			alert('Выберите изображения, кликнув по ним.');
    			return false;
    		}
    		if(objImages.length > 8){
    			alert('Нельзя выбрать больше 8 фотографий!');
    			return false;
    		}

            var images = new Array();
            var imagesString = '<картинки>';
            objImages.each(function (){
                images.push($(this).attr('src').split('/').pop());
            });
            imagesString += images.join(',');
            imagesString += '</картинки>';

    		insertAtCursor($('textarea[name="body"]').eq(0)[0],imagesString.toLowerCase());
    		$('#thumbnails .selected').removeClass('selected');
    	});

    	$('#thumbnails .image-thumb .imageHTML a').live('click', function (){
    		$(this).parent().parent().toggleClass('selected');
    		return false;
    	});

    	$('#thumbnails').sortable({
    		update: function(event, ui) {
    			sortArray = new Array();
    			thumbs = $('#thumbnails .image-thumb');
    			thumbs.each(function (){
    				id = $(this).find('.thumb-del').attr('id');
    				order = thumbs.index(this);
    				sortArray[order] = id;
    			});
            	$.get("/admin/testdrive/image_update_order/" + sortArray + ".html");
    		}
    	});
    	$("#thumbnails").disableSelection();

        $('#set_date').datepicker({
            'dateFormat':'yy-mm-dd',
        });

        $(".thumb-del").live("click", function () {
            $("." + $(this).attr('id')).fadeOut(100);
            $.get("/admin/testdrive/image_delete/" + $(this).attr('id') + ".html");
            try{
                decrementSWFUCounter(swfu);
            }catch(ex){}
            return false;
        });

    	var swfu = new SWFUpload({
    		upload_url: "/admin/testdrive/image_upload.html",
    		post_params: {"SID": "<?=Session::$sid?>"<? echo Storage::get('testdrive_id') ? ', "testdrive_id": "'.Storage::get('testdrive_id').'"' : ''; ?>},

    		file_size_limit : "5 MB",
    		file_types : "*.jpg",
    		file_types_description : "JPG Images",
    		file_upload_limit : "<?=Storage::get('images_limit_count')?>",

    		file_queue_error_handler : fileQueueError,
    		file_dialog_complete_handler : fileDialogComplete,
    		upload_progress_handler : uploadProgress,
    		upload_error_handler : uploadError,
    		upload_success_handler : uploadSuccess,
    		upload_complete_handler : uploadComplete,

    		button_placeholder_id : "spanButtonPlaceholder",
    		button_width: 80,
    		button_height: 22,
    		button_window_mode: SWFUpload.WINDOW_MODE.TRANSPARENT,
    		button_cursor: SWFUpload.CURSOR.HAND,
            swfupload_loaded_handler: swfuLoaded,
            
    		flash_url : "<?=Config::get('i_url')?>resources/swfupload.swf",

    		custom_settings : {
    			upload_target : "divFileProgressContainer"
    		},

    		debug: false
    	});
        function swfuLoaded(){
            stats = this.getStats();
            stats.successful_uploads = thumb_count;
            this.setStats(stats);
        }
        function uploadSuccess(file, serverData) {
        	try {
        		var progress = new FileProgress(file,  this.customSettings.upload_target);

        		if (serverData.substring(0, 7) === "FILEID:") {
        			$.get("/admin/testdrive/get_thumb/" + serverData.substring(7) + ".html", { debug: "0" }, function(data){
        			    $("#thumbnails").append(data);
                    });
        			progress.toggleCancel(false);
        		} else {
        			addImage("images/error.gif");
        			progress.setStatus("Ошибка.");
        			progress.toggleCancel(false);
        			alert(serverData);

        		}
        	} catch (ex) {
        		this.debug(ex);
        	}
        }
        function decrementSWFUCounter(swfu){
            stats = swfu.getStats();
            stats.successful_uploads--;
            swfu.setStats(stats);
        }
        
        $('#testdrive_tpl').click(function(){
        	$('#testdrive_body').val(
        			"<br /><br />\n" +
        			"Текст.\n" +
        			"<br /><br />\n" +	
        			"<b>Заголовок</b>\n" +
        			"<br /><br />\n" +
        			"Текст.\n" +
        			"<br /><br />\n" +
        			"<b>Краткая техническая характеристика “_”</b>\n" +
        			"<br /><br />\n" +
        			"Габаритные размеры	_ х _ х _ см\n" +
        			"<br /><br />\n" +
        			"Снаряженная масса	_ кг\n" +
        			"<br /><br />\n" +
        			"Двигатель	_-цил., _ куб. см\n" +
        			"<br /><br />\n" +
        			"Мощность	_ л.с. при _ об/мин\n" +
        			"<br /><br />\n" +
        			"Крутящий момент	_ Нм при _ об/мин\n" +
        			"<br /><br />\n" +
        			"Коробка передач	вариатор\n" +
        			"<br /><br />\n" +
        			"Тип привода	_\n" +
        			"<br /><br />\n" +
        			"Дорожный просвет	_ см\n" +
        			"<br /><br />\n" +
        			"Максимальная скорость	_ км/ч\n" +
        			"<br /><br />\n" +
        			"Разгон 0-100 км/ч	   _ с\n" +
        			"<br /><br />\n" +
        			"Средний расход топлива	_ л/100 км\n" +
        			"<br /><br />\n" +
        			"Запас топлива	 	_ л\n" +
        			"<br /><br />\n" +
        			"Объем багажника 	_ л");
        	
        });

$(document).ready(function(){


var timeout_title; 
var timeout_slug; 

    $('#title').bind('keyup', function(){
        var hold_slug = $('#hold_slug').attr('checked')?1:0;
        // Проверять аяксом, только если стоит галка "Менять автоматически ЧПУ"  
        if ((hold_slug) == 1) {
            var titleValue = $('#title').attr('value');
            clearTimeout(timeout_title);
            timeout_title = setTimeout(function () { slug(titleValue, hold_slug) },1000);
        }
    });


    $('#slug').bind('keyup', function(){
        var titleValue = $('#slug').attr('value');
        clearTimeout(timeout_slug);
        timeout_slug = setTimeout(function () { slug(titleValue, 1) },1000);
    })

/**
 * Принимает строку и делает запросс на перевод строки в ЧПУ. 
 * В зависимости от флага, меняет/нет поле с чпу
 * 
 * @param {string} titleValue строка
 * @param {int}    hold_slug флаг: менять или нет поле ЧПУ
 * 
 * @returns {void}
 */
function slug(titleValue, hold_slug) {
    var source = $('#source').attr('value');
    var valid_slug = $('#valid_slug');
    var ajaxURL = '/ajax/slug';
    var news_id = $('#news_id').attr('value');

    $.ajaxSetup({cache:false});
    $.getJSON(ajaxURL, {'title':titleValue, 'source':source, 'news_id':news_id, debug:0}, 
        function(json){
            var slug = json['slug'];
            var ableUse = json['ableUse'];
            var error = $("#slug_error");
            if (hold_slug != 0) {
                $('#slug').attr('value', slug);
            }
            
            if (slug == "") {
                ableUse == false;
            }

            if (ableUse) {
                error.html('');
                valid_slug.attr('value', '1');
            } else {
                valid_slug.attr('value', '');                
                error.html('Такой ЧПУ уже есть в базе!');
            } 
        });
        
}
});

