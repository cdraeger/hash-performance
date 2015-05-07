#Hash Performance Testing Tool (bcrypt)

Java command-line interface application for testing the hardware-dependent performance of hash-algorithms.

![demo](http://gfycat.com/FlatAjarInchworm)

_Currently only **bcrypt** is supported (via the [jBCrypt](http://www.mindrot.org/projects/jBCrypt/) library), although this might change in future releases._

##Requirements

[Java Runtime Environment](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (1.6 or greater)

##Usage

[Download the packaged JAR file](https://github.com/cdraeger/hash-performance/blob/master/release/hash-performance-all-1.0.7.jar?raw=true), you will probably want to do that directly on your server:

`wget https://github.com/cdraeger/hash-performance/blob/master/release/hash-performance-all-1.0.7.jar?raw=true`

Start the application: **`java -jar hash-performance-all-1.0.7.jar [options]`**

Add `-h, --help` to see possible parameters first (all optional):

Option                | Description
--------------------- | ------------------------------------------------------
`-c, --color`         | _disables_ colorized output
`-h, --help`          | shows this help.
`-p, --print`         | enables printing of the resulting hash to the console
`-s, --string` <arg>  | sets the string used for the hash-function

##Description

This tool takes the user-input as the log2 of the number of rounds of hashing to apply, and displays the _total time consumed by the hash-function_. The precision of the duration is as good as the platform allows.

The work factor and therefore the duration of the hashing with bcrypt increases exponentially (2^x), which is why the time consumed by hashing passwords is important to know for a proper __balance of server-load, response-times and security__.

_For easy measurement and formatting of the elapsed time, the Guava [Stopwatch](https://github.com/google/guava/blob/master/guava/src/com/google/common/base/Stopwatch.java) implementation is used. Since the whole Guava dependency blows up the file size, only the necessary classes were extracted._

##Changelog

**v1.0.7**

* Switched to Guava [Stopwatch](https://github.com/google/guava/blob/master/guava/src/com/google/common/base/Stopwatch.java) implementation for time measurement and display

**v1.0.6**

* Initial release
