@echo off
REM Kann von hier gerunnt werden : Pfad\Documents\benchbaseWithPoly>
call mvnw clean package -P polypheny
cd .\target\
tar xvzf benchbase-polypheny.tgz
cd .\benchbase-polypheny\
java -jar benchbase.jar -b ycsb -c config/polypheny/sample_ycsb_config.xml --create=true --load=true --execute=true
