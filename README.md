#Hash Performance Testing Tool (bcrypt)

Java command-line interface application for testing the hardware-dependent performance of hash-algorithms.

![demo](http://i.imgur.com/RFCnuRY.gif)

_Currently only **bcrypt** is supported (via the [jBCrypt](http://www.mindrot.org/projects/jBCrypt/) library), although this might change in future releases._

##Requirements

[Java Runtime Environment](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (1.6 or greater)


##Usage

Start the application with

**`java -jar hash-performance.jar [options]`**

`-h, --help` to see possible parameters (all optional):

Option               | Description
-------------------- | --------------------
`-c, --color`        | _disables_ colorized output
`-h, --help`         | shows this help.
`-m, --millis`       | _enables_ output in milliseconds (default: ISO-8601)
`-p, --print`        | enables printing of the resulting hash to the console
`-s, --string` <arg> | sets the string used for the hash-function (by default a hardcoded random 16-character string will be used)

##Description

This tool takes the user-input as the log2 of the number of rounds of hashing to apply, and displays the _total time consumed by the hash-function_. The accuracy of the duration is not perfect, but it should suffice for the intended purpose.

The work factor and therefore the duration of the hashing with bcrypt increases exponentially (2^x), which is why the time consumed by hashing passwords is important to know for properly __balancing server-load, response-times and security__.

_To maintain Java 1.6/1.7 backwards compatibility, the Joda Time library is used. This adds overhead and significantly increases the file size, but spared me manual ISO 8601 formatting while not limiting compatibility to Java 1.8 and above._

##Changelog

**v1.0.6**

* Initial release