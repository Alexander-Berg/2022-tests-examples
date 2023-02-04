package strings_test

import (
	"github.com/YandexClassifieds/vtail/internal/strings"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestSplitAndUnescape(t *testing.T) {
	testCases := []struct {
		in  string
		out []string
	}{
		{``, []string{""}},
		{`\.`, []string{"."}},
		{`a.b`, []string{"a", "b"}},
		{`\.le\.vel\.`, []string{".le.vel."}},
		{`\.le\.vel\..\.inn\.er\.`, []string{".le.vel.", ".inn.er."}},
		{`es\\cape`, []string{`es\cape`}},
		{`es\"cape`, []string{`es"cape`}},
	}

	for _, testCase := range testCases {
		result := strings.SplitAndUnescape(testCase.in, ".")
		require.Equal(t, testCase.out, result)
	}
}

func TestCutMiddle(t *testing.T) {
	type args struct {
		s      string
		length int
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "even",
			args: args{"abcde", 2},
			want: "a ... e",
		},
		{
			name: "odd",
			args: args{"abcde", 3},
			want: "a ... e",
		},
		{
			name: "full",
			args: args{"abcde", 6},
			want: "abcde",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := strings.CutMiddle(tt.args.s, tt.args.length); got != tt.want {
				t.Errorf("CutMiddle() = %v, want %v", got, tt.want)
			}
		})
	}
}
