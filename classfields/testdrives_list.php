<?php if (count($this->data) > 0) { ?>
<div class="testdrives widget widget_theme_white widget_view_padding widget_view_top widget_role_tabs">
	<?php foreach ($this->data as $testdrive) { ?>
    <article class="testdrive">
    	<div class="testdrive-r">
            <div class="testdrive-r-w">
                <h4 class="testdrive-title">
                    <a href="<?=Helpers_Url::l("http://news.auto.ru" . $testdrive['slug_url'], false, false, false);?>">
                        <?=\all7\classes\Helpers\Typograph::process($testdrive['title'], false);?>
                    </a>
                </h4>
                <p class="testdrive-info">
                    <?=$testdrive['date'];?>
                    <?// if($testdrive['comments_count'] >= 0){?>
                        &nbsp;
                        <a href="<?=Helpers_Url::l("http://news.auto.ru", false, false, false) . $testdrive['slug_url_comments'];?>" title="Комментарии"><i class="ico i-comments" title="Комментарии">Комментарии:</i></a>&nbsp;
                        <a href="<?=Helpers_Url::l("http://news.auto.ru", false, false, false) . $testdrive['slug_url_comments'];?>" title="Комментарии">
                        <?=$testdrive['comments_count'];?>
                        </a>
                    <?// } ?>
                </p>
                <p class="testdrive-anons"><?=\all7\classes\Helpers\Typograph::process($testdrive['short_html']);?></p>
            </div>
        </div>
        <p class="testdrive-pic">
            <a href="<?=Helpers_Url::l("http://news.auto.ru" . $testdrive['slug_url'], false, false, false);?>">
                <img class="testdrive-img" src="<?=$testdrive['image']['155x87'];?>" alt="<?=$testdrive['title'];?>">
            </a>
        </p>
    </article>
    <?php } ?>
</div>
<?php } else { ?>
<p class="p fs-14">По данному автомобилю тест-драйвов нет. Попробуйте выбрать другое поколение этого автомобиля.</p><br><br>
<?php } ?>
