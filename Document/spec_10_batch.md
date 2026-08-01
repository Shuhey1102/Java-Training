# 課題仕様書 ⑩ バッチ処理：CSV から入出庫データを一括処理

**フェーズ：** Phase 3　**期間目安：** 3〜4 日　**担当：** 新人

---

## 目的

CSV ファイルに記載された複数の入出庫データを、プログラムが自動で読み込んで一括処理する
「バッチ処理」を実装する。

これまでの課題では画面やコンソールから 1 件ずつ手動で操作していたが、
実務では大量データを自動で処理するバッチが多く使われる。
バッチの基本的な構造（読み込み → 処理 → 結果出力）を体験する。

---

## 背景（業務イメージ）

工場では毎朝、前日の入出庫データが CSV ファイルとして送られてきます。
このファイルを手作業で 1 件ずつ入力するのは非効率です。

そこで「CSV を読み込んで自動的に在庫を更新するバッチプログラム」を作ります。
プログラムを実行するだけで、全件の入出庫処理が完了します。

---

## バッチの全体的な動き

```
起動
  └─→ 処理対象 CSV を読み込む
        └─→ 1 行ずつ処理（入庫 or 出庫）
              ├─→ 成功：在庫更新 + 履歴 DB に INSERT + 成功ログ出力
              └─→ 失敗（在庫不足・部品なしなど）：その行をスキップ + エラーログ出力
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

**フォーマット：**
```csv
partCode,type,quantity
P001,IN,100
P002,OUT,20
P003,IN,50
P999,OUT,10
P002,OUT,9999
```

| カラム | 型 | 説明 | 例 |
|---|---|---|---|
| `partCode` | `String` | 部品コード | `P001` |
| `type` | `String` | 種別（`IN`=入庫 / `OUT`=出庫） | `IN` |
| `quantity` | `int` | 数量（1 以上） | `100` |

**ルール：**
- 1 行目はヘッダー行（処理対象外）
- 空行は無視する
- カラム数が不正な行はスキップしてエラーログに出力する

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
    c. type が "IN" なら inventoryService.stockIn() を呼び出す
       type が "OUT" なら inventoryService.stockOut() を呼び出す
    d. 成功・失敗に関わらず BatchResult を生成してリストに追加
    e. 失敗してもループを止めず次の行へ進む
4. 全行処理後、結果リストを返す
```

**バリデーション（パース時に実施）：**

| チェック内容 | エラーメッセージ例 |
|---|---|
| カラム数が 3 つでない | `フォーマット不正（カラム数エラー）` |
| `type` が `IN` / `OUT` 以外 | `種別が不正です：xxx` |
| `quantity` が整数でない | `数量が整数ではありません：xxx` |
| `quantity` が 1 未満 | `数量は 1 以上を指定してください` |

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
[3行目] P003 / 入庫 / 50個  → 成功（在庫：200 → 250）
[4行目] P999 / 出庫 / 10個  → スキップ（エラー：部品コード P999 は存在しません）
[5行目] P002 / 出庫 / 9999個 → スキップ（エラー：在庫不足：要求数=9999, 現在庫=30）

===== 処理結果サマリー =====
処理件数：5 件
  成功：3 件
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
P003,IN,50
P999,OUT,10
P002,OUT,9999
INVALID_ROW_NO_COLUMNS
P001,UNKNOWN,10
P003,OUT,0
```

**期待される結果：**

| 行 | 内容 | 期待結果 |
|---|---|---|
| 1行目 | P001 入庫 100 | 成功 |
| 2行目 | P002 出庫 20 | 成功 |
| 3行目 | P003 入庫 50 | 成功 |
| 4行目 | P999 出庫 10 | スキップ（部品なし） |
| 5行目 | P002 出庫 9999 | スキップ（在庫不足） |
| 6行目 | カラム数不正 | スキップ（フォーマット不正） |
| 7行目 | 種別 UNKNOWN | スキップ（種別不正） |
| 8行目 | P003 出庫 0 | スキップ（数量不正） |

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

## 動作確認項目

| # | 確認内容 |
|---|----------|
| 1 | バッチ実行後にコンソールにサマリーが表示される |
| 2 | 成功した行の在庫が SSMS で正しく更新されている |
| 3 | 成功した行の履歴が `stock_transactions` テーブルに INSERT されている |
| 4 | エラー行（P999・在庫不足・フォーマット不正など）がスキップされている |
| 5 | スキップ行の在庫・履歴が変わっていない（ロールバック確認） |
| 6 | `batch_output/` にログファイルが生成されている |
| 7 | ログファイルの内容がコンソール出力と一致している |

---

## 提出・確認方法

1. サンプル CSV を使ってバッチを実行し、コンソール出力をデモする
2. SSMS でテーブルの中身を SELECT して在庫・履歴への反映を確認する
3. ログファイルをテキストエディタで開いて見せる
4. 担当者から「なぜ 1 件失敗しても全体を止めないのか」「1 行ごとにトランザクションを分けた理由」「ログを残す目的」を口頭で説明できること
