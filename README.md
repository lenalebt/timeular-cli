# timeular-cli

This cli gives functionality to work with data from the [Timeular Developer API](https://developers.timeular.com/public-api/)
that is fed by your [Timeular](https://timeular.com) device.

Currently, you can create xls exports of your data in a format that in compatible with SAP CATS to make it easier to
report your working time.

# how to run

```sbt run export -o test.xls --startTime 2019-12-02 --endTime 2019-12-20```
