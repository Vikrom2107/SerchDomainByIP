package ru.romanovvb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class IpAndDomainMap {

    private Map<String, String> ipAndDomain;

    public IpAndDomainMap() {
        ipAndDomain= new HashMap<>();
    }
    public String printSortedMap() {
        List<Map.Entry<String,String>> list =
                new LinkedList<>(ipAndDomain.entrySet());
        Collections.sort( list, (o1, o2) -> {
            int o1Key = Integer.parseInt(o1.getKey().split("\\.")[3]);
            int o2Key = Integer.parseInt(o2.getKey().split("\\.")[3]);
            return Integer.compare(o1Key, o2Key);
        });
        String result = "";
        for (Map.Entry<String,String> entry : list) {
            result += entry.getKey() + " : " + entry.getValue() + "\n";
        }

        return result;
    }
    public void saveTxt(File textFile) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(textFile))){
            bw.write(printSortedMap());
            bw.flush();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public Map<String, String> getIpAndDomain() {
        return ipAndDomain;
    }
}
