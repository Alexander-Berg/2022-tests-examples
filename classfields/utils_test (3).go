package strings

import "testing"

func TestCut(t *testing.T) {
	tests := []struct {
		name   string
		input  string
		maxLen int
		append string
		want   string
	}{
		{
			name:   "negative maxlen",
			input:  "",
			maxLen: -1,
			append: ".",
			want:   "",
		},
		{
			name:   "empty input, maxlen=0",
			input:  "",
			maxLen: 0,
			append: ".",
			want:   "",
		},
		{
			name:   "maxlen=0",
			input:  "a",
			maxLen: 0,
			append: ".",
			want:   "",
		},
		{
			name:   "len(append) == maxLen",
			input:  "abcde",
			maxLen: 3,
			append: "...",
			want:   "abc",
		},
		{
			name:   "len(append) > maxLen",
			input:  "abcde",
			maxLen: 2,
			append: "...",
			want:   "ab",
		},
		{
			name:   "empty input",
			input:  "",
			maxLen: 10,
			append: ".",
			want:   "",
		},
		{
			name:   "no cut-ascii",
			input:  "abcdefghij",
			maxLen: 10,
			append: ".",
			want:   "abcdefghij",
		},
		{
			name:   "no cut-utf8",
			input:  "абвгдеёжзи",
			maxLen: 10,
			append: ".",
			want:   "абвгдеёжзи",
		},
		{
			name:   "cut-ascii",
			input:  "abcdefghij",
			maxLen: 5,
			append: ".",
			want:   "abcd.",
		},
		{
			name:   "cut-utf8",
			input:  "абвгдеёжзи",
			maxLen: 5,
			append: ".",
			want:   "абвг.",
		},
		{
			name:   "utf8-append",
			input:  "абвгдеёжзи",
			maxLen: 7,
			append: ".бфг",
			want:   "абв.бфг",
		},
		{
			name:   "ascii-append",
			input:  "абвгдеёжзи",
			maxLen: 7,
			append: "...",
			want:   "абвг...",
		},
	}
	for _, tt := range tests {
		t.Run(
			tt.name, func(t *testing.T) {
				if got := Cut(tt.input, tt.maxLen, tt.append); got != tt.want {
					t.Errorf("Cut() = %v, want %v", got, tt.want)
				}
			},
		)
	}
}
