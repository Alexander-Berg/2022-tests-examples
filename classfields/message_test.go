package domain

import "testing"

func TestCutMiddle(t *testing.T) {
	type args struct {
		s     []byte
		bytes int
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "even",
			args: args{[]byte{10, 11, 13, 14, 15}, 4},
			want: `"\n\v" ... "\x0e\x0f"`,
		},
		{
			name: "odd",
			args: args{[]byte{10, 11, 13, 14, 15}, 3},
			want: `"\n" ... "\x0f"`,
		},
		{
			name: "full",
			args: args{[]byte{10, 11, 13, 14, 15}, 6},
			want: `"\n\v\r\x0e\x0f"`,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := CutMiddle(tt.args.s, tt.args.bytes); got != tt.want {
				t.Errorf("CutMiddle() = %v, want %v", got, tt.want)
			}
		})
	}
}
