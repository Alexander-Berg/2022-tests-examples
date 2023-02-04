<?php
require_once 'PHPUnit/Framework.php';
require_once dirname(__FILE__).'/../../lib5/common.php';
$_GET['debug'] = 0;
class CommentAjaxCommonTest extends UnitTestCase
{
    public function testAddComment()
    {
        $_POST['text']              =  'Новый комментарий AJAX';
        $_POST['parent_comment_id'] = 0;
        $_POST['project_id']        = 1;
        $_POST['subject_group_id']  = 0;
        $_POST['subject_id']        = 2255;
        $_POST['lastRefresh']       = 12324124;
        $_POST['comment_id']        = false;
        $_REQUEST['token']          = false;       
        $_REQUEST['deny_on_mat']    = false;       

        // Создаем запись в сессии о клиенте:
        $author_id = 2977858;
        Session::set('user_id', $author_id);
        
        $comments = Model::getData(
            'Comment_Ajax:addComment@comments',
            array()
        );
        $comments = new SimpleXMLElement($comments["data"]);
        $this->assertEquals((string) $comments->status, "ok", "Комментарий не добавлен");
    }
    
}

?>
