CREATE DOCUMENT NAMESPACE "ycsb";

use ycsb;
db.createCollection("usertable");
db.products.insertMany([
    {
        YCSB_KEY: 1,
        FIELD1: "value1",
        FIELD2: "value2",
        FIELD3: "value3",
        FIELD4: "value4",
        FIELD5: "value5",
        FIELD6: "value6",
        FIELD7: "value7",
        FIELD8: "value8",
        FIELD9: "value9",
        FIELD10: "value10"
      }
]);
