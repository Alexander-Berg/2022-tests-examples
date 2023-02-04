<?php if (count($this->data) > 0) { ?>
<div class="widget widget_theme_white widget_view_padding widget_view_top widget_view_border-top">
    <h2 class="widget-title">
        <a href="<?= Helpers_Url::l('http://news.auto.ru/category/testdrives/', false, false, false) ?>" class="widget-title-link">
            Тест-драйвы<?=($this->mark_header['name'] ? ' ' . $this->mark_header['name'] : '')?>
        </a>
    </h2>
    <ul class="widget-list widget-list_testdrive">
        <?php foreach ($this->data as $testdrive) { ?>
        <li class="widget-item widget-item_testdrive">
            <a href="<?=\Helpers_Url::l('http://news.auto.ru', false, false, false) . $testdrive['slug_url'];?>" title="<?=$testdrive['title'];?>">
                <img class="widget-item-p widget-item-p_wide" src="<?=$testdrive['image']['423x204'];?>" alt="<?=$testdrive['title'];?>">
            </a>
            <a href="<?=\Helpers_Url::l('http://news.auto.ru', false, false, false) . $testdrive['slug_url'];?>" class="widget-item-t" title="<?=$testdrive['title'];?>"><?
                echo \all7\classes\Helpers\Typograph::process($testdrive['title'], false);
            ?></a>&nbsp;
            <?// if ($testdrive['comments_count'] > 0) { ?>
                <span class="widget-item-i">
                    <a href="<?=\Helpers_Url::l('http://news.auto.ru', false, false, false) . $testdrive['slug_url_comments']; ?>" title="Комментарии">
                        <i class="ico i-comments" title="Комментарии">Комментарии:</i>
                    </a>
                    <a href="<?=\Helpers_Url::l('http://news.auto.ru', false, false, false) . $testdrive['slug_url_comments']; ?>" title="Комментарии" class="c-black">
                        <?=$testdrive['comments_count'];?>
                    </a>
                </span>
            <?// } ?>
    	</li>
        <?php } ?>
    </ul>
</div>
<?php }

