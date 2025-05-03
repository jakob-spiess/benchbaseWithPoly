// Switch to the correct namespace
use public;

// Drop the collection if it exists
db.usertable.drop();

// Create the collection
db.createCollection("usertable");

// Create an index on the YCSB_KEY field
db.usertable.createIndex({ YCSB_KEY: 1 }, { name: "IDX_YCSB_KEY", unique: true });
