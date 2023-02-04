(function($){
    $(function (){
    	$('.conf-name .dashed').bind('click', function (){
    		var parent = $(this).parents('.conf-name');
    		parent.toggleClass('cheched');//.siblings().removeClass('cheched');
    		var l = parent.find(':checked').length;
    		parent.find('sup').html(l == 0 ? '' : l);
    	});
    	$('.moderation').bind('click', function (){
    		var div = $(this).parent().parent().parent().find('.reason');
    		$(this).hasClass('bad') ? div.slideDown(300) : div.slideUp(300) ;
    	});
    });
})(jQuery);