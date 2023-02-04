package brexp

import (
	"testing"
)

var testStrings = []struct {
	s string
	r []string
}{
	{
		"ab{{{ddf{a,s,p{2,3}}{dd,qwe}",
		[]string{
			"ab{{{ddfadd",
			"ab{{{ddfaqwe",
			"ab{{{ddfsdd",
			"ab{{{ddfsqwe",
			"ab{{{ddfp2dd",
			"ab{{{ddfp2qwe",
			"ab{{{ddfp3dd",
			"ab{{{ddfp3qwe",
		},
	},
	{
		"cd{{k,5}df,s}{{f,4}f",
		[]string{
			"cdkdf{ff",
			"cdkdf{4f",
			"cd5df{ff",
			"cd5df{4f",
			"cds{ff",
			"cds{4f",
		},
	},
	{
		"a{a..d..f}x",
		[]string{
			"a{a..d..f}x",
		},
	},
	{
		"-035..038",
		[]string{
			"-035..038",
		},
	},
	{
		"sf}}fr{2,3}vb{ev,dd}gg}}",
		[]string{
			"sf}}fr2vbevgg}}",
			"sf}}fr2vbddgg}}",
			"sf}}fr3vbevgg}}",
			"sf}}fr3vbddgg}}",
		},
	},
	{
		"as{2,1}d{234,sdf}df}}",
		[]string{
			"as2d234df}}",
			"as2dsdfdf}}",
			"as1d234df}}",
			"as1dsdfdf}}",
		},
	},
	{
		"as{{2,1}d{234,sdf}}df{}",
		[]string{
			"as{2d234}df{}",
			"as{2dsdf}df{}",
			"as{1d234}df{}",
			"as{1dsdf}df{}",
		},
	},
	{
		"0}}a{b,c{d,e}f{01..03}g,}{}h,{{-01..-6..-3}i,j{s..w..2}}{k}m{{{",
		[]string{
			"0}}ab{}h,-01i{k}m{{{",
			"0}}ab{}h,-04i{k}m{{{",
			"0}}ab{}h,js{k}m{{{",
			"0}}ab{}h,ju{k}m{{{",
			"0}}ab{}h,jw{k}m{{{",
			"0}}acdf01g{}h,-01i{k}m{{{",
			"0}}acdf01g{}h,-04i{k}m{{{",
			"0}}acdf01g{}h,js{k}m{{{",
			"0}}acdf01g{}h,ju{k}m{{{",
			"0}}acdf01g{}h,jw{k}m{{{",
			"0}}acdf02g{}h,-01i{k}m{{{",
			"0}}acdf02g{}h,-04i{k}m{{{",
			"0}}acdf02g{}h,js{k}m{{{",
			"0}}acdf02g{}h,ju{k}m{{{",
			"0}}acdf02g{}h,jw{k}m{{{",
			"0}}acdf03g{}h,-01i{k}m{{{",
			"0}}acdf03g{}h,-04i{k}m{{{",
			"0}}acdf03g{}h,js{k}m{{{",
			"0}}acdf03g{}h,ju{k}m{{{",
			"0}}acdf03g{}h,jw{k}m{{{",
			"0}}acef01g{}h,-01i{k}m{{{",
			"0}}acef01g{}h,-04i{k}m{{{",
			"0}}acef01g{}h,js{k}m{{{",
			"0}}acef01g{}h,ju{k}m{{{",
			"0}}acef01g{}h,jw{k}m{{{",
			"0}}acef02g{}h,-01i{k}m{{{",
			"0}}acef02g{}h,-04i{k}m{{{",
			"0}}acef02g{}h,js{k}m{{{",
			"0}}acef02g{}h,ju{k}m{{{",
			"0}}acef02g{}h,jw{k}m{{{",
			"0}}acef03g{}h,-01i{k}m{{{",
			"0}}acef03g{}h,-04i{k}m{{{",
			"0}}acef03g{}h,js{k}m{{{",
			"0}}acef03g{}h,ju{k}m{{{",
			"0}}acef03g{}h,jw{k}m{{{",
			"0}}a{}h,-01i{k}m{{{",
			"0}}a{}h,-04i{k}m{{{",
			"0}}a{}h,js{k}m{{{",
			"0}}a{}h,ju{k}m{{{",
			"0}}a{}h,jw{k}m{{{",
		},
	},
}

func TestGetTokens(t *testing.T) {
	for _, s := range testStrings {
		e := Expand(s.s)
		for i, es := range e {
			if s.r[i] != es {
				t.Errorf("Expand failed (%s): want = %s got = %s", s.s, s.r[i], es)
			}
		}
		if len(e) != len(s.r) {
			t.Errorf("Expand failed bad slice length (%s): want = %d got = %d", s.s, len(e), len(s.r))
		}
	}
}

func TestExpandWithSpaces(t *testing.T) {
	s := "ab{{{ddf{a,s,p{2,3}}{dd,qwe} qwe{a,b}"
	r := []string{
		"ab{{{ddfadd",
		"ab{{{ddfaqwe",
		"ab{{{ddfsdd",
		"ab{{{ddfsqwe",
		"ab{{{ddfp2dd",
		"ab{{{ddfp2qwe",
		"ab{{{ddfp3dd",
		"ab{{{ddfp3qwe",
		"qwea",
		"qweb",
	}
	e := ExpandWithSpaces(s)
	if len(e) != len(r) {
		t.Errorf("Expand failed bad slice length (%s): want = %d got = %d", s, len(e), len(r))
	}
	for i, es := range e {
		if r[i] != es {
			t.Errorf("Expand failed (%s): want = %s got = %s", s, r[i], es)
		}
	}
}

func BenchmarkGetTokens(b *testing.B) {
	b.ResetTimer()
	for ix := 0; ix < b.N; ix++ {
		for _, s := range testStrings {
			_ = Expand(s.s)
		}
	}
	b.ReportMetric(float64(len(testStrings)), "len")
	b.ReportAllocs()
}
