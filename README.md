
=====
nopol
=====

A set of tools to process the annotations of the Clueweb datasets.

These tools were used to create the version of the dataset that was processed
externally and to parse and test those annotations to the final output format.

# How to use?

## Generating the cleaned dataset

This step consist in genrating a copy of Clueweb

## Parsing the annotations

We first convert the original annotations into a format that it's easier to 
process. This is an example of the original annotations:

    clueweb09-en0000-00-00271.html
    Fooo	4543	6594	0.00123131231	/m/AAAAA
    Barr	5221	7522	0.00837645381	/m/XXXXX

    clueweb09-en0000-00-00272.html
    Example	12258	18124	0.00092574045	/m/ZZZZZ

Which get converted as follows:

    clueweb09-en0000-00-00271   Fooo     4543	6594	0.00123131231	/m/AAAAA
    clueweb09-en0000-00-00271   Barr	5221	7522	0.00837645381	/m/XXXXX
    clueweb09-en0000-00-00272   Example	12258	18124	0.00092574045	/m/ZZZZZ

In both cases, the fields are separated by tabs.

The class FormatAnnotations allows you to convert an initial annotation file:

    cat input | java lemur.nopol.io.google.FormatAnnotations > output

You also can provide the input file name:

    java lemur.nopol.io.google.FormatAnnotations input > output

## Testing the annotations



WARC files from the Clueweb datasets 

Extracts the records on a WARC file as plain (HTML) files.

