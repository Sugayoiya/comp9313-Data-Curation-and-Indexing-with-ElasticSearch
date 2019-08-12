# comp9313-assignment3

This is a Spark standalone program
Spark version **2.4.3**
Scala version **2.11.12**
Elasticsearch version **6.1.1 or above**
Stanford Core NLP version **`stanford-corenlp-full-2017-06-09`**

Usage:
`spark-submit --packages org.scalaj:scalaj-http_2.11:2.4.2,org.json:json:20180813 --class "CaseIndex"
--master local[2] JAR_FILE FULL_PATH_OF_DIRECTORY_WITH_CASE_FILES`
