# What Is My IP

Simple WhatIsMyIpAddress CLI Client.

## Usage

```
whatismyip 1.3.0
Usage: whatismyip [options]

-f, --file <value>      Output file (will be created if not exists)
-i, --interval <value>  update interval (in Scala's Duration format)
--help                  prints this usage text
--version
```

### Examples

#### Check and print every 1 minute (default behaviour)

```sh
sbt run
```

#### Write to the file instead of `stdout`

```sh
sbt "run --file=/tmp/whatismyip"
```

#### Use the custom interval

```sh
sbt "run --interval=2.minutes --file=/tmp/whatismyip"
```

See [scala.concurrent.duration.DurationConversions](https://www.scala-lang.org/api/current/scala/concurrent/duration/DurationConversions.html)
for possible interval durations.
