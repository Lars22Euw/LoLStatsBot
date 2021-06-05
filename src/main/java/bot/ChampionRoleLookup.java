package bot;

import com.merakianalytics.orianna.types.common.Role;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.staticdata.Champions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChampionRoleLookup {
    public static final String COULD_NOT_GET_U_GG_TIERLIST = "Could not get u.gg Tierlist";
    static Map<Champion, Map<StalkRole, Double>> championRoles = new HashMap<>();
    // TODO: normalize champion as such that sum over roles = 1
    static void initialize(String riotApi) {
        try {
            URL url = new URL("https://champion.gg/statistics/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            try (var ir = new InputStreamReader(con.getInputStream());
                 BufferedReader in = new BufferedReader(ir, 8192*8192);
                 FileWriter wr = new FileWriter("champion.gg.txt")) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    var parts = inputLine.split(">");
                    var isPickRateFollowing = false;
                    for (var p : parts) {
                        if (isPickRateFollowing
                                || p.contains("class=\"champion-link\"")) {
                            wr.write(p);
                            wr.write("\n");
                            isPickRateFollowing = false;
                        }
                        if (p.contains("<div class=\"champion-pick-rate\"")) {
                            isPickRateFollowing = true;
                        }
                    }
                }
            }
            con.disconnect();

            Champions.get().forEach(c -> {
                championRoles.put(c, new HashMap<>());
                for (var role : StalkRole.values()) {
                    championRoles.get(c).put(role, 0.5);
                }
            });
            var isChamp = false;
            Champion champ;
            StalkRole role;
            double pickRate = 0.0;
            try (var fr = new FileReader("champion.gg.txt");
                    BufferedReader in = new BufferedReader(fr, 8192*8192)) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (isChamp) {
                        var parts = inputLine.split("/");
                        champ = Champion.named(parts[2]).get();
                        role = StalkRole.fromString(parts[3].split("\"")[0]);
                        championRoles.get(champ).put(role, pickRate);
                    } else {
                        pickRate = Double.parseDouble(inputLine.split("%")[0]);
                    }
                    isChamp = !isChamp;
                }
            }
            Champions.get().forEach(c -> {
                var cRoles = championRoles.get(c);
                var sum = 0.0;
                for (var cRole : StalkRole.values()) {
                    sum += cRoles.get(cRole);
                }
                for (var cRole : StalkRole.values()) {
                    double finalSum = sum;
                    sum += cRoles.compute(cRole, (r, v) -> v / finalSum);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(COULD_NOT_GET_U_GG_TIERLIST, e);
        }
        /*
        try {
            URL ritoClash = new URL("https://euw1.api.riotgames.com/lol/clash/v1/players/by-summoner/p8U89rJqXUZdoAo3dc1QGsQhDBOVv0LTGT_o_o2r0MgM900");
            HttpURLConnection riotCon = (HttpURLConnection) ritoClash.openConnection();
            riotCon.setRequestProperty("X-Riot-Token", riotApi);
            riotCon.setRequestMethod("GET");
            try (var ir = new InputStreamReader(riotCon.getInputStream());
                 BufferedReader in = new BufferedReader(ir, 8192 * 8192);) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public static Map<StalkRole, Double> get(Champion champion) {
        return championRoles.get(champion);
    }
}
