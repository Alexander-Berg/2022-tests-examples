'use strict';

const assert = require('power-assert');
const text = require('../../app/utils/text');

suite('text', () => {
  test('.mergeCallstack() — should serialize it to the single line', () => {
    assert.equal(text.mergeCallstack('HTTPError: 400 Bad Request ← POST https://github.yandex-team.ru/api/v3/repos/partner/yharnam/issues/6/comments\n    at /var/www/node_modules/got/index.js:105:11\n    at BufferStream.<anonymous> (/var/www/node_modules/got/node_modules/read-all-stream/index.js:64:3)\n    at emitNone (events.js:72:20)\n    at BufferStream.emit (events.js:166:7)\n    at finishMaybe (/var/www/node_modules/got/node_modules/readable-stream/lib/_stream_writable.js:511:14)\n    at afterWrite (/var/www/node_modules/got/node_modules/readable-stream/lib/_stream_writable.js:390:3)\n    at doNTCallbackMany (node.js:463:18)\n    at process._tickCallback (node.js:361:17)'),
      'HTTPError: 400 Bad Request ← POST https://github.yandex-team.ru/api/v3/repos/partner/yharnam/issues/6/comments ← at /var/www/node_modules/got/index.js:105:11 ← at BufferStream.<anonymous> (/var/www/node_modules/got/node_modules/read-all-stream/index.js:64:3) ← at emitNone (events.js:72:20) ← at BufferStream.emit (events.js:166:7) ← at finishMaybe (/var/www/node_modules/got/node_modules/readable-stream/lib/_stream_writable.js:511:14) ← at afterWrite (/var/www/node_modules/got/node_modules/readable-stream/lib/_stream_writable.js:390:3) ← at doNTCallbackMany (node.js:463:18) ← at process._tickCallback (node.js:361:17)');
  });
});
