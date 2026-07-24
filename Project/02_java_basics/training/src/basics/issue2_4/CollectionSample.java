package basics.issue2_4;
import basics.issue2_3.Part;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CollectionSample {

    public static void main(String[] args){
        List<Part> parts = new ArrayList<>();
        parts.add(new Part("P001", "Part A", 5, "WH01"));
        parts.add(new Part("P002", "Part B", 10, "WH02"));
        
        System.out.println("----部品リスト(Listにて出力)----");
        for (Part wkPart : parts){
            System.out.println(wkPart);
        }

        Map<String, Part> partMap = new HashMap<>();
        partMap.put("P001", new Part("P001", "Part A", 5, "WH01"));
        partMap.put("P002", new Part("P002", "Part B", 10, "WH02"));

        System.out.println("----部品リスト(Mapにて出力)----");
        for (String wkKey : partMap.keySet()){
            System.out.println(partMap.get(wkKey));
        }
    }
}