package basiscs.issue2_5;

public class ExceptionSample extends RuntimeException {
    public StockException(int request, int stock) {
        super("在庫不足：要求数=" + request + ", 現在庫=" + stock);
    }    
}