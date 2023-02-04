var exec = require('child_process').spawnSync;

let text = process.argv[2];

console.log(text);

// Разбиваем посылаемый текст на блоки по 150 символов (в незакодированном виде)
// Потому что adb shell input text не пропускает больше килобайта (ориентировочно) за раз.

text = text.slice(2, text.length-1);

const limit = 150;
let bs = 0;
let start = 0;
let result = [];
for(let i=0; i<text.length; i++) {
  if(text[i] === '\\') bs++;
  if(bs >= limit && text[i] === '\\') {
    result.push(text.slice(start, i));
    start = i;
    bs = 0;
  }
  if(i === text.length - 1) {
      result.push(text.slice(start, text.length));
  }
}

//console.log(result);

result = result.map(e => '$\'' + e + '\'')

//console.log(result);

result.forEach(e => {
  exec('adb', ['shell', 'input', 'text', e]);
})
