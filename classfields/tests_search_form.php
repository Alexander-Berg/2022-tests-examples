<h1>Форма поиска. Прототип</h1>
<style>
td {
    padding: 5px;
}
</style>
<?=$this->form->getFormStartTag()?>
    <table cellpadding="5" cellspacing="5" width="70%">
        <tr>
            <td width="50%">
                Категория: <?=$this->form->getElement('category_id')?>
            </td>
            <td width="50%">
                Секция: <?=$this->form->getElement('section_id')?>
            </td>
        </tr>
        <tr>
            <td>
                Год от: <?=$this->form->getElement('year_from')?>
            </td>
            <td>
                Год до: <?=$this->form->getElement('year_to')?>
            </td>
        </tr>
        <tr>
            <td>
                Цена от: <?=$this->form->getElement('price_from')?>USD
            </td>
            <td>
                Цена до: <?=$this->form->getElement('price_to')?>USD
            </td>
        </tr>
        <tr>
            <td>
                Пробег от: <?=$this->form->getElement('run_from')?>
            </td>
            <td>
                Пробег до: <?=$this->form->getElement('run_to')?>
            </td>
        </tr>
        <tr>
            <td>
                Руль: <?=$this->form->getElement('wheel_key')?>
            </td>
            <td>
                Таможня: <?=$this->form->getElement('custom_key')?>
            </td>
        </tr>
        <tr>
            <td width="50%">
                client_id: <?=$this->form->getElement('client_id')?>
            </td>
            <td width="50%">
                user_id: <?=$this->form->getElement('user_id')?>
            </td>
        </tr>
        <tr>
            <td width="50%">
                За: <?=$this->form->getElement('period')?>
            </td>
            <td width="50%">
                сортировать: <?=$this->form->getElement('sort')?>
            </td>
        </tr>
        <tr>
            <td>
                <?=$this->form->getElement('query', array('style' => 'width:100%'))?>
            </td>
            <td>
                <?=$this->form->getElement('search', array('style' => 'margin-left: 20px;'))?>
            </td>
        </tr>
    </table>
<?=$this->form->getFormEndTag()?>