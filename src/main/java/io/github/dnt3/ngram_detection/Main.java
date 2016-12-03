package io.github.dnt3.ngram_detection;

import io.github.dnt3.ngram_detection.structures.NGram;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;


/**
 * Created by nikolas on 20/11/16.
 */
public class Main {

    private static int max_n = 0;
    private static String input_file = "input.dat";
    private static PrintStream printStream = System.out;

    public static void main(String[] args) {

        // access arguments
        for (int i = 0; i != args.length; i++) {
            if (args[i].equalsIgnoreCase("-i")) {
                input_file = args[i+1];
            } else if (args[i].equalsIgnoreCase("-o")) {
                String output_file = args[i+1];
                try {
                    printStream = new PrintStream(output_file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (args[i].equalsIgnoreCase("-h")) {
                System.out.println("Program's flags:  -i inputFile -o outputFile");
            }
        }

        if (!(new File(input_file)).exists()){
            System.out.println("Wrong flags! Use -h to see commands");
            return;
        }

        Map<String, Integer> occurrence_map = new HashMap<>();
        countOccurrences(occurrence_map);
        Map<String, Vector<NGram>> index = create_index(occurrence_map, input_file);
        printStream.println("\nF\n");
        printMap(index, printStream, occurrence_map);

		/*Search in file*/
        LinkedList<String> searchTerms = new LinkedList<>();
        for (int i = 0; i< max_n-1; i++) searchTerms.add("");
        Stream<String> lines = null;
        try {
            String search_file = "text_stream.dat";
            lines = Files.lines(Paths.get(search_file));
            lines.forEach(line -> {
                String searchKey = null, backupKey = null;
                String[] parts = line.split(" ");
                for (String part : parts) {
					if(searchTerms.size() < 2*max_n-1){
						searchTerms.add(part);
					}
					if (searchTerms.size() >= 2*max_n-1) {
                        int search_offset = max_n-1;
                        int backup_offset = max_n-1;
                        searchKey = searchTerms.get(search_offset);
                        /* Search middle term */
                        if(index.containsKey(searchKey)) {
							Vector<NGram> ngrams = index.get(searchKey);
							for (NGram ngram : ngrams) {
								int offset = ngram.getOffset();
                                boolean areEqual = true;
                                for(int i=0 ; i<ngram.getSize() ; i++) {
                                    backupKey = searchTerms.get(backup_offset-offset+i);
                                    Vector<String> terms = ngram.getTerms();
                                    if (!terms.get(i).equals(backupKey)) {
                                        areEqual = false;
                                        break;
                                    }
                                }
                                search_offset++;
                                if (areEqual) {
                                    printStream.println("NGram match! key: " + searchKey + ",\tngram: " + ngram);
                                }
                            }
                        } /* else Skip it */
                        searchTerms.remove(); /* remove first */
                    }
                }
			});
            // TODO: check-fix hot finish!
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (lines != null)
				lines.close();
		}
    }

    // Create Hash (Occurrence Map)
    private static void countOccurrences(Map<String, Integer> map) {
        Vector<String> terms = new Vector<>();

		Stream<String> lines = null;
		try {
			lines = Files.lines(Paths.get(input_file));
            lines.forEach(line -> {
                // Split Line
                String[] parts = line.split(" ");
                terms.clear();
                Collections.addAll(terms, parts);
                int n = terms.size();
                String last = terms.get(n - 1);
                terms.remove(last);
                if(n-1 > max_n){
                	max_n = n-1;
                }
                for (String part : terms) {
                    if (map.containsKey(part)) { //key exists
                        map.put(part, map.get(part) + 1);
                    } else { //key does not exists
                        map.put(part, 1);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
			if (lines != null)
				lines.close();
		}
    }

    private static Map<String, Vector<NGram>> create_index(Map<String, Integer> occurrence_map, final String input_file) {
        Map<String, Vector<NGram>> index = new HashMap<>();
        try {
            Files.lines(Paths.get(input_file)).forEach(line -> {
                NGram ngram = NGram.parseLineToNgram(line);
                String key = ngram.findLeastUsedWord(occurrence_map);

                /* Insert to hash */
                Vector<NGram> ngrams_vec;
                if (index.containsKey(key)) {
                    ngrams_vec = index.get(key);
                    ngrams_vec.add(ngram);
                } else {
                    ngrams_vec = new Vector<>();
                    ngrams_vec.add(ngram);
                    index.put(key, ngrams_vec);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return index;
    }

    private static void printMap(Map<String, Vector<NGram>> map, PrintStream printWriter, Map<String, Integer> occurrence_map) {
        printWriter.println("Found " + map.size() + " words.\n\n");
        for (Map.Entry<String, Vector<NGram>> entry : map.entrySet()) {
            String key = entry.getKey();
            Vector<NGram> ngrams_vec = entry.getValue();
            printWriter.print("Key:" + key + " ("+occurrence_map.get(key)+")\t\tValues:\t" );
            for (NGram node : ngrams_vec) {
                printWriter.print(node + "\n\t\t\t");
            }
            printWriter.println();
        }
    }

}
