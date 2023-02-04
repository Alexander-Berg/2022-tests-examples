package lbexporter

import (
	"reflect"
	"testing"
)

func Test_addPrefix(t *testing.T) {
	type args struct {
		prefix  string
		columns []string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			name: "common",
			args: args{prefix: "abc", columns: []string{"123", "456", "789"}},
			want: []string{"abc123", "abc456", "abc789"},
		},
	}
	for _, tt := range tests {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			if got := addPrefix(tt.args.prefix, tt.args.columns); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("addPrefix() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_chainStringSlices(t *testing.T) {
	type args struct {
		slices [][]string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			name: "two slices",
			args: args{[][]string{{"abc", "def"}, {"hij", "klm"}}},
			want: []string{"abc", "def", "hij", "klm"},
		},
		{
			name: "a slice and an empty slice",
			args: args{[][]string{{}, {"hij", "klm"}}},
			want: []string{"hij", "klm"},
		},
		{
			name: "a slice and nil",
			args: args{[][]string{{"abc", "def"}, nil}},
			want: []string{"abc", "def"},
		},
	}
	for _, tt := range tests {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			if got := chainStringSlices(tt.args.slices...); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("chainStringSlices() = %v, want %v", got, tt.want)
			}
		})
	}
}
