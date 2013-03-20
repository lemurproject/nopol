
nopol
=====

Tools for processing annotations on the Clueweb datasets. 

This project includes tools for the three steps of the process:

1. Convert the WARC files into the format required to do the annotations.
2. Convert the annotations into the published format
3. Test the annotations in the original documents.

## Tools

### Convert the annotations into the published format

The original annotations are generated in the following format:

   filename
   (rest)

However, the published format is the following (tab separated):
    
    docid entity    start end score mid

Use the following command:

    lemur.nopol.io.FormatAnnotations < input.txt > output.tsv


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

    lemur.nopol.TestAnnotations file.warc.gz annotations.tsv

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







