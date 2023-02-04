<?php
$from = $argv[1];
$to = $argv[2];
$headers = 'From: '.$from . "\r\n" . 'Reply-To: '.$from . "\r\n";
mail($to, 'hello', 'message', $headers);
?>