package selector

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"regexp"
	"testing"
)

var aorbExp, _ = regexp.Compile("a*|b*")
var abExp, _ = regexp.Compile("a.*b")

func TestParse(t *testing.T) {
	tests := []struct {
		name      string
		selector  string
		selectors *Selectors
	}{
		{name: "All selector", selector: "", selectors: &Selectors{[]Selector{&All{}}}},
		{name: "Any selector", selector: "lbl=\"*\"", selectors: &Selectors{[]Selector{&Any{key: "lbl"}}}},
		{name: "Absent selector", selector: "lbl=\"-\"", selectors: &Selectors{[]Selector{&Absent{key: "lbl"}}}},
		{name: "Glob selector", selector: "lbl=\"a*|b*\"", selectors: &Selectors{[]Selector{&Glob{key: "lbl", values: []string{"a*", "b*"}}}}},
		{name: "NotGlob selector", selector: "lbl!=\"a*|b*\"", selectors: &Selectors{[]Selector{&NotGlob{key: "lbl", values: []string{"a*", "b*"}}}}},
		{name: "Regexp selector", selector: "lbl=~\"a*|b*\"", selectors: &Selectors{[]Selector{&Regexp{key: "lbl", regexp: aorbExp}}}},
		{name: "NotRegexp selector", selector: "lbl!~\"a*|b*\"", selectors: &Selectors{[]Selector{&NotRegexp{key: "lbl", regexp: aorbExp}}}},
		{name: "Exact selector", selector: "lbl==\"ab\"", selectors: &Selectors{[]Selector{&Exact{key: "lbl", value: "ab"}}}},
		{name: "NotExact selector", selector: "lbl!==\"ab\"", selectors: &Selectors{[]Selector{&NotExact{key: "lbl", value: "ab"}}}},
		{name: "Multiple selector", selector: "lbl=\"a*|b*\", lbl=\"*\", lbl!==\"ab\", lbl!~\"a*|b*\"",
			selectors: &Selectors{[]Selector{
				&Glob{key: "lbl", values: []string{"a*", "b*"}},
				&Any{key: "lbl"},
				&NotExact{key: "lbl", value: "ab"},
				&NotRegexp{key: "lbl", regexp: aorbExp},
			}}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			selectors, _ := ParseSelectors(test.selector)

			assert.Equal(t, len(test.selectors.selectors), len(selectors.selectors))
			for i, selector := range test.selectors.selectors {
				assert.Equal(t, selector, selectors.selectors[i])
			}
		})
	}
}

func TestMatch(t *testing.T) {
	tests := []struct {
		name     string
		selector Selector
		match    []map[string]string
		notMatch []map[string]string
	}{
		{name: "All selector", selector: &All{}, match: []map[string]string{{"l": "a"}, {"l2": "b"}, {}}},
		{name: "Any selector",
			selector: &Any{key: "lbl"},
			match:    []map[string]string{{"lbl": "a1"}, {"l2": "b", "lbl": "a2"}},
			notMatch: []map[string]string{{"lbl1": "a1"}, {"l2": "b", "lbl2": "a2"}, {}},
		},
		{name: "Absent selector",
			selector: &Absent{key: "lbl"},
			match:    []map[string]string{{"lbl1": "a1"}, {"l2": "b", "lbl2": "a2"}, {}},
			notMatch: []map[string]string{{"lbl": "a1"}, {"l2": "b", "lbl": "a2"}},
		},
		{name: "Glob selector",
			selector: &Glob{key: "lbl", values: []string{"a*", "b*", "c?e"}},
			match:    []map[string]string{{"lbl": "a1"}, {"l2": "b", "lbl": "b2"}, {"lbl": "cde"}},
			notMatch: []map[string]string{{"lbl": "1"}, {"l2": "b", "lbl": "2"}, {"lbl1": "cde"}, {}},
		},
		{name: "NotGlob selector",
			selector: &NotGlob{key: "lbl", values: []string{"a*"}},
			match:    []map[string]string{{"lbl": "1"}, {"l2": "b", "lbl": "2"}, {"lbl": "cde"}},
			notMatch: []map[string]string{{"lbl": "a1b2cbe"}, {"l2": "b", "lbl": "a1b2cbe"}, {"lbl": "a1b2cbe"}, {}},
		},
		{name: "Regexp selector",
			selector: &Regexp{key: "lbl", regexp: abExp},
			match:    []map[string]string{{"lbl": "acccb"}, {"l2": "b", "lbl": "ascb"}},
			notMatch: []map[string]string{{"lbl": "a1"}, {"l2": "b", "lbl": "b2"}, {"lbl": "cde"}, {}},
		},
		{name: "NotRegexp selector",
			selector: &NotRegexp{key: "lbl", regexp: abExp},
			match:    []map[string]string{{"lbl": "a1"}, {"l2": "b", "lbl": "b2"}, {"lbl": "cde"}},
			notMatch: []map[string]string{{"lbl": "acccb"}, {"l2": "b", "lbl": "ascb"}, {}},
		},
		{name: "Exact selector",
			selector: &Exact{key: "lbl", value: "abc"},
			match:    []map[string]string{{"lbl": "abc"}, {"l2": "b", "lbl": "abc"}},
			notMatch: []map[string]string{{"lbl": "abcd"}, {"l2": "b", "lbl": "ab"}, {}},
		},
		{name: "NotExact selector",
			selector: &NotExact{key: "lbl", value: "abc"},
			match:    []map[string]string{{"lbl": "abcd"}, {"l2": "b", "lbl": "ab"}},
			notMatch: []map[string]string{{"lbl": "abc"}, {"l2": "b", "lbl": "abc"}, {}},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			for i := 0; i < len(test.match); i++ {
				match := test.selector.Match(test.match[i])
				assert.Equal(t, true, match, fmt.Sprintf("value %s", test.match[i]))
			}

			for i := 0; i < len(test.notMatch); i++ {
				match := test.selector.Match(test.notMatch[i])
				assert.Equal(t, false, match, fmt.Sprintf("value %s", test.notMatch[i]))
			}
		})
	}
}

func TestMatchMultiple(t *testing.T) {
	tests := []struct {
		name      string
		selectors *Selectors
		match     []map[string]string
		notMatch  []map[string]string
	}{
		{name: "Multiple selector",
			selectors: &Selectors{[]Selector{
				&Glob{key: "lbl", values: []string{"a*", "b*"}},
				&Exact{key: "lbl2", value: "value"},
			}},
			match:    []map[string]string{{"lbl": "a1", "lbl2": "value"}, {"l": "v", "lbl": "b1", "lbl2": "value"}},
			notMatch: []map[string]string{{"lbl": "a1"}, {"lbl2": "value"}, {"lbl": "a1", "lbl2": "value2"}, {}},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			for i := 0; i < len(test.match); i++ {
				match := test.selectors.Match(test.match[i])
				assert.Equal(t, true, match, fmt.Sprintf("value %s", test.match[i]))
			}

			for i := 0; i < len(test.notMatch); i++ {
				match := test.selectors.Match(test.notMatch[i])
				assert.Equal(t, false, match, fmt.Sprintf("value %s", test.notMatch[i]))
			}
		})
	}
}
