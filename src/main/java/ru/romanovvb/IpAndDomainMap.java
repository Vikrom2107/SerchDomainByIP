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

    // Сортирует порядок вывода по IP в строку
    public String printSortedMap() {
        List<Map.Entry<String,String>> list =
                new LinkedList<>(ipAndDomain.entrySet());
        Collections.sort( list, (o1, o2) -> {
            String[] o1Key = o1.getKey().split("\\.");
            String[] o2Key = o2.getKey().split("\\.");
            int[] o1KeyInt = new int[4];
            int[] o2KeyInt = new int[4];

            for (int i = 0; i < o1Key.length; i++)
                o1KeyInt[i] = Integer.parseInt(o1Key[i]);
            for (int i = 0; i < o2Key.length; i++)
                o2KeyInt[i] = Integer.parseInt(o2Key[i]);

            if (o1KeyInt[0] != o2KeyInt[0])
                return Integer.compare(o1KeyInt[0], o2KeyInt[0]);
            if (o1KeyInt[1] != o2KeyInt[1])
                return Integer.compare(o1KeyInt[1], o2KeyInt[1]);
            if (o1KeyInt[2] != o2KeyInt[2])
                return Integer.compare(o1KeyInt[2], o2KeyInt[2]);
            return Integer.compare(o1KeyInt[3], o2KeyInt[3]);
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
