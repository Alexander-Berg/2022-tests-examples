<?php

use \api2\classes\Acl\iResources;

class Api2_Test_controller extends Controller
{
    use \lib5\Traits\DI;

    /**
     * Форма для тестирования API
     *
     * @return array
     */
    public function getFormAction()
    {
        //if (\lib5\classes\Helpers\Ip::isOfficeRequest()) {
            if (\Storage::get('SERVER_TYPE') === 'dev') {
                // хардкод: пользователь с ид=15 - это TestApi
                $user = $this->getDi()->getResource('\\api2\\models\\Users')->cache()->findByPk(15);
                $key_login  = $user['user_api_key'];
                $key_secret = $user['secret_key'];
                $uuid       = '0caf4f71500324dbc9cb7e97d4a20603';
            } else {
                $key_login  = '';
                $key_secret = '';
                $uuid       = '';
            }
            return [
                'form' => $this->_getForm($key_login, $key_secret, $uuid),
            ];
        //} else {
        //    return $this->code404();
        //}
    }

    /**
     * Получить список проектов для селекта
     *
     * @return array
     */
    private function _getProjects()
    {
        $projects = $this->getDi()->getResource('\\api2\\models\\Api\\Projects')->cache()->findAllByParams(['order' => ['name']]);
        $projects = ['' => 'выберите проект...'] + \Helpers_Array::collectPairs($projects, 'id', 'name');

        return $projects;
    }

    /**
     * Получить список версий для селекта.
     *
     * @return array
     */
    private function _getVersions()
    {
        $versions = [];
        foreach ($this->getDi()->getResource('\\api2\\models\\Versions')->cache()->findAllByParams([]) as $version) {
            $name = trim($version['major'] . '.' . ($version['minor'] ? : ''), '.');
            $versions[$name] = $name;
        }

        return $versions;
    }

    /**
     * Получить форму
     *
     * @param string $key_login  Ключ авторизации
     * @param string $key_secret Секретный ключ
     * @param string $uuid       UUID
     *
     * @return \Form2
     */
    private function _getForm($key_login, $key_secret, $uuid)
    {
        $form = new Form2('api_form', '', Form2::METHOD_POST);
        $form->addElement(
            'project',
            'select',
            ['options' => $this->_getProjects()]
        );
        $form->addElement(
            'controller',
            'select',
            ['options' => []]
        );
        $form->addElement(
            'method',
            'select',
            ['options' => []]
        );
        $form->addElement(
            'api_url',
            'hidden',
            ['value' => '/']
        );
        $form->addElement(
            'version',
            'select',
            [
                'label'    => 'Версия',
                'options'  => $this->_getVersions(),
            ]
        );
        $form->addElement(
            'key_login',
            'text',
            [
                'label' => 'Ключ авторизации',
                'value' => $key_login,
            ]
        );
        $form->addElement(
            'key_secret',
            'text',
            [
                'label' => 'Секретный ключ',
                'value' => $key_secret,
            ]
        );
        $form->addElement(
            'uuid',
            'text',
            [
                'label' => 'uuid',
                'value' => $uuid,
            ]
        );
        $form->addElement(
            'interface',
            'select',
            [
                'label'    => 'Интерфейс доступа',
                'options'  => ['rest' => 'REST'],
                'selected' => 'rest',
            ]
        );
        $form->addElement(
            'format',
            'select',
            [
                'label'    => 'Формат ответа',
                'options'  => ['json' => 'JSON'],
                'selected' => 'json',
            ]
        );
         $form->addElement(
            'sid',
            'text',
            [
                'label' => 'Идентификатор сессии',
                'value' => \Session::getSid()
            ]
        );
        $form->addElement(
            'params',
            'text',
            [
                'label' => 'Параметры запроса',
                'value' => ''
            ]
        );
        $form->addElement(
            'send',
            'submit',
            [
                'value' => 'Отправить'
            ]
        );

        $formMethod = new \Form2_Element_Select([
            'name' => 'method_type',
            'id' => 'method_type',
            'options' => [
                'GET'  => 'GET',
                'POST' => 'POST'
            ],
            'label' => 'Тип запроса'
        ]);

        $form->addElement($formMethod);

        $filesGroup = new \Form2_Group('files', false);
        $fileFieldGroup = new \Form2_Group('file_field_name', false);

        $fileUploadElement = new \Form2_Element_File([
            'name' => 0
        ]);

        $fileUploadNameElement = new \Form2_Element_Text([
            'name' => 0,
            'class' => 'file_field_name'
        ]);

        $fileFieldGroup->addElement($fileUploadNameElement);
        $filesGroup->addElement($fileUploadElement);

        $form->addGroup($filesGroup);
        $form->addGroup($fileFieldGroup);

        return $form;
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
        $controllers = $this->getDi()->getResource('\\api2\\models\\Api\\Controllers')
            ->cache()
            ->findAllByParams(['api_project_id' => (int) $project_id]);
        $controllers = \Helpers_Array::rebuildForKey($controllers, 'id');
        $controllers = ['' => 'выберите контроллер...'] + array_map(function ($item) {
            return strtolower($item['name']);
        }, $controllers);

        $form = new Form2('controllers', "");
        $form->addElement('controller', 'select', ['options' => $controllers]);

        return [
            'data' => $form->getElement('controller')
        ];
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
        $methods = $this->getDi()->getResource('\\api2\\models\\Api\\Methods')
            ->cache()
            ->findAllByParams(['api_controller_id' => (int) $controller_id]);
        $methods = \Helpers_Array::rebuildForKey($methods, 'id');
        $methods = ['' => 'выберите метод...'] + array_map(function ($item) {
            return strtolower($item['name']);
        }, $methods);

        $form = new Form2('methods', "");
        $form->addElement('method', 'select', ['options' => $methods]);

        return [
            'data' => $form->getElement('method')
        ];
    }
}
