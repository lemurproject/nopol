
ClueWeb Annotations
===================

Tools for processing annotations on the Clueweb datasets. 

This project includes tools for the three steps of the process:

1. Convert the WARC files into the format required to do the annotations.
2. Convert the annotations into the published format
3. Test the annotations in the original documents.

## Tools

### Convert the annotations into the published format

The original annotations are generated in plain text files that contain the 
output for several documents from ClueWeb. We convert them to another format 
that makes them easier to process. 

The following is an example of the annotation in the original format:

    clueweb09-en0000-00-01198.html
    VOC     41856   41859   1.4636309e-05   /m/03zbcv
    US      42670   42672   0.0054688235    /m/09c7w0
    Birch   42722   42727   8.6088057e-06   /m/0hpx4
    MDF     42751   42754   5.1136312e-06   /m/01pd_h

    clueweb09-en0000-00-01500.html
    Illinois Appellate Court        7750    7774    0.00019704235   /m/02z5_ft
    Lisa Madigan    8996    9008    0.00038186592   /m/04q09d

Each block contains the annotations for a single document; where th first line 
corresponds to the document ID (WARC-TREC-ID) and each of the next lines are 
the annotations identified in them. Each annotation has the following format 
(tab separated):

    text        start-pos  end-pos  probability   freebase-id

`text` is the matched text on the document. This is not exactly the same 
string, since it may have been transformed: removing additional whitespace or 
normalizing the characters.

`start-pos` The byte in which the identified entity starts.

`end-pos` The byte in which the identified entity ends.

`probability` A confidence score assigned to the annotation.

`freebase-id` The identifier of the entity in Freebase; it is also knwon as 
`mid` (Metabase Id). This identifier is used to form the URL the entity in 
Freebase:

    http://www.freebase.com/m/XXXXX


## Output format

The annotations are converted to the a format that is easier to process. The 
format adds two columns at the begining of each line:

* Document identifier (WARC-TREC-ID)
* Original encoding: name of the encoding used to process the entry. The
  positions of the annotations correspond to the locations byte offsets
  after processing the file in this encoding.

The following is an example of the format:

    clueweb09-en0000-00-01198   UTF-8  VOC     41856   41859   1.4636309e-05   
    /m/03zbcv
    clueweb09-en0000-00-01198   UTF-8  US      42670   42672   0.0054688235    
/m/09c7w0


Note that the first field is the actual value of WARC-TREC-ID, without the 
.html extension.

## Converting the original annotations

The program lemur.cw.ann.FormatAnnotations allows you to convert the original 
annotations into the output format. It supports both formats used for the 
annotations:

* Plain text files using the format described above
* .tar.gz archives that contains plain text files, using the same format

The program is a command line utility used as follows:

    lemur.cw.ann.ConvertOriginal tar|text [input1 [input2 ...]] > output.tsv
    
Where `inputN` is the an input file, either plain text or .tar.gz, depending on 
the mode selected. If no input files are specified, the standard input is 
used.

The annotations are written to the standard output.

### Test the annotations in the original documents

This command allows you to test that the annotations match the original 
documents. For each annotation, the program reads the content of the document
and it compares the text mentioned in the annotation with the actual text found
within the offsets. The program does approximate match on the strings, ignoring 
whitespace, and case differences.

If the annotations do not match, they are compared after reading the document
using different encodings in the following order:

- UTF-8
- ISO-8859-1 (HTTP's default)
- Encoding identified using the ICU library

If there is no match, an error is printed to the standard output. Otherwise
(i.e. if the texts match), no output is generated.

Command line usage:

    lemur.cw.ann.DetectEncoding file.warc.gz annotations.tsv[.gz] [debug]

Use 'true' as the last argument to print the annotations that can't be matched to the stderr


### Convert the WARC files into .tar.gz files

The class ProcessWarc allows you to convert a WARC file into a .tar.gz, where
for each response record a file entry is added to the tar.gz. The content
of the file entry corresponds to the body of the WARC record, with some 
processing steps.

- Each character of the the HTTP headers is replaced by white space    
- The body is written as it is. 

The file entries have the same size (in bytes) than the actual WARC records. 

Each file entry is named using the WARC header `WARC-TREC-ID` with the
extension `.html` appended to it.

#### Command line usage:

The program has two modes: reading individual files or reading an entire 
directory.

Individual files:

    lemur.nopol.ProcessWarc file file.warc.gz output.tar.gz

Directories:

    lemur.nopol.ProcessWarc dir input-directory output-directory





