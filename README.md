# comp9313-assignment3

This is a Spark standalone program

Spark version: 2.4.3

Scala version: 2.11.12

Elasticsearch version: 6.1.1 or above(need to do something in CaseIndex.scala #27-30)

Stanford Core NLP version: stanford-corenlp-full-2017-06-09

**what this program does:**

* Create the index (in ElasticSearch) with the corresponding mapping.
* Perform the necessary data curation tasks to transform the input data into the representation
used for indexing (i.e., XML to JSON representation mapping), and enrich the original data
with the three entity types specified above (i.e., person, location and organization).
* Index the curated/enriched data using ElasticSearch.

**Usage:**

at root directory:
`~$: sbt package`

`~$: spark-submit --packages org.scalaj:scalaj-http_2.11:2.4.2,org.json:json:20180813 --class "CaseIndex"
--master local[2] JAR_FILE FULL_PATH_OF_DIRECTORY_WITH_CASE_FILES`

**input format:**
```
<?xml version="1.0"?>
<case>
<name>Sharman Networks Ltd v...</name>
<AustLII>http://www.austlii.edu.au/au/cases...</AustLII>
<catchphrases>
<catchphrase>application for leave to appeal...</catchphrase>
<catchphrase>authorisation of multiple infringements...</catchphrase>
...
<sentences>
<sentence id="s0">Background to the current application...</sentence>
<sentence id="s1">1 The applicants Sharman Networks...</sentence>
...
<sentences>
</case>
```

**The schema above shows that a case is made of:**
* A name (\<name\>): This is the title of the case
* Source URL (\<AustLII\>): The original source of the legal report
* A list of catchphrases (\<catchphrases\>): These are short sentences that summarize the
case
* Sentences (\<sentences\>): The list of sentences contained in the legal case report

**index and mapping:**
```
{
  "legal_idx": {
    "aliases": {},
    "mappings": {
      "properties": {
        "AustLII": {
          "type": "text"
        },
        "id": {
          "type": "text"
        },
        "location": {
          "type": "text"
        },
        "name": {
          "type": "text"
        },
        "organization": {
          "type": "text"
        },
        "person": {
          "type": "text"
        },
        "sentence": {
          "type": "text"
        }
      }
    }
  }
}
```
