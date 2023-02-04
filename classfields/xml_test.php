#!/usr/bin/php -q
<?php
$shell_script = __FILE__;
require_once(dirname(__FILE__).'/../../lib5/common.php');

$_GET = array();
$_GET['event_log'] = 0;

ignore_user_abort(true);
set_time_limit(0);
header ('Content-type: text/html; charset=utf-8');

Db::q("SET group_concat_max_len := @@max_allowed_packet");
$res = Db::q("SELECT * FROM all5.sale_property_names");
$data = Db::fetchQuery($res, 'id');

echo '<?xml version="1.0" encoding="utf-8"?>';
echo '<sphinx:docset>';
echo '<sphinx:schema>
<sphinx:field name="description"/>
<sphinx:field name="address"/>
<sphinx:attr name="id" type="int" bits="16" default="1"/>
<sphinx:attr name="client_id" type="int" bits="16" default="1"/>
<sphinx:attr name="user_id" type="int" bits="16" default="1"/>
<sphinx:attr name="category_id" type="int" bits="16" default="1"/>
<sphinx:attr name="section_id" type="int" bits="16" default="1"/>
<sphinx:attr name="mark_id" type="int" bits="16" default="1"/>
<sphinx:attr name="mark_type" type="int" bits="16" default="1"/>
<sphinx:attr name="group_id" type="int" bits="16" default="1"/>
<sphinx:attr name="model_id" type="int" bits="16" default="1"/>
<sphinx:attr name="color_id" type="int" bits="16" default="1"/>
<sphinx:attr name="expire_date" type="timestamp"/>
<sphinx:attr name="set_date" type="timestamp"/>
<sphinx:attr name="year" type="int" bits="16" default="1"/>
<sphinx:attr name="photo" type="int" bits="16" default="1"/>
<sphinx:attr name="price_usd" type="int" bits="16" default="1"/>
<sphinx:attr name="status" type="int" bits="16" default="1"/>';

$skip_arr = array(
    "price",
    "price_file_id",
    "contact_id",
    "phone_set_id",
    "sms_id",
    "sms_color",
    "client_color",
    "class_id",
    "create_date",
    "model_name",
    "modification_id",
    "modification_name",
    "ip",
    "deleted_time",
    "deleted_by_user",
    "deleted_reason_alias",
    "deleted_reason_comment"
);


$skip_arr2 = array();



foreach($data as $key => $info) {
    if($info['search_type'] == 'no') {
        $skip_arr2[] = $info['alias'];
        continue;
    }
    if($info['search_type'] == 'field') {
        echo '<sphinx:field name="'.$info['alias'].'"/>';
    } elseif($info['search_type'] == 'attr') {
        echo '<sphinx:attr name="'.$info['alias'].'" type="'.$info['field_type'].'"/>';
    }
}

echo '</sphinx:schema>';





$num = Db::getRow("SELECT Count(id) count FROM all5.sale");
$iterations = floor($num['count']/50000);
$num_prop_tables = ceil($num['count']/100000);

$a = 1;
for($j = 0; $j <= $iterations; $j++) {

    $sql = "SELECT
                sale.*, UNIX_TIMESTAMP(sale.set_date) set_date, UNIX_TIMESTAMP(sale.expire_date) expire_date,
                CASE ";
    for($k = 1; $k <= $num_prop_tables; $k ++) {
        $postfix = $k == 1 ? '' : '_'.$k;
        $sql .= "WHEN sale.id < ".($k * 100000)." THEN
                        GROUP_CONCAT(sale_property_names{$postfix}.name ,' !=! ', sale_property_values{$postfix}.property_value  SEPARATOR ' !,! ')";
    }
    $sql .= "    END as props
            FROM all5.sale";
    for($k = 1; $k <= $num_prop_tables; $k ++) {
        $postfix = $k == 1 ? '' : '_'.$k;
        $sql .= " LEFT JOIN all5.sale_property_values{$postfix} ON sale_property_values{$postfix}.sale_id = sale.id AND sale.id BETWEEN ".(($k-1)*100000 + 1)." AND ".($k*100000 - 1)."
                LEFT JOIN all5.sale_property_names AS sale_property_names{$postfix} ON sale_property_values{$postfix}.property_id = sale_property_names{$postfix}.id";
    }

    $sql .= "
            GROUP BY sale.id
            ORDER BY NULL LIMIT ".($j * 50000).", 50000";

    /*
    $sql .= "
            GROUP BY sale.id
            ORDER BY NULL LIMIT 159200, 100";
    */



    /*
    $sql = "SELECT sale.*, GROUP_CONCAT(name ,' = ', property_value  SEPARATOR ', ') props FROM all5.sale
            LEFT JOIN (
                SELECT
                all5.sale_property_values ON sale_property_values.sale_id = sale.id AND sale.sale_id BETWEEN 1 AND 99999
            ) as sale_property_values
            LEFT JOIN all5.sale_property_names ON sale_property_values.property_id = sale_property_names.id
            GROUP BY sale.id
            ORDER BY NULL LIMIT 250001, 100";
    */

    $res = Db::q($sql);
    $arr = Db::fetchQuery($res, 'id');
    foreach($arr as $key => $val) {
        echo '<sphinx:document id="'.$a.'">';
        $props = explode(" !,! ", $val['props']);
        foreach($props as $p_key => $p_val) {
            if(strlen(trim($p_val))) {
                $p_name = false;
                $p_value = false;
                list($p_name, $p_value) = explode(' !=! ', $p_val);
                if(in_array($p_name, $skip_arr2)) continue;
                $s_cdata = $e_cdata = '';
                if($p_name == 'complect_name') {
                    $s_cdata = '<![CDATA[';
                    $e_cdata = ']]>';
                }
                echo '<'.$p_name.'>'.$s_cdata.trim(preg_replace('/[^\x09\x0A\x0D\x20-\xD7FF\xE000-\xFFFD]/', ' ',  $p_value)).$e_cdata.'</'.$p_name.'>';
                if($a <= 100 && $p_name == 'run') {
                    file_put_contents('/home/zotov/logs/sph_log.txt', '<'.$p_name.'>'.$s_cdata.trim(preg_replace('/[^\x09\x0A\x0D\x20-\xD7FF\xE000-\xFFFD]/', ' ',  $p_value)).$e_cdata.'</'.$p_name.">\n", FILE_APPEND);
                }
            }
        }
        unset($val['props']);
        //if($val['id'] == 55333) $val['description'] = '';
        foreach($val as $s_key => $s_val) {
            if(in_array($s_key, $skip_arr)) continue;
            $s_cdata = $e_cdata = '';
            if($s_key == 'description' || $s_key == 'address' || $s_key == 'model_name' || $s_key == 'modification_name') {
                $s_cdata = '<![CDATA[';
                $e_cdata = ']]>';
            }
            echo '<'.$s_key.'>'.$s_cdata.trim(preg_replace('/[^\x09\x0A\x0D\x20-\xD7FF\xE000-\xFFFD]/', ' ',  $s_val)).$e_cdata.'</'.$s_key.'>';
        }

        echo '</sphinx:document>';
        $a++;
    }
    unset($arr);
    flush();
}

echo '</sphinx:docset>';
?>