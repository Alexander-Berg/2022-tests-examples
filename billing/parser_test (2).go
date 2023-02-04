package main

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/resource"
	"a.yandex-team.ru/library/go/test/yatest"
	purecalc "a.yandex-team.ru/yql/library/purecalc/go/binding"
)

type Output struct {
	ID        uint64 `yson:"id" json:"id"`
	MasterUID string `yson:"master_uid" json:"master_uid"`
	Version   uint64 `yson:"version" json:"version"`
	Obj       any    `yson:"obj" json:"obj"`
}

func TestParseData(t *testing.T) {
	output, err := ParseData()
	require.NoError(t, err)
	result, err := json.Marshal(*output)
	require.NoError(t, err)
	require.JSONEq(t, string(resource.Get("output.json")), string(result))
}

func ParseData() (*Output, error) {
	type Input struct {
		Data []byte `yson:"Data"`
	}
	var data any
	err := json.Unmarshal(resource.Get("input.json"), &data)
	if err != nil {
		return nil, err
	}
	raw, err := json.Marshal(data)
	if err != nil {
		return nil, err
	}
	input := []any{Input{Data: raw}}
	factory := purecalc.NewProgramFactory(
		purecalc.SetUDFDir(yatest.BuildPath("transfer_manager/udfs")),
		purecalc.AddFile(purecalc.Inline, "schema", string(resource.Get("schema.json"))),
	)
	program, err := factory.MakePullListProgram(
		string(resource.Get("parser.yql")),
		[]purecalc.Column{{Name: "Data", Type: purecalc.String}},
	)
	if err != nil {
		return nil, err
	}
	defer program.Close()
	res, err := program.ApplySlice(input)
	if err != nil {
		return nil, err
	}
	var output Output
	res.Next()
	err = res.Scan(&output)
	if err != nil {
		return nil, err
	}
	return &output, nil
}
