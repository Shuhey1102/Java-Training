package basics.issue2_2;

public class Part {

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
    
    public boolean isLowStock(int threshold) {
        return stock < threshold;
    }
}