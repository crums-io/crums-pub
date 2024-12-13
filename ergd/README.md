ergd
=====

Timechain REST server.

### Build

To build, `cd` to this subdirectory, then

>  $ mvn clean package appassembler:assemble

Try

>  $ ./target/binary/bin/ergd -h

to verify it worked.


### `jpackage` options

To package a Java runtime use the `jpackage` command from *this* subdirectory
using the option-setting @files below.
Output is directed to the `target` subdirectory (the one maven creates).
Note the OS-specifc @file when built for each platform below.


#### Linux

>
      $ jpackage @jpckg/base @../jpckg/attrib @../jpckg/linux

This generates an `.rpm` or `.deb` file, depending on distro.

#### Mac

>
       $ jpackage @jpckg/base @../jpckg/attrib @../jpckg/mac

#### Windows

The `@..\jpckg\file` does not work on windows. Copy those files first, from parent to local directory.

>
       $ copy ..\jpckg\attrib jpckg
       $ copy ..\jpckg\win jpckg
       $ jpackage @jpckg\base @jpckg\attrib @jpckg\win



