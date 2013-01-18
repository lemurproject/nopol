package lemur.nopol;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import lemur.nopol.io.WarcHTMLResponseRecord;
import lemur.nopol.io.WarcRecord;

public class Nopol {

	private static DataInputStream loadInputFile(File inFile)
			throws IOException {
		return new DataInputStream(new GZIPInputStream(new FileInputStream(
				inFile)));
	}

	public static void processFile(File inFile, File outFile)
			throws IOException {
		Exporter exporter = new Exporter();

		int n = -1;
		int nDocs = 0;
		int nErrors = 0;

		DataInputStream inStream = loadInputFile(inFile);

		TarWriter writer = new TarWriter(outFile);

		WarcRecord record = null;
		while ((record = WarcRecord.readNextWarcRecord(inStream)) != null) {
			WarcHTMLResponseRecord response = new WarcHTMLResponseRecord(record);
			n++;
			if (!response.isValid()) {
				continue;
			}

			try {
				byte[] content = exporter.exportRecord(record);
				String trecId = response.getTargetTrecID();
				writer.writeRecord(trecId, content);
				nDocs++;
			} catch (Exception e) {
				nErrors++;
				System.err.printf("Error processing record: %d\n", n);
				e.printStackTrace();
			}
		}

		writer.close();
		System.err.printf("Records written: %d. Errors: %d\n", nDocs, nErrors);
	}

	public static void processDir(File inputDir, File outDir) throws IOException {
		File[] inputFiles = inputDir.listFiles();

		for (File inputFile : inputFiles) {
			String outFname = inputFile.getName().replaceAll("\\.warc.gz", ".tar.gz");
			File outFile = new File(outDir, outFname);
			System.err.printf("Input file: %s\nOutput file: %s\n", 
					inputFile.getAbsolutePath(),
					outFile.getAbsolutePath());
			processFile(inputFile, outFile);
		}
	}
	
	public static void usageAndExit(){
		System.err.println("Usage: file|dir input output");
		System.exit(1);
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			usageAndExit();
		}

		String type = args[0];
		File inputArg = new File(args[1]);
		File outArg = new File(args[2]);
		if (type.equals("file")){
			processFile(inputArg, outArg);
		}else if (type.equals("dir")){
			processDir(inputArg, outArg);
		}else{
			usageAndExit();
		}
	}
}
