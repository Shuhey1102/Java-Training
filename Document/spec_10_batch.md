# 課題仕様書 ⑩ バッチ処理：CSV から入出庫データを一括処理（部品自動登録）

**フェーズ：** Phase 3　**期間目安：** 3〜4 日　**担当：** 新人

---

## 目的

CSV ファイルに記載された入出庫データを一括処理するバッチを実装する。

処理の中で「部品コードが `parts` テーブルに存在するか」を確認し、
- **存在しない場合**：`parts` に自動で新規登録してから入出庫処理を行う
- **存在する場合**：そのまま入出庫処理を行う

この「存在確認 → 分岐（INSERT or スキップして UPDATE）」は実務でよく使われるパターン。

---

## 背景（業務イメージ）

工場では毎朝、前日の入出庫データが CSV ファイルとして送られてきます。
このファイルには既存の部品だけでなく、**まだシステムに登録されていない新しい部品**の
入出庫データが含まれることがあります。

手動で「先に部品登録してから入出庫処理」するのは非効率なため、
バッチが自動で判断して処理します。

---

## バッチの全体的な動き

```
起動
  └─→ 処理対象 CSV を読み込む
        └─→ 1 行ずつ処理
              │
              ├─→ parts テーブルに partCode が存在するか確認
              │     ├─→ 存在しない → parts に自動 INSERT（部品名・倉庫はデフォルト値）
              │     └─→ 存在する  → そのまま次へ
              │
              ├─→ type に応じて入出庫処理
              │     ├─→ IN  : 在庫数を増やす + 履歴 INSERT
              │     └─→ OUT : 在庫数を減らす + 履歴 INSERT
              │
              ├─→ 成功：DB 更新 + 成功ログ出力
              └─→ 失敗：その行をスキップ + エラーログ出力
                        ↓（全行処理後）
              └─→ 処理結果サマリーを表示して終了
```

> **ポイント：** 1 件失敗してもバッチ全体を止めない。エラーになった行はスキップして
> 次の行に進み、最後にまとめて結果を報告する。

---

## プロジェクト構成

課題 ⑤ で作成した `05_project_inventory_system` にバッチ用クラスを追加する。

```
05_project_inventory_system/
└── src/
    └── inventory/
        ├── model/
        │   ├── Part.java                      ← 流用
        │   ├── StockTransaction.java          ← 流用
        │   └── BatchResult.java               ← 新規追加（処理結果を表すクラス）
        ├── dao/
        │   ├── PartDao.java                   ← 流用
        │   └── TransactionDao.java            ← 流用
        ├── service/
        │   ├── InventoryService.java          ← 流用
        │   └── BatchService.java              ← 新規追加（バッチ処理ロジック）
        ├── util/
        │   └── DbConnection.java              ← 流用
        ├── exception/                         ← 流用
        └── app/
            └── BatchMain.java                 ← 新規追加（バッチのエントリーポイント）

batch_input/                                   ← プロジェクト直下に作成
└── stock_batch.csv                            ← バッチ処理対象ファイル

batch_output/                                  ← プロジェクト直下に作成（自動生成）
└── batch_result_yyyyMMdd_HHmmss.log           ← 実行結果ログ
```

---

## 処理対象 CSV の仕様

### ファイル：`batch_input/stock_batch.csv`

バッチ実行時に読み込む入出庫指示ファイル。
`operation` カラムは不要。部品の存在確認はバッチ側で自動的に行う。

**フォーマット：**
```csv
partCode,type,quantity
P001,IN,100
P002,OUT,20
P006,IN,50
P999,OUT,10
P002,OUT,9999
```

| カラム | 型 | 説明 | 例 |
|---|---|---|---|
| `partCode` | `String` | 部品コード | `P001` |
| `type` | `String` | 種別（`IN`=入庫 / `OUT`=出庫） | `IN` |
| `quantity` | `int` | 数量（1 以上） | `100` |

> **`P006` のような未登録部品が来た場合：**
> バッチが自動的に `parts` テーブルに INSERT してから入出庫処理を行う。
> 自動登録時の部品名は `"未登録部品_部品コード"`、倉庫コードは `"WH01"`（デフォルト）とする。

**ルール：**
- 1 行目はヘッダー行（処理対象外）
- 空行は無視する
- カラム数が 3 つでない行はスキップしてエラーログに出力する
- `type` が `IN` / `OUT` 以外の行はスキップする

---

## 実装するクラス

### BatchResult クラス（`model/BatchResult.java`）

1 行分の処理結果を表すクラス。

| フィールド | 型 | 説明 |
|---|---|---|
| `lineNumber` | `int` | CSV の行番号（ヘッダー除く） |
| `partCode` | `String` | 部品コード |
| `type` | `String` | 種別（`IN` / `OUT`） |
| `quantity` | `int` | 数量 |
| `autoInserted` | `boolean` | 部品を自動登録した場合 `true` |
| `success` | `boolean` | 処理成功なら `true` |
| `message` | `String` | 成功時は完了メッセージ、失敗時はエラー内容 |

実装すること：
- 全フィールドを引数に持つコンストラクタ
- 各フィールドの getter
- `toString()` メソッド

---

### BatchService クラス（`service/BatchService.java`）

CSV を読み込んで 1 行ずつ入出庫処理を行うクラス。

```java
public class BatchService {

    private final InventoryService inventoryService;

    public BatchService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * CSV ファイルを読み込んで一括入出庫処理を行う。
     * @param filePath 処理対象 CSV のパス
     * @return 各行の処理結果リスト
     */
    public List<BatchResult> execute(String filePath) {
        // 実装すること
    }
}
```

**処理の流れ：**

```
1. CSV ファイルを開く（ファイルが存在しない場合は例外をスロー）
2. 1 行目（ヘッダー）をスキップ
3. 2 行目以降を 1 行ずつ処理
    a. カンマで分割してパースする
    b. バリデーション（後述）
    c. partCode が parts テーブルに存在するか確認（partDao.findByCode()）
         └─ 存在しない場合 → parts に自動 INSERT（部品名・倉庫はデフォルト値）
    d. type に応じて入出庫処理
         ├─ "IN"  → inventoryService.stockIn() を呼び出す
         └─ "OUT" → inventoryService.stockOut() を呼び出す
    e. 成功・失敗に関わらず BatchResult を生成してリストに追加
    f. 失敗してもループを止めず次の行へ進む
4. 全行処理後、結果リストを返す
```

> **ポイント：** c と d は同一トランザクションで実行すること。
> 部品の自動 INSERT に成功しても、その後の入出庫処理が失敗した場合は
> 部品 INSERT ごとロールバックする。

**バリデーション（パース時に実施）：**

| チェック内容 | エラーメッセージ例 |
|---|---|
| カラム数が 3 つでない | `フォーマット不正（カラム数エラー）` |
| `type` が `IN` / `OUT` 以外 | `種別が不正です：xxx` |
| `quantity` が整数でない | `数量が整数ではありません：xxx` |
| `quantity` が 1 未満 | `数量は 1 以上を指定してください` |
| `OUT` で在庫不足 | `在庫不足：要求数=xxx, 現在庫=xxx` |

---

### BatchMain クラス（`app/BatchMain.java`）

バッチのエントリーポイント。コマンドラインから実行する。

```java
public class BatchMain {
    public static void main(String[] args) {
        String filePath = "batch_input/stock_batch.csv";

        // BatchService を生成して実行
        // 結果を画面に表示
        // ログファイルに出力
    }
}
```

**実行時のコンソール出力例：**
```
===== 入出庫バッチ処理 開始 =====
処理ファイル：batch_input/stock_batch.csv
開始日時：2025-04-01 09:00:00

[1行目] P001 / 入庫 / 100個 → 成功（在庫：100 → 200）
[2行目] P002 / 出庫 / 20個  → 成功（在庫：50 → 30）
[3行目] P006 / 入庫 / 50個  → 成功（部品を自動登録しました / 在庫：0 → 50）
[4行目] P999 / 出庫 / 10個  → スキップ（エラー：在庫不足：要求数=10, 現在庫=0 ※自動登録後）
[5行目] P002 / 出庫 / 9999個 → スキップ（エラー：在庫不足：要求数=9999, 現在庫=30）

===== 処理結果サマリー =====
処理件数  ：5 件
  成功    ：3 件（うち部品自動登録：1 件）
  スキップ：2 件
終了日時：2025-04-01 09:00:01
結果ログ：batch_output/batch_result_20250401_090001.log
===========================
```

---

## ログファイル出力

`batch_output/` フォルダに実行日時付きのログファイルを出力すること。

**ファイル名：** `batch_result_yyyyMMdd_HHmmss.log`（例：`batch_result_20250401_090001.log`）

**内容：** コンソール出力と同じ内容をファイルにも書き出す（`BufferedWriter` で実装）

```
===== 入出庫バッチ処理 開始 =====
処理ファイル：batch_input/stock_batch.csv
開始日時：2025-04-01 09:00:00

[1行目] P001 / 入庫 / 100個 → 成功（在庫：100 → 200）
...（省略）...

===== 処理結果サマリー =====
処理件数：5 件
  成功：3 件
  スキップ：2 件
終了日時：2025-04-01 09:00:01
```

---

## 業務ルール

| ルール | 内容 |
|---|---|
| 1 件失敗してもバッチ全体は止めない | エラー行はスキップして次の行に進む |
| トランザクションは 1 行ごと | 1 行の処理（在庫更新 + 履歴 INSERT）は同一トランザクションで実行する |
| スキップした行は DB に反映しない | エラー行の在庫更新・履歴 INSERT はロールバックする |
| ログは必ず出力する | 成功・失敗に関わらず全行の結果をログに残す |
| 処理済みファイルの扱い | 今回は特に移動・削除しなくてよい（再実行可能な状態のまま） |

---

## サンプル CSV の用意

以下の内容で `batch_input/stock_batch.csv` を作成してバッチを実行すること。

```csv
partCode,type,quantity
P001,IN,100
P002,OUT,20
P006,IN,50
P999,OUT,10
P002,OUT,9999
INVALID_ROW_NO_COLUMNS
P001,UNKNOWN,10
P003,OUT,0
```

**期待される結果：**

| 行 | 内容 | 期待結果 |
|---|---|---|
| 1行目 | P001 入庫 100 | 成功（既存部品・在庫増加） |
| 2行目 | P002 出庫 20 | 成功（既存部品・在庫減少） |
| 3行目 | P006 入庫 50 | 成功（**未登録部品を自動 INSERT** してから入庫） |
| 4行目 | P999 出庫 10 | スキップ（自動登録後も在庫 0 のため在庫不足） |
| 5行目 | P002 出庫 9999 | スキップ（在庫不足） |
| 6行目 | カラム数不正 | スキップ（フォーマット不正） |
| 7行目 | 種別 UNKNOWN | スキップ（種別不正） |
| 8行目 | P003 出庫 0 | スキップ（数量不正） |

> **4行目（P999）の補足：** P999 は未登録のため自動 INSERT されるが、
> 初期在庫は 0 のため OUT 処理が在庫不足でスキップされる。
> このとき P999 の部品 INSERT もロールバックされること（同一トランザクションのため）。

---

## 実装のヒント

### 日時付きファイル名の生成

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

LocalDateTime now = LocalDateTime.now();
String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
String logFileName = "batch_output/batch_result_" + timestamp + ".log";
```

### コンソールとファイルへの同時出力

```java
// コンソールとファイルの両方に書くメソッドを用意すると便利
private void log(BufferedWriter writer, String message) throws IOException {
    System.out.println(message);
    writer.write(message);
    writer.newLine();
}
```

### 1 行ごとのトランザクション

```java
for (String line : lines) {
    Connection conn = DbConnection.getConnection();
    try {
        conn.setAutoCommit(false);
        // 入出庫処理
        conn.commit();
        // BatchResult（成功）をリストに追加
    } catch (Exception e) {
        conn.rollback();
        // BatchResult（失敗）をリストに追加してループ継続
    } finally {
        conn.close();
    }
}
```

---

## JAR ビルドと実行確認

Eclipse 上での動作確認が取れたら、JAR ファイルとしてビルドしてコマンドラインから実行する手順を体験する。
バッチは本番環境ではコマンドラインやスケジューラから JAR を直接実行するのが一般的。

---

### classpath とは

JAR を実行するには、Java に「このプログラムが使う外部ライブラリ（JAR）はどこにあるか」を教える必要がある。
これを **classpath**（クラスパス）と呼ぶ。

```
【Eclipse 上】                        【コマンドライン実行】
Eclipse がビルドパスを自動で解決     → 自分で -cp オプションで指定する必要がある

ビルドパスに追加した JAR
├── mssql-jdbc-12.x.x.jre11.jar      → -cp に明示的に含める
└── logback-classic-x.x.x.jar        → -cp に明示的に含める
```

classpath を指定しないと `ClassNotFoundException` が発生する。

---

### 手順 1：依存 JAR を lib フォルダに集める

バッチ実行に必要な JAR をプロジェクトの `lib/` フォルダにまとめておく。

```
05_project_inventory_system/
└── lib/
    ├── mssql-jdbc-12.x.x.jre11.jar    ← JDBC ドライバ（課題⑤で追加済み）
    ├── slf4j-api-2.x.x.jar             ← ログ（課題⑥で追加済み）
    └── logback-classic-x.x.x.jar      ← ログ（課題⑥で追加済み）
```

---

### 手順 2：JAR ファイルのエクスポート

Eclipse から実行可能 JAR としてエクスポートする。

1. プロジェクトを右クリック →「エクスポート」→「Runnable JAR ファイル」を選択
2. 以下を設定する

| 項目 | 設定値 |
|---|---|
| 起動構成 | `BatchMain - 05_project_inventory_system` |
| エクスポート先 | `C:\work\inventory-batch.jar`（任意） |
| ライブラリー処理 | 「必要なライブラリーをサブフォルダーにコピー」を選択 |

3. エクスポートすると以下が生成される

```
C:\work\
├── inventory-batch.jar
└── inventory-batch_lib/            ← 依存 JAR が自動コピーされる
    ├── mssql-jdbc-12.x.x.jre11.jar
    └── logback-classic-x.x.x.jar
```

---

### 手順 3：コマンドラインから実行する

コマンドプロンプトで以下を実行する。

#### パターン A：Runnable JAR（マニフェストに classpath 設定済み）

```bat
cd C:\work
java -jar inventory-batch.jar
```

> Runnable JAR はマニフェスト（`MANIFEST.MF`）に `Main-Class` と `Class-Path` が自動設定されるため、`-cp` の指定が不要。

---

#### パターン B：通常の JAR（classpath を自分で指定する）

Eclipse で「JAR ファイル」として出力した場合は、`-cp` で classpath を指定する必要がある。

```bat
cd C:\work

:: Windows の classpath 区切りは「;」（セミコロン）
java -cp inventory-batch.jar;lib\mssql-jdbc-12.x.x.jre11.jar;lib\logback-classic-x.x.x.jar com.example.inventory.app.BatchMain
```

**classpath の書き方ガイド：**

| 要素 | 説明 | 例 |
|---|---|---|
| `-cp` または `-classpath` | classpath を指定するオプション | `-cp` |
| 自分の JAR | 自分でビルドした JAR | `inventory-batch.jar` |
| 区切り文字 | Windows は `;`、Mac/Linux は `:` | `;` |
| 依存 JAR | 外部ライブラリの JAR（複数ある場合も `;` で繋ぐ） | `lib\mssql-jdbc.jar;lib\logback.jar` |
| メインクラス | `main()` メソッドを持つクラスのフル修飾名 | `com.example.inventory.app.BatchMain` |

**ワイルドカードで lib フォルダ内の JAR を一括指定：**

```bat
:: lib\ 以下の全 JAR を一括指定（Java 6 以降）
java -cp inventory-batch.jar;lib\* com.example.inventory.app.BatchMain
```

---

### 手順 4：実行時のよくあるエラー

| エラー | 原因 | 対処 |
|---|---|---|
| `ClassNotFoundException: com.microsoft.sqlserver...` | JDBC ドライバが classpath に含まれていない | `-cp` に `mssql-jdbc.jar` を追加する |
| `Could not find or load main class BatchMain` | メインクラスのパッケージ名が間違っている | フル修飾名（`パッケージ名.クラス名`）で指定する |
| `batch_input/stock_batch.csv が見つからない` | 実行ディレクトリが違う | `cd` でプロジェクトフォルダに移動してから実行する |
| `NoClassDefFoundError` | コンパイル時と実行時で classpath が違う | 実行時の `-cp` にも同じ JAR を含める |

---

---

### 手順 5：バッチスクリプト化

毎回コマンドを打つのではなく、`.bat` ファイルにまとめて1クリックで実行できるようにする。
実務でもバッチは `.bat` や `.sh` から起動するのが一般的。

#### 作成するファイル

```
05_project_inventory_system/
└── scripts/
    └── run_batch.bat
```

#### `.bat` の基本構文ガイド

```bat
@echo off                        ← コマンド自体をコンソールに表示しない
:: これはコメント               ← :: でコメントを書く

set 変数名=値                    ← 変数の定義
echo %変数名%                    ← 変数の参照（% で囲む）
echo メッセージ                  ← コンソールに出力
echo.                            ← 空行を出力

if %ERRORLEVEL% equ 0 (         ← 終了コードが 0（成功）なら
    echo 成功
) else (
    echo 失敗
)

pause                            ← 「続行するには何かキーを押してください」で一時停止
exit /b 0                        ← スクリプトを終了コード 0 で終了
```

> **`%ERRORLEVEL%` とは：**
> 直前に実行したコマンド（ここでは java）の終了コード。
> Java 側で `System.exit(0)` → 成功、`System.exit(1)` → 失敗として受け取れる。

#### 実装するスクリプト：`run_batch.bat`

以下の仕様でスクリプトを作成すること。

| 項目 | 内容 |
|---|---|
| JAR の呼び出し | `inventory-batch.jar` を `lib\*` で classpath 指定して実行 |
| 終了コードの確認 | Java の終了コードに応じて成功・失敗メッセージを表示 |
| ログ出力 | 実行日時・終了コードを `bat_logs\` フォルダにログとして書き出す |

```bat
@echo off
:: ============================================================
:: run_batch.bat  入出庫バッチ起動スクリプト
:: ============================================================

:: ---- 設定 --------------------------------------------------
set JAVA_HOME=C:\pleiades\java\17
set BASE_DIR=%~dp0..
set JAR=%BASE_DIR%\inventory-batch.jar
set LIB=%BASE_DIR%\lib
set MAIN=com.example.inventory.app.BatchMain
set LOG_DIR=%BASE_DIR%\bat_logs

:: 実行日時をファイル名用に取得（yyyyMMdd_HHmmss 形式）
set STAMP=%DATE:~0,4%%DATE:~5,2%%DATE:~8,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%
set STAMP=%STAMP: =0%
set LOG_FILE=%LOG_DIR%\batch_%STAMP%.log
:: ------------------------------------------------------------

:: ---- ログフォルダ作成 --------------------------------------
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: ---- 開始ログ ----------------------------------------------
echo ===== 入出庫バッチ 開始 ===== >> "%LOG_FILE%"
echo 開始日時：%DATE% %TIME%       >> "%LOG_FILE%"
echo JAR     ：%JAR%               >> "%LOG_FILE%"

echo ===== 入出庫バッチ 開始 =====
echo 開始日時：%DATE% %TIME%

:: ---- Java の存在確認 ---------------------------------------
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] Java が見つかりません：%JAVA_HOME%
    echo [ERROR] Java が見つかりません：%JAVA_HOME% >> "%LOG_FILE%"
    exit /b 1
)

:: ---- JAR の存在確認 ----------------------------------------
if not exist "%JAR%" (
    echo [ERROR] JAR が見つかりません：%JAR%
    echo [ERROR] JAR が見つかりません：%JAR% >> "%LOG_FILE%"
    exit /b 1
)

:: ---- JAR の実行 --------------------------------------------
"%JAVA_HOME%\bin\java" -cp "%JAR%;%LIB%\*" %MAIN%
set EXIT_CODE=%ERRORLEVEL%

:: ---- 終了コードの確認 --------------------------------------
echo 終了コード：%EXIT_CODE%
echo 終了コード：%EXIT_CODE% >> "%LOG_FILE%"

if %EXIT_CODE% equ 0 (
    echo ===== 入出庫バッチ 正常終了 =====
    echo ===== 入出庫バッチ 正常終了 ===== >> "%LOG_FILE%"
) else (
    echo [ERROR] ===== 入出庫バッチ 異常終了 =====
    echo [ERROR] ===== 入出庫バッチ 異常終了 ===== >> "%LOG_FILE%"
)

echo 終了日時：%DATE% %TIME%       >> "%LOG_FILE%"

pause
exit /b %EXIT_CODE%
```

> **`%~dp0` とは：**
> スクリプト自身が置かれているフォルダのパスを返す特殊変数。
> `scripts\` フォルダから `..` で1つ上のプロジェクトルートを参照している。
> これにより、どこから `.bat` を実行しても正しいパスで動作する。

#### フォルダ構成（スクリプト追加後）

```
05_project_inventory_system/
├── inventory-batch.jar
├── lib/
│   └── mssql-jdbc-12.x.x.jre11.jar
├── scripts/
│   └── run_batch.bat          ← 作成するスクリプト
├── batch_input/
│   └── stock_batch.csv
├── batch_output/              ← Java のログ出力先
│   └── batch_result_*.log
└── bat_logs/                  ← .bat のログ出力先（自動生成）
    └── batch_*.log
```

> **Java ログと .bat ログの使い分け：**
> - `batch_output/`：Java バッチの処理内容（何件成功・スキップしたかなど）
> - `bat_logs/`：スクリプトの起動・終了・終了コードの記録
> 実務では「バッチが起動したかどうか」と「バッチの中身」を別ログに分けることが多い。

---

## 動作確認項目


## 動作確認項目

| # | 確認内容 |
|---|----------|
| 1 | バッチ実行後にコンソールにサマリーが表示される |
| 2 | 既存部品（P001・P002）の在庫が正しく更新されている（SSMS で確認） |
| 3 | 未登録部品（P006）が `parts` テーブルに自動登録されている（SSMS で確認） |
| 4 | P006 の入庫履歴が `stock_transactions` に INSERT されている |
| 5 | P999 の OUT でロールバックされ、`parts` に P999 が残っていない |
| 6 | エラー行（在庫不足・フォーマット不正・種別不正）がスキップされている |
| 7 | `batch_output/` に Java のログファイルが生成されている |
| 8 | ログに「部品自動登録」の件数が記録されている |
| 9 | JAR ファイルがエクスポートできている |
| 10 | コマンドラインから JAR を実行して正常に動作する |
| 11 | classpath を自分で指定してコマンドラインから実行できる（パターン B）|
| 12 | `run_batch.bat` をダブルクリックで実行して JAR が起動する |
| 13 | `bat_logs/` にスクリプトのログファイルが生成されている |
| 14 | Java を異常終了（`System.exit(1)`）させたとき、`.bat` ログに `[ERROR]` が記録される |

---

## 提出・確認方法

1. サンプル CSV を使ってバッチを実行し、コンソール出力をデモする
2. SSMS でテーブルの中身を SELECT して在庫・履歴への反映を確認する
3. Java のログファイルをテキストエディタで開いて見せる
4. コマンドラインから JAR を実行してデモする（classpath を自分で指定するパターン B でも実行できること）
5. `run_batch.bat` をダブルクリックで実行して `.bat` ログが出力されることをデモする
6. 担当者から以下を口頭で説明できること
   - 「なぜ 1 件失敗しても全体を止めないのか」
   - 「1 行ごとにトランザクションを分けた理由」
   - 「未登録部品の自動 INSERT が OUT 失敗時にロールバックされる理由」
   - 「classpath とは何か・なぜ指定が必要か」
   - 「`%ERRORLEVEL%` で何を判定しているか」
   - 「Java ログと .bat ログを分けた理由」
