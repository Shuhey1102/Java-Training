package basics.issue2_3;
import basics.issue2_3.Part;
import basics.issue2_3.ManagedPart;


public class InheritanceSample {
    public static void main(String[] args){

        Part part1 = new Part("P001","Part A",5,"WH01");
        Part part2 = new ManagedPart("P002","Part B",15,"WH02","Manager X");

        part1.printInfo();
        part2.printInfo();
    }
}