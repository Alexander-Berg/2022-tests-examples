YMAPSDF_PATH=/usr/share/yandex/maps/ymapsdf2
DB_CONN="host=pg93mpro.maps.dev.yandex.net port=5432 user=mapspro password=mapspro dbname=export"

YMAPSDF_SCHEMA=$YMAPSDF_PATH/ymapsdf.sql

MAPSPRO_PATH="../"

LOG_NAME=log.txt
DATALOG_NAME=datalog.txt

load () {
    xxd -p gosprom.kmz | tr -d '\n' > ./gosprom.hex

    { echo -n "DROP SCHEMA IF EXISTS hexmodel CASCADE; CREATE SCHEMA hexmodel; "\
    "CREATE TABLE hexmodel.hexdata (hex text); INSERT INTO hexmodel.hexdata VALUES('" \
    && cat ./gosprom.hex \
    && echo "');" ;}\
    | psql -q "$DB_CONN"

    rm ./gosprom.hex

    echo "DROP SCHEMA IF EXISTS $1 CASCADE; CREATE SCHEMA $1; SET search_path to $1,public;" \
    | cat - $YMAPSDF_SCHEMA $2 | psql -q "$DB_CONN"
}

ymapsdf2json () {
    $MAPSPRO_PATH/ymapsdf2json/ymapsdf2json --log-level=info --conn="$DB_CONN" --schema=$1 --id-mode=original --max-connections=2 \
    | jq \
    >$2.json
}

json2ymapsdf () {
    $MAPSPRO_PATH/json2ymapsdf/src/json2ymapsdf --conn="$DB_CONN" --schema=$2 \
        --transform-cfg=$MAPSPRO_PATH/json2ymapsdf/cfg/json2ymapsdf.xml \
        --data-error-log=$DATALOG_NAME <$1.json
}

dump () {
    env PGOPTIONS='-c bytea_output=hex' pg_dump  --data-only --inserts --schema=$1 "$DB_CONN" \
    | grep "^INSERT INTO" | grep -v "INSERT INTO ft_type VALUES" |  grep -v "INSERT INTO access VALUES"  \
    | sort \
    | awk '{table=$3; line=$0; if (table!=prevtable) {print "\n--", table;} print line; prevtable=table;}' \
    | sed -e "s/'schema_version', '2.[0-9]*.[0-9]*-[0-9]*\(\.local\)*'/'schema_version', '2.0.0-0'/" \
    > $1.sql
}

cmpsize() {
    SIZE_1=`wc -c < $1`
    SIZE_2=`wc -c < $2`

    if [ $SIZE_1 -ne $SIZE_2 ]
    then
        return 1
    fi
}
testnofail () {
    echo -n "$1 $2 $3: "
    if "$@" &>> $LOG_NAME
    then
        echo ok
        return 0
    else
        echo failed
        return 1
    fi
}

test () {
    if ! testnofail $1 $2 $3
    then
        echo "Test terminated. See test.log for details."
        exit 1
    fi
}

rm -f $LOG_NAME
rm -f $DATALOG_NAME

test load step0 test.sql
test ymapsdf2json step0 step1
test json2ymapsdf step1 step2
test ymapsdf2json step2 step3
test json2ymapsdf step3 step4

test dump step0
test dump step2
test dump step4

testnofail cmpsize step1.json step3.json
testnofail cmpsize step1.json step1.json.canon
testnofail cmpsize step2.sql step4.sql
test cmp step2.sql step2.sql.canon
test cmpsize step1.json step1.json.canon
