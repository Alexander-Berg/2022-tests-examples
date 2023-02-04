<?php
/**
 *
 * Тестовые функции пробовать здесь.
 * @package market_api
 * @version $Id$
 */

Core::getInstance()->addPath(dirname(__FILE__) . '/classes/');
Core::getInstance()->addPath(ROOT_PATH . 'lib5/libs/Api_Client/');

class Api_Test_controller extends Controller
{
    public $method = "test";
    public function load_function()
    {
        $req = $this->params_arr['function'];
        if(method_exists($this, $req)) return $this->$req();
        else die("<h1>Иди на хуй!</h1>");
    }

    public function test_reg()
    {
       $test = "////asdasdda/aa/asasdsadasddsadsa///";

       echo preg_replace("`(^([/]*)|([/]*$))`","",$test);

    }

    public function call_m()
    {
        echo realpath(dirname(__FILE__).'/../') ;
    }

    public function test_enc()
    {
         $sweet = array('a' => 'яблоко', 'b' => 'банан');
        $fruits = array('сладко' => $sweet, 'sour' => 'лимон');
        $start = microtime(true);

        for($i=0;$i++<100000;)
            $this->encoding($fruits);

       echo "End: ".(microtime(true) - $start);
       print_r($fruits);
    }

   public function encoding($fruits)
    {


        $in  = "utf8";
        $out = 'cp1251';

//       $fr = array_walk_recursive($fruits,array(&$this,"convert_encoding"),array("in"=>$in,"out"=>$out));

    }
        protected function convert_encoding(&$item, $key,$data)
         {
            $key =  $key;
            $item =  $item;

         }

    public function test_api()
    {
        $cur_class = Controller_Api::factory("Category@market",array('key'=>'e985727cab16eafbd2bfa1a4a9e193bbac799aa78c0afa5fee60ae55db523230'));

        try{
           $data_arr =  $cur_class->getList();
        } catch(Exception $e) {
           echo $e->getMessage();
        }

        print_r($data_arr);
    }


    public function test_upload_nomer_story()
    {
        Core::getInstance()->addPath(ROOT_PATH . 'lib5/libs/Api_Client/');
                $config =  array (
              'key_login'	=> '620b5412b4c2f74240cd2ac3cf2842521ebf4c38ca7eed9ee7a8f19b893cf6f1'
            , 'key_secret'  => '74e49fc586620d0d5638c9a0d015f55bfbb7d6ee78bd325bc55275b18160deae'
            , 'api_url'		=> 'http://ftonn2.auto/rest/api'
            , 'format'		=> 'serialize'
            , 'interface'	=> 'rest'
        );
        $client = new Api_Client($config);

        // включаем поддержку загрузки
        $client->_setUploadSupport(true);
        $sid = $client->users->auth->login(array('login' => 'tonn@auto.ru', 'pass' => 'autoru'));
        $sid = $sid['sid'];
        $story["category_id"] = 9;
        $story['subject']     = "Женя обкакался";
        $story['body']        = "Сегодня женя обкакался, лол";
        $story['coordinates_lat'] = "55.8007";
        $story['coordinates_lng'] = "37.7386";
        $story['hide_author'] = "";
        $story['numbers'] = array(0=>array("nomer"=>"A119ВВ190","group_id"=>3,"mark_id"=>4));

        $start = microtime(true);
        $data = $client->nomer->story->add(array('file1' => '/home/haron/Photos/PICT0010.JPG','file2' => '/home/haron/Photos/PICT0010.JPG',"sid"=>$sid,"story"=>serialize($story)));
        echo "<br> end:".(microtime(true)-$start);
        echo "<pre>";
        print_r($data);
    }

    public function test_h()
    {
        $out = array();

        $client = new Api_Client(Config::get('api_client_config@api'));

        //print_r($client->catalog->mark->getList(array('category_id' => 5)));
        //exit();
        //print_r($client->catalog->group->getList(array('category_id' => 15, 'mark_id' => 170)));

        /*$stories = $client->nomer->story->getList(array('location' => '56.231973451633706;36.64808954296882;55.37996387233378;38.71351923046882'));
        print_r($stories);*/
        //print_r($client->nomer->story->getInfo(array('id' => 1)));
        //print_r($client->_getError());
        //print_r($client->nomer->category->getList(array()));

        /*$client->_setUploadSupport(true);
        var_dump($client->nomer->story->upload(array('file1' => '/home/haron/Photos/PICT0010.JPG')));
        */

        /*var_dump($client->users->auth->validSession(array()));
        return ;*/


        var_dump($sid = $client->users->auth->login(array('test' => 'Жопа', 'login' => 'haron@auto.ru', 'pass' => 'autoru')));


        //$sid = array('sid' => '2411118-d9f11aa061b137bea99f43eb6161e03c_f4bf54c4fa68334a_34ecf48ce9b9f6f29d4c9f304fe8aeba');
        //var_dump($client->users->auth->validSession(array('sid' => $sid['sid'])));
        //print_r($client->users->auth->logout(array('sid' => $sid['sid'])));
        //print_r($client->users->profile->me(array('sid' => $sid['sid'])));
        //print_r($client->nomer->story->getInfo(array('id' => 1, 'sid' => $sid['sid'])));

        $story = array(
            'sid' => $sid['result']['sid'],
        );

        //var_dump($client->nomer->story->add($story));


        // включаем поддержку загрузки
        /*$client->_setUploadSupport(true);


        $story["category_id"] = 9;
        $story['subject']     = "All великая команда" . time();
        $story['body']        = " гыг аты баты ";
        $story['coordinates_lat'] = "55.8007";
        $story['coordinates_lng'] = "37.7386";
        $story['hide_author'] = "";
        $story['numbers'] = array(array("nomer"=>"A119ВВ190", "group_id"=>3, "mark_id"=>4));


        $story['file1'] = '/home/haron/Photos/PICT0010.JPG';
        //$story['file2'] = '/home/haron/Photos/PICT0010.JPG';

        var_dump($client->nomer->story->add($story));
        print_r($client->_getLastError());*/

        return $out;
    }
    /**
    * Форма для тестирования API
    *
    * @return array
    */
    public function getFormAction()
    {
        $config = Config::get('api_client_config');
        $form = new Form2('api_form', '', Form2::METHOD_POST);
        $proj_list = Model::getData('Api_Project:getList@api');
        $projects = array('' => 'выберите проект...');
        foreach ($proj_list as $project) {
            $projects[$project['id']] = $project['project_alias'];
        }
        $form->addElement(
            'project',
            'select',
            array(
                'options' => $projects
            )
        );
        $form->addElement(
            'controller',
            'select',
            array(
                'options' => array()
            )
        );
        $form->addElement(
            'method',
            'select',
            array(
                'options' => array()
            )
        );
        $form->addElement(
            'api_url',
            'hidden',
            array(
                'value' => $config['api_url']
            )
        );
        $form->addElement(
            'version',
            'hidden',
            array(
                'value' => $config['version']
            )
        );
        $form->addElement(
            'key_login',
            'text',
            array(
                'label' => 'Ключ авторизации',
                'value' => $config['key_login']
            )
        );
        $form->addElement(
            'key_secret',
            'text',
            array(
                'label' => 'Секретный ключ',
                'value' => $config['key_secret']
            )
        );
        $form->addElement(
            'interface',
            'select',
            array(
                'label' => 'Интерфейс доступа',
                'options'  => array('rest' => 'REST'),
                'selected' => $config['interface'],
            )
        );
        $form->addElement(
            'format',
            'select',
            array(
                'label' => 'Формат ответа',
                'options'  => array('json' => 'JSON'),
                'selected' => $config['format'],
            )
        );
        $form->addElement(
            'sid',
            'text',
            array(
                'label' => 'Идентификатор сессии',
                'value' => ''
            )
        );
        $form->addElement(
            'params',
            'text',
            array(
                'label' => 'Параметры запроса:',
                'value' => ''
            )
        );
        $form->addElement(
            'send',
            'submit',
            array(
                'value' => 'Отправить'
            )
        );
        return array(
            'form' => $form
        );
    }
    /**
    * Список контроллеров для select
    *
    * @param int $project_id Ид проекта
    *
    * @return array
    */
    public function getControllersAction($project_id)
    {
        $project_id = (int) $project_id;
        $form = new Form2('controllers', "");
        $controllers = Model::getData('Api_User_Cont:getList@api', array('project_id' => $project_id));
        $controllers = array('' => 'выберите контроллер...') + array_map(function ($item) {
            return strtolower($item['controller']);
        }, $controllers);
        $form->addElement('controller', 'select', array('options' => $controllers));
        return array(
            'data' => $form->getElement('controller')
        );
    }
    /**
    * Список методов для select
    *
    * @param int $controller_id Ид контроллера
    *
    * @return array
    */
    public function getMethodsAction($controller_id)
    {
        $controller_id = (int) $controller_id;
        $form = new Form2('methods', "");
        $methods = Model::getData('Api_User_Method:getList@api', array('controller_id' => $controller_id));
        $methods = array('' => 'выберите метод...') + array_map(function ($item) {
            return strtolower($item['method']);
        }, $methods);
        $form->addElement('method', 'select', array('options' => $methods));
        return array(
            'data' => $form->getElement('method')
        );
    }
}
