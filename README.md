# timeular-cli

This cli gives functionality to work with data from the [Timeular Developer API](https://developers.timeular.com/public-api/)
that is fed by your [Timeular](https://timeular.com) device.

Currently, you can create xls exports of your data in a format that in compatible with SAP CATS to make it easier to
report your working time.

# how to run

```sbt run export -o test.xls --start-time 2019-12-02 --end-time 2019-12-20```

either set the `TIMEULAR_API_KEY` and `TIMEULAR_API_SECRET` environment variables, or supply `--api-key` or `--api-secret` cli params.
