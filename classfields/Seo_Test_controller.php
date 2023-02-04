<?php

/**
 * Контроллер для тестов
 */
class Seo_Test_controller extends Controller
{
    /**
     * Хз что за метод
     * 
     * @param int   $param_num  количество параметров
     * @param array $params_arr массив параметров
     * 
     * @return void
     */
    public function getSeoData($param_num, $params_arr)
    {
    }
    
    /**
     * Страничка для тестирования сео-параметров
     * 
     * @return array
     */
    public function output()
    {
        Helpers_Seo::setParams('var1', 'нож');
        Helpers_Seo::setParams(array('var2' => 'ножницы', 'var3' => 'деньги'));
        return array('data' => $this->params_arr['param1'] . '|' . $this->params_arr['param2'] . '|' . $this->params_arr['param3']);
    }
}