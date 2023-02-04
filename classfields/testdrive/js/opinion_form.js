        $('.voting li').hover(function (){
    		$(this).toggleClass('hover');
    	},function (){
    		$(this).toggleClass('hover');
    	}).click(function (){
    		current = $(this);
    		parentBlock = current.parent();
    		parentBlock.find('input').removeAttr('checked');
    		parentBlock.find('li').removeClass('checked');
    		current.addClass('checked').find('input').attr({'checked':'checked'});
    	});

    	$('.hint').focus(function (){
    		if(this.value == this.title){
    			this.value = '';
    			$(this).removeClass('grey');
    		}
    	}).blur(function (){
    		if(this.value == ''){
    			this.value = this.title;
    			$(this).addClass('grey');
    		}
    	});

        $(".thumb-del").live("click", function () {
            $("." + $(this).attr('id')).fadeOut(100);
            $.get("/my/image_delete/" + $(this).attr('id') + ".html");
            return false;
        });
        
        $(".thumb-main").live("click", function(){
        	$.get("/my/image_main/" + $(this).attr('id') + ".html", {'opinion_id':"<?=Storage::get('opinion_id')?>"});
        	$(".image-thumb").removeClass("main-image-thumb");
        	$("." + $(this).attr('id')).addClass("main-image-thumb");
        });

    	var swfu = new SWFUpload({
    		upload_url: "/my/image_upload.html",
    		post_params: {"SID": "<?=Session::$sid?>"<? echo Storage::get('opinion_id') ? ', "opinion_id": "'.Storage::get('opinion_id').'"' : ''; ?>},

    		file_size_limit : "5 MB",
    		file_types : "*.jpg",
    		file_types_description : "JPG Images",
    		file_upload_limit : "16",

    		file_queue_error_handler : fileQueueError,
    		file_dialog_complete_handler : fileDialogComplete,
    		upload_progress_handler : uploadProgress,
    		upload_error_handler : uploadError,
    		upload_success_handler : uploadSuccess,
    		upload_complete_handler : uploadComplete,

    		button_placeholder_id : "spanButtonPlaceholder",
    		button_width: 70,
    		button_height: 30,
    		button_window_mode: SWFUpload.WINDOW_MODE.TRANSPARENT,
    		button_cursor: SWFUpload.CURSOR.HAND,

    		flash_url : "<?=Config::get('i_url')?>resources/swfupload.swf",

    		custom_settings : {
    			upload_target : "divFileProgressContainer"
    		},

    		debug: false
    	});

        function uploadSuccess(file, serverData) {
        	try {
        		var progress = new FileProgress(file,  this.customSettings.upload_target);

        		if (serverData.substring(0, 7) === "FILEID:") {
        			$.get("/my/get_thumb/" + serverData.substring(7) + ".html", { debug: "0" }, function(data){
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
