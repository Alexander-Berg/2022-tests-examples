const csv2json = require('./csv2json');

const fs = require('fs');
const path = require('path');

const questionsCsv = fs.readFileSync(path.resolve(__dirname, '../mocks/questions.csv'), 'utf8');

it('должен сформировать объект c вопросами из csv файла', () => {
    expect(csv2json(questionsCsv)).toMatchSnapshot();
});
