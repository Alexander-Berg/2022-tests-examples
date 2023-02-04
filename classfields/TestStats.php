<?php

namespace builder\classes;

class TestStats
{

    private $server_name;

    private $project;

    private $run_id;

    private $last_build;

    private $build_list = 'job/#project#/api/json';

    private $build_report = 'job/#project#/#build#/testReport/(root)/api/json';

    private $build_test_report = 'job/#project#/#build#/testReport/(root)/#test#/api/json';

    public function __construct($server_name, $project) {
        $this->server_name = $server_name;
        $this->project = $project;
    }

    public function getTestStats() {

        try {
            $needMake = $this->checkLastStat();

            if ($needMake) {
                $this->startMakeStat();

                $this->deleteOldStats();

                $builds = $this->getBuildList();

                foreach ($builds as $build) {
                    $this->getReportByBuild($build);
                    $this->last_build = $build;
                    $this->addLastBuild();
                }

                $this->stopMakeStat();
            }
        } catch (\Exception $e) {
            echo $e->getMessage(), "\n";
        }
    }

    private function checkLastStat() {

        $return = true;

        $db = \builder\classes\DbHelper::getInstance('webtests_stats', false, 'zone1');

        $sql = 'SELECT MAX(last_build) as last_build, MAX(end) as end FROM webtests_stats.builder_runs WHERE project=#project';
        $last_stat = $db->fetch($db->q($sql, array('project' => $this->project)));
        $this->last_build = $last_stat['last_build'];

        if ($last_stat['end'] > date('Y-m-d H:i:s', strtotime('-3 hour'))) {
            $return = false;
        }

        return $return;
    }

    private function startMakeStat() {
        $db = \builder\classes\DbHelper::getInstance('webtests_stats', false, 'zone1');

        $sql = 'INSERT INTO webtests_stats.builder_runs (project, start) VALUES (#project, now())';
        $db->q($sql, array('project' => $this->project));
        $this->run_id = $db->getInsertId();
    }

    public function deleteOldStats() {
        $db = \builder\classes\DbHelper::getInstance('webtests_stats', false, 'zone1');

        $sql = 'DELETE FROM webtests_stats.builder_runs WHERE end < (now() - interval 2 week)';
        $db->q($sql);

        $sql = 'DELETE FROM webtests_stats.test_stats WHERE date < (now() - interval 2 week)';
        $db->q($sql);
    }

    private function stopMakeStat() {
        $db = \builder\classes\DbHelper::getInstance('webtests_stats', false, 'zone1');
        if ($this->run_id) {
            $sql = 'UPDATE webtests_stats.builder_runs SET end=now(), last_build=#last_build WHERE id=#id';
            $db->q($sql, array('last_build' => $this->last_build, 'id' => $this->run_id));
        } else {
            $sql = 'INSERT INTO webtests_stats.builder_runs (project, start, end, last_build) VALUES (#project, now(), now(), #last_build)';
            $db->q($sql, array('project' => $this->project, 'last_build' => $this->last_build));
        }
    }

    private function addLastBuild() {
        $db = \builder\classes\DbHelper::getInstance('webtests_stats', false, 'zone1');

        if ($this->run_id) {
            $sql = 'UPDATE webtests_stats.builder_runs SET last_build=#last_build WHERE id=#id';
            $db->q($sql, array('last_build' => $this->last_build, 'id' => $this->run_id));
        } else {
            $sql = 'INSERT INTO webtests_stats.builder_runs (project, start, last_build) VALUES (#project, now(), #last_build)';
            $db->q($sql, array('project' => $this->project, 'last_build' => $this->last_build));
            $this->run_id = $db->getInsertId();
        }
    }

    private function getBuildList() {

        $builds = array();

        $result = $this->sendCurlRequestJson($this->server_name . $this->build_list, array('project' => $this->project));
        if (!empty($result->builds)) {

            $builds = array();

            foreach ($result->builds as $build) {
                if ($build <= $this->last_build) {
                    break;
                }
                $builds[] = $build->number;
            }
        } else {
            echo "ниче нет";
        }

        return array_reverse($builds);
    }

    private function getReportByBuild($build) {
        $tests = array();

        $params = array(
            'project' => $this->project,
            'build'   => $build
        );

        $result = $this->sendCurlRequestJson($this->server_name . $this->build_report, $params, false);

        if (!$result) {
            return false;
        } else {

            if (!empty($result->child)) {
                foreach ($result->child as $child) {
                    $tests[] = $child->name;
                }
            }

            $last_test_class = '';
            $last_duration = 0;

            foreach ($tests as $test) {

                $test_class = (strpos($test, '::')) ? strstr($test, '::', true) : $test;

                if ($last_test_class && $last_test_class != $test_class) {
                    $this->saveTestClassStat($build, $last_test_class, $last_duration);
                    $last_duration = 0;
                }

                $last_duration += $this->getTestClassDuration($build, $test);
                $last_test_class = $test_class;
            }

            $this->saveTestClassStat($build, $last_test_class, $last_duration);
        }

        return $tests;
    }

    private function getTestClassDuration($build, $test) {

        $duration = 0;

        $test_way = str_replace(array("\\", "::"), array("_", "__"), $test);

        $params = array(
            'project' => $this->project,
            'build'   => $build,
            'test'    => $test_way
        );

        $result = $this->sendCurlRequestJson($this->server_name . $this->build_test_report, $params, false);

        if (!empty($result->child)) {
            foreach ($result->child as $test_i) {
                $duration += $test_i->duration;
            }
        }

        return $duration;
    }

    private function saveTestClassStat($build, $test_class, $duration) {
        $db = \builder\classes\DbHelper::getInstance('webtests_stats', false, 'zone1');

        $sql = 'INSERT INTO webtests_stats.test_stats (test_class, duration, date, build, run_id) VALUES (#test_class, #duration, now(), #build, #run_id)';
        $db->q($sql, array('test_class' => $test_class, 'duration' => $duration, 'build' => $build, 'run_id' => $this->run_id));
    }

    private function sendCurlRequestJson($url, $params = array(), $not_empty = true) {

        if ($params) {
            $search = array();
            $replace = array();

            foreach ($params as $key => $param) {
                $search[] = '#' . $key . '#';
                $replace[] = $param;
            }

            $url = str_replace($search, $replace, $url);
        }

        $curl = curl_init();
        curl_setopt($curl, CURLOPT_URL, $url);
        curl_setopt($curl, CURLOPT_CONNECTTIMEOUT, 3);
        curl_setopt($curl, CURLOPT_TIMEOUT, 5);
        curl_setopt($curl, CURLOPT_HEADER, 0);
        curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);

        $result = curl_exec($curl);

        if (curl_errno($curl)) {
            throw new \Exception('Ошибка при работе CURL с ' . $url . '(' . curl_error($curl) . ')');
        }
        curl_close($curl);

        if (($result && !strpos($result, "window.location.replace('..');") && !strpos($result, '>Not found<')) || $not_empty == true) {

            $result = json_decode($result);

            if (json_last_error() != JSON_ERROR_NONE) {
                throw new \Exception('Невозможно декодировать JSON ' . $url . ' -- ' . $result);
            }

        } else {
            $result = false;
        }

        return $result;
    }
}

?>