FakeCommentMetaData = {
            (1120000000016603,2,3): {
                    'parent_id': 0,
                    'parent_author_id': 1120000000016603
                }
            }

class FakeCommentMeta(object):

    def __init__(self, feed_id, item_no, comment_id):
        self.feed_id = feed_id
        self.item_no = item_no
        self.comment_id = comment_id

    def __getattr__(self, attr):
        return FakeCommentMetaData[(self.feed_id, self.item_no, self.comment_id)]\
                [attr]

class FakeResizer():

    def __init__(self, callback=None):
        self.callback = callback

    def resize(self, url, width):
        if self.callback:
            self.callback(url, width)
        return url

