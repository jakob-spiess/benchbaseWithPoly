DROP TABLE USERTABLE;
CREATE TABLE USERTABLE(
    YCSB_KEY INTEGER PRIMARY KEY,
    FIELD1 VARCHAR,
    FIELD2 VARCHAR,
    FIELD3 VARCHAR,
    FIELD4 VARCHAR,
    FIELD5 VARCHAR,
    FIELD6 VARCHAR,
    FIELD7 VARCHAR,
    FIELD8 VARCHAR,
    FIELD9 VARCHAR,
    FIELD10 VARCHAR
);
ALTER TABLE USERTABLE ADD INDEX IDX_YCSB_KEY ON (YCSB_KEY);