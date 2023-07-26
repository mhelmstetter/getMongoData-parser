# getMongoData-parser

Parses MongoDB getMongoData.js output and produces an HTML report and/or inserts a summary into MongoDB, exports to
Excel.


Quickstart
----------

1. Download the getMongoData script. It is recommended to use the version
   from [this gist](https://gist.github.com/mhelmstetter/063ddfc941336d5b7959c00ac7217ea8). Execute the script using
   the `mongo` shell, note that the script does not currently work using the newer `mongosh` shell. Instructions for
   executing are at the top of the getMongoData js file.

1. Download the jar file:

```
wget https://github.com/mhelmstetter/getMongoData-parser/blob/master/bin/getMongoDataParser.jar?raw=true
```

2. Create an alias, run the following and/or add it to your `.zprofile` or `.bashprofile` (update the path to the
   correct location!)

```
alias getmongodata="java -jar ${HOME}/git/getMongoData-parser/bin/getMongoDataParser.jar"
```

3. Run it!

```
getmongodata -f getMongoData.log
```

or

```
java -jar getMongoDataParser.jar -f getMongoData.log
```

the output will be written to a file of the same name with a `html` extension, e.g. getMongoData.html for the above
example.

#### Database output

```
getmongodata -f getMongoData.log -c mongodb://localhost:27017/stats
```

The output will be written to the database provided in the `-c` argument.    (Note: the database name must be provided
in the connection string; adjust connection string as needed!).  This will create two collections: `collStats`and `shardStats` in the database.
