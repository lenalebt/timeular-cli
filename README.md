# timeular-cli

This cli gives functionality to work with data from the [Timeular Developer API](https://developers.timeular.com/public-api/)
that is fed by your [Timeular](https://timeular.com) device.

# how to run

either set the `TIMEULAR_API_KEY` and `TIMEULAR_API_SECRET` environment variables, or supply `--api-key` or `--api-secret` cli params.

Full usage instructions (get them by calling the application without any parameters):

```
Usage: timeular-cli [start|stop|list-activities|export] [options] <args>...

  --api-key <value>        The timeular API key.
  --api-secret <value>     The timeular API secret.
  --api-server <value>     The timeular API server.
  -o, --output-format <value>
                           The output format to be used.
  -f, --output-file <value>
                           The file to export to. Skip for writing to stdout. Writing to stdout for binary formats (such as xls) is currently not supported.
  -k, --output-options <value>
                           The output options for the format. Depends on the specific output format. Try e.g. 'report=true'
Command: start activity
start tracking an activity
  activity
Command: stop [activity]
stop tracking an activity
  activity
Command: list-activities
list the activities known to timeular
Command: export [options]
stop tracking an activity
  --start-time             Start time of when the export starts.
  --end-time               End time of when the export ends.
```

Example calls: `TIMEULAR_API_KEY` and `TIMEULAR_API_SECRET` are set beforehand.
Create an xls file report that you can copy-paste to SAP CATS.
```
timeular-cli export --start-time 2019-12-01 --end-time 2019-12-20 -o xls -f test.xls --output-options report=true
```

Report as text, only if you worked too much.
```
timeular-cli export --start-time 2019-12-01 --end-time 2019-12-20 -o text
```

Export all your data to xls
```
timeular-cli export --start-time 2019-12-01 --end-time 2019-12-20 -o xls -f test.xls
```

Export all your data to csv
```
timeular-cli export --start-time 2019-12-01 --end-time 2019-12-20 -o csv -f test.csv
```

See all logged entries in the given time frame, as text, on the console
```
timeular-cli export --start-time 2019-12-01 --end-time 2019-12-20 -o text --output-options report=true
```

There are some "rather versionless" artifacts in the github releases.
I expect them not to work properly, nor to be well-documented.
