@echo off
REM Kann von hier gerunnt werden : Pfad\Documents\benchbaseWithPoly>
call mvnw clean package -P polypheny -DskipTests
cd .\target\
tar xvzf benchbase-polypheny.tgz
cd .\benchbase-polypheny\
java -jar benchbase.jar -b tpch -c config/polypheny/sample_tpch_mql_config.xml --create=false --load=true --execute=true
