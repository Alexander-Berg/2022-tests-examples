$(function(){
	var t = null;
	if (document.location.hash) t = /^#(.*)/.exec(document.location.hash);
	var photo_name =  (t && t[1]) ? t[1] : false;
	var photos_a_arr = $('div#player-photos ul li a');
	var photos_links = new Array();
	for(var i=0, photo_link, tmp; i < photos_a_arr.length; i++){
		photo_link = photos_a_arr[i].href;
		tmp = /(.*?)\.jpg/.exec(photo_link.substr(1+photo_link.lastIndexOf('/')));
		photos_links.push(tmp[1]);
	}

	function getPhotoIdByName(name){
		for(var i=0; i < photos_links.length; i++){
			if (photos_links[i]==name) return i+1;
		}
		return 1;
	}

	var start_id;
	if (/^[0-9]{1,3}$/.test(photo_name)) {
		start_id = photo_name;
	}else{
		start_id = getPhotoIdByName(photo_name);
	}

	$('#thumbs ul').jcarousel({
		scroll: 3,
		initCallback: function (carousel) {
			$('#zoomer .next-photo').bind('click', function() {
				lastValue = carousel.options.scroll;
				carousel.options.scroll = 1;
				carousel.next();
				carousel.options.scroll = lastValue;
				return false;
			});

			$('#zoomer .prev-photo').bind('click', function() {
				lastValue = carousel.options.scroll;
				carousel.options.scroll = 1;
				carousel.prev();
				carousel.options.scroll = lastValue;
				return false;
			});
		}
	});


	$('#thumbs a').click(function (){
		var _this = $(this);
		var list = $('#thumbs a');

		var title = _this.attr('title');
		var comment = $('#zoomer .photo-comment');
		comment.html(title);     
		title.length > 0 ? comment.show(200) : comment.hide(200);
		$('#zoomer img').hide().attr({'src':this.href}).load(this.href, function(response, status){
            if (status == "error") {
                $('#zoomer').addClass("no-photo");
            }
			$(this).fadeIn(150);
		});

		list.removeClass('selected').find('img.selected').remove();
		_this.addClass('selected').append('<img src="http://i.auto.ru/design/2009/img/space.gif" alt="" class="selected" />');

		var index =  parseInt(list.index($('#thumbs a.selected')));

		$('#zoomer .prev-photo, #zoomer .next-photo').removeClass('disabled');
		if(index == 0) $('#zoomer .prev-photo').addClass('disabled');
		if(index+2 > list.length) $('#zoomer .next-photo').addClass('disabled');
		self.status = index+' >= '+list.length;

		return false;
	});

	//При загрузке увеличиваем первую фотку
	$('#thumbs li:first-child a').trigger('click');

	$('#zoomer .next-photo,#zoomer .prev-photo').click(function(){
		var list = $('#thumbs a');
		var length = list.length;
		var increment = $(this).hasClass('next-photo') ? +1 : -1 ;
		var index =  parseInt(list.index($('#thumbs a.selected'))) + increment;

		$('#zoomer .prev-photo, #zoomer .next-photo').removeClass('disabled');
		if(index == 0) $('#zoomer .prev-photo').addClass('disabled');
		if(index >= length) $('#zoomer .next-photo').addClass('disabled');

		if(index < 0 || index > length) return;

		list.eq(index).trigger('click');
	});

	$('#zoomer img').click(function (){
		$('#zoomer .next-photo').trigger('click');
	});

	$(document).keydown(function(event){
		if(event.ctrlKey && event.keyCode == 37) $('#zoomer .prev-photo').trigger('click');
		if(event.ctrlKey && event.keyCode == 39) $('#zoomer .next-photo').trigger('click');
	});

});