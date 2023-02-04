package clickhouse

import (
	"fmt"
	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/backend/parser"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"strings"
	"testing"
	"time"
)

func TestClickHouseBuilder_Build(t *testing.T) {
	qp := parser.NewQueryParser()
	b := &QueryBuilder{
		TableName: "logs.logs",
	}
	q, err := qp.Parse(`version=1 message="w'tf" service!='se\n\'vice' level=INFO (rest.st\.\uff=1 or rest.stuff=2 rest.foo.bar=3)`)
	require.NoError(t, err)
	sqlString := b.Build(QueryParams{
		Start:  time.Date(2020, 1, 1, 0, 0, 0, 0, time.UTC),
		Filter: q,
	})
	require.NoError(t, err)
	assert.Contains(t, sqlString, "st.uff")
	assert.Contains(t, sqlString, `se\n\'vice`)
}

func printQuery(p *core.Parenthesis, offset int) {
	spad := strings.Repeat("  ", offset)
	fmt.Printf("%soperator: %s\n", spad, p.Operator.String())
	for _, sp := range p.Parenthesis {
		printQuery(sp, offset+1)
	}
	for _, cond := range p.Conditions {
		fmt.Print(spad, cond.Field, " ", cond.Operator, " ", queryEscape(cond.Value), "\n")
	}
}
