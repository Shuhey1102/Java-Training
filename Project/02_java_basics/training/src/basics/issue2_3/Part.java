package basics.issue2_3;
import basics.issue2_3.Printable;

public class Part implements Printable{

    private String partCode;
    private String partName;
    private int stock;
    private String warehouseCode;

    public Part(String partCode, 
                String partName, 
                int stock, 
                String warehouseCode) {
        this.partCode = partCode;
        this.partName = partName;
        this.stock = stock;
        this.warehouseCode = warehouseCode;
    }

    public String getPartCode() {
        return partCode;
    }

    public String getPartName() {
        return partName;
    }

    public int getStock() {
        return stock;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setPartCode(String partCode) {
        this.partCode = partCode;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    @Override
    public String toString(){
        return "Part [partCode=" + this.partCode + ", partName=" + this.partName + ", stock=" + this.stock + ", warehouseCode=" + this.warehouseCode + "]";
    }

    public void printInfo(){

        System.out.println(
             "------------部品情報------------" 
                + System.lineSeparator() +"部品コード: " + this.partCode
                + System.lineSeparator() +"部品名: " + this.partName
                + System.lineSeparator() +"在庫数: " + this.stock
                + System.lineSeparator() +"倉庫コード: " + this.warehouseCode
        );
    }

    public boolean isLowStock(int threshold) {
        return stock < threshold;
    }
}