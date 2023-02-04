import unittest

from markupsafe import Markup

from genisys.web import filters


class FormatDiffTextTestCase(unittest.TestCase):
    def test(self):
        text1 = """\
she said "fairwell"
he watched after he for a while
a night passed for her
and three thousand years for him
meanwhile dozen of empires
flourished and collapsed to the obscurity
but someone get merried
and someone just so
"""
        text2 = """\
and now for something completelly different
she said "fairwell"
a night passed for her
and three thousand years for him
meanwhile dozen of empires
this line is new
flourished and collapsed to the gloom
but someone get merried
and someone
"""
        diff1 = filters.format_text_diff(text1, text2)
        self.assertIsInstance(diff1, Markup)
        expected1 = """\
she said &#34;fairwell&#34;
<span class="unique">he watched after he for a while
</span>a night passed for her
and three thousand years for him
meanwhile dozen of empires
flourished and collapsed to the <span class="changed">obscurity</span>
but someone get merried
and <span class="changed">someone just so</span>
"""
        self.assertEquals(str(diff1), expected1)

        diff2 = filters.format_text_diff(text2, text1)
        expected2 = """\
<span class="unique">and now for something completelly different
</span>she said &#34;fairwell&#34;
a night passed for her
and three thousand years for him
meanwhile dozen of empires
<span class="unique">this line is new
</span>flourished and collapsed to the <span class="changed">gloom</span>
but someone get merried
and <span class="changed">someone</span>
"""
        self.assertEquals(str(diff2), expected2)

    def test_word_comparison(self):
        text1 = "tweenkle twnkle little i wonder whaaat you are"
        text2 = "twinkle twinkle little star how i wonder what you are"
        diff1 = filters.format_text_diff(text1, text2)
        expected1 = """\
tw<span class="changed">ee</span>nkle \
twnkle little i wonder \
wha<span class="unique">aa</span>t you are
"""
        self.assertEquals(str(diff1), expected1)

        diff2 = filters.format_text_diff(text2, text1)
        expected2 = """\
tw<span class="changed">i</span>nkle tw<span class="unique">i</span>nkle \
little <span class="unique">star how </span>i wonder what you are
"""
        self.assertEquals(str(diff2), expected2)

    def test_indentation(self):
        text1 = """\
something:
    else: 1
foo:
    bar:
        baz: 1"""
        text2 = """\
something:
    else: 1"""
        diff1 = filters.format_text_diff(text1, text2)
        expected1 = """\
something:
    else: 1
<span class="unique">foo:
    bar:
        baz: 1
</span>"""
        self.assertEquals(str(diff1), expected1)

        diff2 = filters.format_text_diff(text2, text1)
        expected2 = 'something:\n    else: 1\n'
        self.assertEquals(str(diff2), expected2)

    def test_indentation2(self):
        text1 = """\
config:
   client:
      Transport:
         Netlibus: true
""" + "         " + """
foo:
   bar:
      baz:
         quux: 1"""
        text2 = """\
config:
   client:
      Transport:
         Netlibus: true
"""
        expected1 = """\
config:
   client:
      Transport:
         Netlibus: true
<span class="unique">""" + "         " + """
foo:
   bar:
      baz:
         quux: 1
</span>"""
        diff1 = filters.format_text_diff(text1, text2)
        self.maxDiff = None
        self.assertEquals(str(diff1), str(expected1))
