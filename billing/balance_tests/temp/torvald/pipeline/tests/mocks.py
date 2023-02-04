# -*- coding: utf-8 -*-

class Comment(object):
    def __init__(self, text):
        self.text = text


class Comments(object):
    def __init__(self):
        self.comments = []

    def get_all(self):
        return self.comments

    def add(self, text):
        comment = Comment(text)
        self.comments.append(comment)

class StartrekTicket(object):

    def __init__(self, state, alarm=None):
        self.otrsTicket = state
        self.crashId = alarm
        self.comments = Comments()

    def update(self, *args, **kwargs):
        if 'crashId' in kwargs:
            self.crashId = kwargs['crashId']
        if 'otrsTicket' in kwargs:
            self.otrsTicket = kwargs['otrsTicket']
        if 'comment' in kwargs:
            self.comments.add(kwargs['comment'])