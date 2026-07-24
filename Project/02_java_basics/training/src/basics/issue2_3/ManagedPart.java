package basics.issue2_3;
import basics.issue2_3.Part;

public class ManagedPart extends Part {

    private String manager;

    public ManagedPart(String partCode, 
                String partName, 
                int stock, 
                String warehouseCode,
                String manager) {
        super(partCode, partName, stock, warehouseCode);
        this.manager = manager;
    }

    @Override
    public void printInfo(){
        super.printInfo();
        System.out.println("管理者: " + this.manager);
    }

    @Override
    public String toString(){
        return "ManagedPart [partCode=" + super.getPartCode() + ", partName=" + super.getPartName() + ", stock=" + super.getStock() + ", warehouseCode=" + super.getWarehouseCode() + ", manager=" + this.manager + "]";
    }    
}