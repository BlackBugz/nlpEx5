package de.tudarmstadt.lt.teaching.nlp4web.ml.reader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class JRCNameTransformer {
	static CharsetEncoder asciiEncoder = Charset.forName("ISO-8859-1")
			.newEncoder(); // or "ISO-8859-1" for ISO Latin 1

	public static boolean isPureAscii(String v) {
		return asciiEncoder.canEncode(v);
	}
	
	static int SKIP_N = 133;

	public static void main(String args[]) throws Exception {
		String inputFile ="/home/marco/Scrivania/NLP/Es5/sets/entities";
		String outputFile = "/home/marco/Scrivania/NLP/Es5/sets/entities.list";
		
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		
		int count = 0;
		int skip = SKIP_N;
		String line = "";
		while((line = reader.readLine()) != null)
		{
			skip--;
			if(skip < 0)
			{
				String pieces[] = line.split("\t");
				if(JRCNameTransformer.isPureAscii(pieces[3]))
				{
					String label = "";
					switch(pieces[1].toUpperCase()){
					case "O":
						label = "ORG";
						break;
					case "P":
						label ="PER";
						break;
					case "T":
						label ="LOC";
						break;
					default:
						System.out.println("!! > " + line);
						label = "MISC";
						break;
					}
					writer.write(label+" "+pieces[3].replace("+", " ")+"\n");
					count++;
					skip = SKIP_N;
				}
				else
				{
					skip = 0;
				}
			}
		}
		reader.close();
		writer.close();
		System.out.println("I wrote "+count+" lines");
	}

}
