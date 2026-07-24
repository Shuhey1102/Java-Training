package basics.issue2_1;

public class ControlSample {

    //1. 1〜100 の整数のうち、3 の倍数は `Fizz`、5 の倍数は `Buzz`、両方の倍数は `FizzBuzz`、それ以外は数字をコンソールに出力する（FizzBuzz）
    //2. `switch` 文を使い、月（1〜12）を受け取って季節（春・夏・秋・冬）を出力するメソッドを実装する        

    public static void main(String[] args) {
        // 課題1
        System.out.println("----課題1 FizzBuzz Start----");
        fizzBuzz();
        System.out.println("----課題1 FizzBuzz End----");
        // 課題2
        System.out.println("----課題2 季節出力 Start----");
        outSeason(3);
        outSeason(6);
        outSeason(9);
        outSeason(12);
        System.out.println("----課題2 季節出力 End----");
    }

    private static void fizzBuzz() {
        int minNum = 1;
        int maxNum = 100;
        
        for (int i = minNum; i <= maxNum; i++){
            if(i % 3 == 0 && i % 5 == 0){
                System.out.println("FizzBuzz");  
            }
            else if(i % 3 == 0){
                System.out.println("Fizz");
            }
            else if(i % 5 == 0){
                System.out.println("Buzz");
            }
            else{
                System.out.println(i);
            }
        }
    }
    
    /*
        param month 月（1〜12）
        output 季節（春・夏・秋・冬）をコンソールに出力する
    */
    private static void outSeason(int month){
        switch(month){
            case 3:
            case 4:
            case 5:
                System.out.println("春");
                break;
            case 6:
            case 7:
            case 8:
                System.out.println("夏");
                break;
            case 9:
            case 10:
            case 11:
                System.out.println("秋");
                break;
            case 12:
            case 1:
            case 2:
                System.out.println("冬");
                break;
            default:
                System.out.println("1〜12の範囲で入力してください。");
        }
    }
}