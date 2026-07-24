package basics.issue2_2;
import basics.issue2_2.Part;

public class PartTest{

    public static void main(String[] args){
        Part part1 = new Part("P001","Part A",5,"WH01");
        Part part2 = new Part("P002","Part B",15,"WH02");
        Part part3 = new Part("P003","Part C",9,"WH03");
       
        int wkWhreeshold = 10;
        System.out.println(part1);
        System.out.println("Method isLowStock for part1:" + part1.isLowStock(wkWhreeshold));
        System.out.println(part2);
        System.out.println("Method isLowStock for part2:" + part2.isLowStock(wkWhreeshold));
        System.out.println(part3);
        System.out.println("Method isLowStock for part3:" + part3.isLowStock(wkWhreeshold));
    }
}