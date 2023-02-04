process.title = 'pages-server';
const express = require('express');
const app = express();
app.use(express.static('./'));
/*app.get('/init.js', (req, res) => {
  res.redirect('https://api-maps.tst.c.maps.yandex.ru/2.1.74/?' + req.url.split('?')[1])
})*/
app.listen(8080);
