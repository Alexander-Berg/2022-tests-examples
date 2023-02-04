<?php
require_once 'PHPUnit/Framework.php';
require_once dirname(__FILE__).'/../../lib5/common.php';
$_GET['debug'] = 0;
class CommentCommonTest extends UnitTestCase
{
    public function testAddComment()
    {
        echo "\r\n" . __METHOD__;     
        $project_id        = 1;  
        $subject_group_id  = 0;
        $text              = 'Новый комментарий';
        $parent_comment_id = false;
        $deny_on_mat       = false;
        $subject_id        = 2255;

        // Создаем запись в сессии о клиенте:
        $author_id = 2977858;
        Session::set('user_id', $author_id);
        
        // Получаем набор данных добавляемого комментария
        $comments = Model::getData(
            'comments:addComment@comments',
            array(
                'project_id'        => $project_id,
                'subject_group_id'  => $subject_group_id,
                'subject_id'        => $subject_id,
                'text'              => $text,
                'parent_comment_id' => $parent_comment_id,
                'deny_on_mat'       => $deny_on_mat
            )
        );
        // Проверка на тип возвращаемого методом addComment
        $this->assertTrue(is_array($comments), "Метод addComments вернул не массив данных созданного комментария");
        // Проверка добавленного комментария
        $this->assertTrue($comments['statusBool'], "Комментарий с пустым parent_id не добавлен по причине: " . $comments["statusString"]);
        Model::getData(
                'comments:removeCommentReal@comments',
                array(
                    'comment_id' => $comments['comment']['id']
                )
        );
        
        $parent_comment_id = 508931;
        // Получаем набор данных добавляемого комментария
        $comments = Model::getData(
            'comments:addComment@comments',
            array(
                'project_id'        => $project_id,
                'subject_group_id'  => $subject_group_id,
                'subject_id'        => $subject_id,
                'text'              => $text,
                'parent_comment_id' => $parent_comment_id,
                'deny_on_mat'       => $deny_on_mat
            )
        );
        
        // Проверка на тип возвращаемого методом addComment
        $this->assertTrue(is_array($comments), "Метод addComments вернул не массив данных созданного комментария");
        // Проверка добавленного комментария
        $this->assertTrue($comments['statusBool'], "Комментарий с указанным parent_id не добавлен по причине: " . $comments["statusString"]);
        Model::getData(
                'comments:removeCommentReal@comments',
                array(
                    'comment_id' => $comments['comment']['id']
                )
        );
    }
    
    public function testAddCommentPOST() {
        echo "\r\n" . __METHOD__;   
        $_POST['comment_text']      = 'Новый POST комментарий';
        $_POST['parent_comment_id'] = false;

        $project_id        = 1;  
        $subject_group_id  = 0;
        $deny_on_mat       = false;
        $subject_id        = 2255;
        
        $parent_comment_id  = $_POST['parent_comment_id'];
        $text               = $_POST['comment_text'];
        $comments = Model::getData(
            'comments:addComment@comments',
            array(
                'project_id'        => $project_id,
                'subject_group_id'  => $subject_group_id,
                'subject_id'        => $subject_id,
                'text'              => $text,
                'parent_comment_id' => $parent_comment_id,
                'deny_on_mat'       => $deny_on_mat
            )
        );
        // Проверка на тип возвращаемого методом addComment
        $this->assertTrue(is_array($comments), "Метод addComments вернул не массив данных созданного комментария");
        // Проверка добавленного комментария
        $this->assertTrue($comments['statusBool'], "Комментарий не добавлен по причине: " . $comments["statusString"]);
        Model::getData(
                'comments:removeCommentReal@comments',
                array(
                    'comment_id' => $comments['comment']['id']
                )
        );
    }
    
    public function testEditComment()
    {
        echo "\r\n" . __METHOD__;   
        $comment_id = 508930;
        $text       = "Новый текст комментария";
        $comments = Model::getData(
            'comments:editComment@comments',
            array(
                'comment_id' => $comment_id,
                'text'       => $text 
            )
        );
        $text_new = Model::getData(
            'comments:getComment@comments',
            array(
                'comment_id' => $comment_id
            )
        );
        $text_new = $text_new["text"];
        $this->assertEquals($text_new, $text, "Комментарий не был обновлен");
    }
    
    public function testGetCommentsBoxHTML()
    {
        echo "\r\n" . __METHOD__;
        $subject_id       = 2255;
        $subject_group_id = 0;
        $settings         = array();
        $projects_arr     = Config::get('project_alias_2_id');
        foreach ($projects_arr as $project_alias => $project_id) {
            $comments = Model::getData(
                'comments:getCommentsBoxHTML@comments',
                array(
                    "project_alias"    => $project_alias,
                    "subject_id"       => $subject_id,
                    "subject_group_id" => $subject_group_id,
                    "settings"         => $settings
                )
            );
            if (!empty($comments)) {
                $return = true;
            } else {
                $return = false;    
            }
            $this->assertTrue($return, "Не создана форма добавления комментария");
        }
    }
}         
?>
