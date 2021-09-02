package bot;

import com.merakianalytics.orianna.types.core.summoner.Summoner;
import util.U;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class NameLookup {

    public static Map<String, String> ids = new HashMap<>();

    static Map<String, String> parseLookupFile(String name, String separator) {

        var map = new HashMap<String, String>();
        try (final var br = new BufferedReader(new FileReader(name))) {
            while (br.ready()) {
                var line = br.readLine().split(separator);
                // assumes pair of values
                map.put(line[0], line[1]);
            }
        } catch (IOException e) {
            U.log("Could not load lookupfile: ", name);
            e.printStackTrace();
        }
        return map;
    }


    public static void initIds() {

        var names = parseLookupFile("names-lookup.txt", "-");
        try {
            Files.deleteIfExists(Paths.get("names-lookup.txt"));
            Files.createFile(Paths.get("names-lookup.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        var idSeparator = " ";
        try (var bw = new BufferedWriter(new FileWriter("ids-lookup.txt", true))) {
            for (var name : names.entrySet()) {
                var id = Summoner.named(name.getValue()).get().getAccountId();
                bw.write(name.getKey() + idSeparator + id + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ids = parseLookupFile("ids-lookup.txt", idSeparator);
    }
}
