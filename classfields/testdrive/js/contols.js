$(function (){
	var testDriveSelectedIndex = 0;

	$('<div id="selector"><div><img src="//i.auto.ru/testdrive/img/i-arrow-prev-test.gif" alt="Предыдущий" title="Предыдущий" class="prev" /><img src="//i.auto.ru/testdrive/img/i-arrow-next-test.gif" alt="Следующий" title="Следующий"  class="next"/></div></div>').appendTo('#testdrive #previewTestDrive').hide();

	testDriveQty = $('#previewTestDrive .testdrive-cell').length;
	for(i = 0; i < testDriveQty; i++){
		$('<input type="radio" name="td" value="' + i + '" />').appendTo('#selector div').click(function (){
			testDriveSelectedIndex = parseInt(this.value);
			$('#previewTestDrive .testdrive-cell').hide().eq(this.value).fadeIn();
		});
	}

	$('#selector img').click(function (){
		collection = $('#previewTestDrive .testdrive-cell');
		testDriveSelectedIndex += ($(this).hasClass('next') ? +1 : -1);
		if(testDriveSelectedIndex >= collection.length) testDriveSelectedIndex = 0;
		if(testDriveSelectedIndex < 0) testDriveSelectedIndex = collection.length - 1;
		collection.hide().eq(testDriveSelectedIndex).fadeIn();
		$('#selector input').each(function (index){
			this.checked = (index == testDriveSelectedIndex);

		});
	});

	$('#previewTestDrive .testdrive-cell').not(':first').hide();

	$('#testdrive .autoru').hover(function (){
		$('#selector').show();
	},function (){
		$('#selector').hide();
	});

	$('#comment-form textarea').keypress(charCounter).keydown(charCounter).keyup(charCounter).blur(charCounter).focus(charCounter).mouseover(charCounter);
	var maxCharComment = 500;
	function charCounter(){
		if(this.value.length > maxCharComment) this.value = this.value.substring(0,maxCharComment);
		$('#comment-form .counter').html(this.value.length + ' из ' + maxCharComment + ' символов.');
	}


	$('.voting:not(.close) li').hover(function (){
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

	$('#votes input[type="radio"]').click(function (){
		collection = $(this).parent().parent().find('div.selected').removeClass('selected').find('img').remove();
		$(this).parent().find('div').addClass('selected').append('<img src="http://i.auto.ru/testdrive/img/bg-vote.png" alt="" />');
	});
	$('#vote-form li div').click(function (){
		$(this).next('input:enabled').trigger('click');
	});

	$('#vote-form').submit(function (){
		form = $(this);
		vote = '';
        id = this.elements[0].value;
        var recaptcha_challenge_field = this.elements[6] ? this.elements[6].value : '';
		var recaptcha_response_field  = this.elements[7] ? this.elements[7].value : '';

		if(this.elements[1].checked) vote = this.elements[1].value;
		if(this.elements[2].checked) vote = this.elements[2].value;
		if(this.elements[3].checked) vote = this.elements[3].value;
		if(this.elements[4].checked) vote = this.elements[4].value;
		if(this.elements[5].checked) vote = this.elements[5].value;

		if(vote == ''){
			alert('Поставьте оценку!');
			return false;
		}else if(recaptcha_response_field.length < 1) {
			alert('Введите защитный код!');
			return false;
		}

		form.find('.captcha,.vote-desc,.default-result').hide();
		form.find('input').hide();

        var callback = function(xml){
            var status = $(xml).find('status').text();
            var html = $(xml).find('html').text();
            switch (status){
                case 'ok':
                    form.find('input').attr('disabled', 'disabled').hide();
                    $('#rate-wrapper-' + id).html(html);
                    form.find('p.stat').css('margin-top', ((eval(form.find('h3').css('margin-top').replace('px', '')) + 3) + 'px'));
                    break;
                case 'captcha':
                    form.find('.captcha,.vote-desc,.default-result').show();
                    form.find('input').show();
                    alert("Неверно указан защитный код!");
                    //window.location.href = window.location.href;
                    break;
                case 'denied':
                    alert('У вас нет прав для данной операции');
                    break;
            }
        };

		$.post(this.action, {
							'mark':vote,
                            'id':id,
							'recaptcha_response_field'  : recaptcha_response_field,
                            'recaptcha_challenge_field' : recaptcha_challenge_field
							},
                            callback,
                            "xml");
		return false;
	});

	$('.thumbs a').click(function (){
		var _this = $(this);
		var parentBlock = _this.parent().parent().parent().parent();

		var title = _this.attr('title');
		var comment = parentBlock.find('.zoomer .photo-comment');
		comment.html(title);
		title.length > 0 ? comment.show(200) : comment.hide(200);

		parentBlock.find('.zoomer img').hide().attr({'src':this.href}).load(function(){
			$(this).fadeIn(100);
		});

		parentBlock.find('.thumbs a').removeClass('selected').find('img.selected').remove();
		_this.addClass('selected').append('<img src="http://img.auto.ru/design2007/img/miss/space.gif" alt="" class="selected"/>');

		return false;
	});

	$('.player-photos').each(function (){
		$('<div class="zoomer"><img src="http://i.auto.ru/testdrive/img/space.gif" alt="" /></div>').appendTo(this).click(function (){
			var list = $(this).parent().find('.thumbs a');
			var length = list.length;
			var index =  parseInt(list.index(list.filter('.selected'))) + 1;

			//if(index < 0) index = length - 1;
			if(index >= length) index = 0;
			//if(index < 0 || index > length) return;

			list.eq(index).trigger('click');
		});
	});

	//При загрузке увеличиваем первую фотку
	$('.thumbs li:first-child a').trigger('click');

	//Mega-tabs
	/*$('.mega-tabs .selected').each(function (){
		$(this).next('.tab-block').show();

	});
	$('.mega-tabs .tab-link').click(function (){
		thisTab = $(this);
		parentBlock = thisTab.parent();

		if(thisTab.hasClass('selected')) return false;

		parentBlock.find('.tab-link').removeClass('selected');
		parentBlock.find('.tab-block').fadeOut(200);
		thisTab.addClass('selected');
		thisTab.next('.tab-block').fadeIn(200);

	});*/

	$('<li class="all"><a href="#">Весь список</a></li>').appendTo('.subcategory-list .collapsed').click(function (){
		$(this).parent().toggleClass('collapsed');
	});
});