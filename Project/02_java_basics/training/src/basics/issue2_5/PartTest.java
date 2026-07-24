package basics.issue2_5;

import basics.issue2_5.Part;
import basics.issue2_5.ExceptionSample;

public class PartTest{

    public static void main(String[] args){
        Part part1 = new Part("P001","Part A",5,"WH01");
        Part part2 = new Part("P002","Part B",15,"WH02");
        Part part3 = new Part("P003","Part C",9,"WH03");

        try {
            part1.decreaseStock(3);
        } catch (ExceptionSample e) {
            System.out.println(e.getMessage());
        }

        try {
            part1.decreaseStock(6);
        } catch (ExceptionSample e) {
            System.out.println(e.getMessage());
        }
    }
}
