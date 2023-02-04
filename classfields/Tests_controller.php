<?php

class Tests_controller extends Controller
{
    public function TestSphinx()
    {
        $clientObj = $this->getClient('all');

        /* опции по умолчанию. Считаем кол-во совпавших триграм  */
        //$clientObj->SetMatchMode ( SPH_MATCH_EXTENDED2 );
        //$clientObj->SetRankingMode ( SPH_RANK_WORDCOUNT );
        $clientObj->SetLimits(0, 1000, 1000);
        $clientObj->SetArrayResult ( true );
        //$clientObj->ResetFilters();
        //$clientObj->SetFilter ( "client_id", array(2252));
        $range_fields = array('run', 'id', 'client_id', 'user_id');
        srand(time());
        if(rand(0, 1) === 1) {
            $min = rand(0, 1000);
            $max = $min + rand(0, 1000);
            $clientObj->SetFilterRange ( $range_fields[rand(0, 3)], $min, $max);
        } else {
            $clientObj->SetFilter ( $range_fields[rand(0, 3)], array(rand(0, 1000)));
        }

        //$clientObj->SetSortMode ( SPH_SORT_EXPR,  "@weight + category_weight + 2 - abs(len - $len)");

        print_r($clientObj->Query("", "all"));
    }

    public function TestSearchForm()
    {
        $clientObj = $this->getClient('all');
        $form = new Form2('parser-query-form', '/tests/search_form/', Form2::METHOD_POST);
        $form->addElement('query',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('client_id',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('user_id',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('category_id',  'select',   array('label' => 'Найти', 'options' => array('' => 'Любая категория', '15' => 'Легковые'), 'view' => 'form_errors@lib5'));
        $form->addElement('section_id',  'select',   array('label' => 'Найти', 'options' => array('' => 'Любая секция', '1' => 'Поберданные', '2' => 'Новые'),  'view' => 'form_errors@lib5'));
        $form->addElement('run_to',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('run_from',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('wheel_key',  'select',   array('label' => 'Найти', 'options' => array('' => 'Любой', '1' => 'Левый', '2' => 'Правый'),  'view' => 'form_errors@lib5'));
        $form->addElement('custom_key',  'select',   array('label' => 'Найти', 'options' => array('' => 'Любой', '1' => 'растаможен', '2' => 'Не растаможен'),  'view' => 'form_errors@lib5'));
        $form->addElement('sort',  'select',   array('label' => 'Найти', 'options' => array('1' => 'По цене', '2' => 'По пробегу'),  'view' => 'form_errors@lib5'));
        $form->addElement('period',  'select',   array('label' => 'Найти', 'options' => array('' => 'Любой', '1' => 'За неделю', '2' => 'За месяц'),  'view' => 'form_errors@lib5'));

        $form->addElement('year_from',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('year_to',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('price_from',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('price_to',  'text',   array('label' => 'Найти',  'view' => 'form_errors@lib5'));
        $form->addElement('search', 'submit', array('label' => 'Найти',  'value' => 'Найти'));

        $form->addFilter('callback', array('strip_tags'));
        $form->addFilter('callback', array('trim'));
        if ($form->isSubmit()) {
            $values = $form->getValues();

            $clientObj = $this->getClient('all');

            /* опции по умолчанию. Считаем кол-во совпавших триграм  */
            //$clientObj->SetMatchMode ( SPH_MATCH_EXTENDED2 );
            //$clientObj->SetRankingMode ( SPH_RANK_WORDCOUNT );
            $clientObj->SetLimits(0, 10, 10);
            $clientObj->SetArrayResult ( true );
            //$clientObj->ResetFilters();
            //$clientObj->SetFilter ( "client_id", array(2252));
            $range_fields = array('run', 'id', 'client_id', 'user_id');

            if($values['category_id']) {
                $clientObj->SetFilter ( 'category_id', array($values['category_id']));
            }
            if($values['wheel_key']) {
                $clientObj->SetFilter ( 'wheel_key', array($values['wheel_key']));
            }
            if($values['custom_key']) {
                $clientObj->SetFilter ( 'custom_key', array($values['custom_key']));
            }
            if($values['section_id']) {
                $clientObj->SetFilter ( 'section_id', array($values['section_id']));
            }
            if($values['client_id']) {
                $clientObj->SetFilter ( 'client_id', array($values['client_id']));
            }
            if($values['user_id']) {
                $clientObj->SetFilter ( 'user_id', array($values['user_id']));
            }

            if($values['year_from'] && $values['year_to']) {
                $clientObj->SetFilterRange ( 'year', $values['year_from'], $values['year_to']);
            } elseif($values['year_from']) {
                $clientObj->SetFilterRange ( 'year', $values['year_from'], 100000000000);
            } elseif($values['year_to']) {
                $clientObj->SetFilterRange ( 'year', 0, $values['year_to']);
            }

            if($values['price_from'] && $values['price_to']) {
                $clientObj->SetFilterRange ( 'price_usd', $values['price_from'], $values['price_to']);
            } elseif($values['price_from']) {
                $clientObj->SetFilterRange ( 'price_usd', $values['price_from'], 100000000000);
            } elseif($values['price_to']) {
                $clientObj->SetFilterRange ( 'price_usd', 0, $values['price_to']);
            }

            if($values['run_from'] && $values['run_to']) {
                $clientObj->SetFilterRange ( 'run', $values['run_from'], $values['run_to']);
            } elseif($values['run_from']) {
                $clientObj->SetFilterRange ( 'run', $values['run_from'], 100000000000);
            } elseif($values['run_to']) {
                $clientObj->SetFilterRange ( 'run', 0, $values['run_to']);
            }

            if($values['period']) {
                switch($values['period']) {
                    case 1:
                        $clientObj->SetFilterRange ( 'set_date', time() - 7*24*60*60, time());
                        break;
                    case 2:
                        $clientObj->SetFilterRange ( 'set_date', time() - 31*24*60*60, time());
                        break;
                    default: break;
                }
            }

            if($values['sort']) {
                switch($values['sort']) {
                    case 1: $sort = 'price_usd ASC, '; break;
                    case 2: $sort = 'run ASC, '; break;
                    default: $sort = ''; break;
                }
            }

            $clientObj->setSortMode(SPH_SORT_EXTENDED, $sort.'@id ASC');

            $res = $clientObj->Query($values['query'], "all");
            print_r($res['matches']);
            echo '<br /><br />';
            echo 'total_found: '.$res['total_found'];
            echo '<br />';
            echo 'query time: '.$res['time'];

        }
        return array(
            'form'              => $form,
        );
    }

    private function getClient($client)
    {
        $cl = new SphinxClient();
        $confs = Config::get('sphinxClients@sphinx');
        if (!isset($confs[$client])) {
            throw new SphinxSearchException("Client name '{$client}' is not defined", 1);
        }
        $conf = $confs[$client];

        $cl->setServer($conf['host'], $conf['port']);

        $cl->setConnectTimeout(30);
        $cl->setMaxQueryTime(30000);

        return $cl;
    }
}

?>