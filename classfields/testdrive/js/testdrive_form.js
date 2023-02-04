    	$('#insertImageButton').click(function (){
    		obj = $('#thumbnails .selected .imageHTML');
    		if(obj.length == 0){
    			alert('Выберите изображения, кликнув по ним.');
    			return false;
    		}
    		if(obj.length > 8){
    			alert('Нельзя выбрать больше 8 фотографий!');
    			return false;
    		}
    		imageList = '<div class="player-photos">\n\t<div class="thumbs">\n\t\t<ul>\n';
    		obj.each(function (){
    			imageList += '\t\t\t\t<li>' + $(this).html() + "</li>\n";
    		});
    		imageList += "\t\t</ul>\n\t</div>\n</div>";

    		insertAtCursor($('textarea[name="body"]').eq(0),imageList.toLowerCase());

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
