# 課題仕様書 ⑪ SQL 応用：CASE 文・JOIN・GROUP BY・サブクエリ

**フェーズ：** Phase 3　**期間目安：** 3〜4 日　**担当：** 新人

---

## 目的

実務で頻出する SQL パターンを習得する。
CASE 文・JOIN・GROUP BY・サブクエリを組み合わせて、
段階的に複雑なクエリを書けるようにする。

---

## 背景（業務イメージ）

部品在庫管理システムのデータを使って、
「現場が実際に欲しい情報」を SQL で取り出すレポートを作る。
各チャプターで SQL を SSMS で確認してから、Java で実装する流れで進める。

---

## 事前準備：テストデータ投入

分析が成り立つよう、SSMS で以下を実行してテストデータを投入すること。

```sql
USE InventoryTraining;

INSERT INTO stock_transactions (transaction_id, part_code, type, quantity, transaction_at) VALUES
('TX0001', 'P001', 'IN',  200, '2025-03-01 09:00:00'),
('TX0002', 'P001', 'OUT',  30, '2025-03-03 10:00:00'),
('TX0003', 'P001', 'OUT',  50, '2025-03-07 14:00:00'),
('TX0004', 'P001', 'IN',  100, '2025-03-10 09:00:00'),
('TX0005', 'P001', 'OUT',  80, '2025-03-15 11:00:00'),
('TX0006', 'P002', 'IN',  500, '2025-03-01 09:00:00'),
('TX0007', 'P002', 'OUT', 100, '2025-03-05 10:00:00'),
('TX0008', 'P002', 'OUT',  80, '2025-03-10 11:00:00'),
('TX0009', 'P002', 'IN',  200, '2025-03-15 09:00:00'),
('TX0010', 'P002', 'OUT',  60, '2025-03-20 14:00:00'),
('TX0011', 'P003', 'IN',  300, '2025-03-01 09:00:00'),
('TX0012', 'P003', 'OUT',  10, '2025-03-08 10:00:00'),
('TX0013', 'P003', 'OUT',   5, '2025-03-16 11:00:00');
```

---

## プロジェクト構成

課題 ⑤ のプロジェクトに追加する。

```
05_project_inventory_system/
└── src/
    └── inventory/
        ├── dao/
        │   └── ReportDao.java        ← 新規追加（レポート用クエリ専用）
        ├── model/
        │   └── ReportRow.java        ← 新規追加（汎用レポート行）
        ├── service/
        │   └── ReportService.java    ← 新規追加
        └── app/
            └── ReportMain.java       ← 新規追加

report_output/                        ← プロジェクト直下に作成
├── report_01_type_label.csv
├── report_02_stock_status.csv
├── report_03_summary.csv
├── report_04_joined.csv
└── report_05_subquery.csv
```

---

## チャプター 1：CASE 文の基礎

### 何を学ぶか

`CASE` 文を使って、DB の値を「条件によって別の値に変換」する方法を学ぶ。
Java でいう `if-else` / `switch` に相当する SQL の仕組み。

### CASE 文の構文

```sql
-- 単純 CASE（値が一致するか）
CASE カラム名
    WHEN '値1' THEN '変換後1'
    WHEN '値2' THEN '変換後2'
    ELSE 'それ以外'
END

-- 検索 CASE（条件式で判定）
CASE
    WHEN 条件式1 THEN '変換後1'
    WHEN 条件式2 THEN '変換後2'
    ELSE 'それ以外'
END
```

### 動作確認クエリ（SSMS で試すこと）

```sql
-- ① 単純 CASE：IN/OUT を日本語に変換する
SELECT
    part_code,
    CASE type
        WHEN 'IN'  THEN '入庫'
        WHEN 'OUT' THEN '出庫'
        ELSE '不明'
    END AS type_label,
    quantity,
    transaction_at
FROM stock_transactions
ORDER BY transaction_at;

-- ② 検索 CASE：在庫数のレベルを判定する
SELECT
    part_code,
    part_name,
    stock,
    CASE
        WHEN stock >= 100 THEN '十分'
        WHEN stock >= 50  THEN '普通'
        WHEN stock >= 10  THEN '少ない'
        ELSE                   '危険'
    END AS stock_status
FROM parts
ORDER BY stock DESC;

-- ③ CASE を数値変換に使う（集計の前処理）
--    IN は +quantity、OUT は -quantity として扱う
SELECT
    part_code,
    type,
    quantity,
    CASE type
        WHEN 'IN'  THEN  quantity
        WHEN 'OUT' THEN -quantity
    END AS signed_quantity    -- 入庫はプラス、出庫はマイナスで表現
FROM stock_transactions
ORDER BY part_code, transaction_at;
```

### 課題 1：レポート出力

**レポート 01**（`report_01_type_label.csv`）を出力すること。

| 出力カラム | 内容 |
|---|---|
| 部品コード | `part_code` |
| 種別 | `IN` → `入庫`、`OUT` → `出庫`（CASE 文で変換） |
| 数量 | `quantity` |
| 取引日時 | `transaction_at` |

**レポート 02**（`report_02_stock_status.csv`）を出力すること。

| 出力カラム | 内容 |
|---|---|
| 部品コード | `part_code` |
| 部品名 | `part_name` |
| 在庫数 | `stock` |
| 在庫ステータス | 100以上→`十分` / 50以上→`普通` / 10以上→`少ない` / それ以下→`危険` |

---

## チャプター 2：GROUP BY と集計関数

### 何を学ぶか

`GROUP BY` でデータをグループ化し、`SUM` / `COUNT` / `AVG` などで集計する方法を学ぶ。
さらに `CASE` と組み合わせることで、条件付き集計ができるようになる。

### 動作確認クエリ（SSMS で試すこと）

```sql
-- ④ GROUP BY の基本：部品ごとの取引件数と合計数量
SELECT
    part_code,
    COUNT(*)       AS tx_count,
    SUM(quantity)  AS total_qty,
    MAX(quantity)  AS max_qty,
    MIN(quantity)  AS min_qty,
    AVG(quantity)  AS avg_qty
FROM stock_transactions
GROUP BY part_code
ORDER BY part_code;

-- ⑤ CASE + SUM（条件付き集計）：入庫合計と出庫合計を同じ行に並べる
--    ※ GROUP BY で集計するときに CASE で振り分けるのは実務で最頻出のパターン
SELECT
    part_code,
    SUM(CASE WHEN type = 'IN'  THEN quantity ELSE 0 END) AS total_in,
    SUM(CASE WHEN type = 'OUT' THEN quantity ELSE 0 END) AS total_out,
    SUM(CASE WHEN type = 'IN'  THEN quantity ELSE -quantity END) AS net_change
FROM stock_transactions
GROUP BY part_code
ORDER BY part_code;

-- ⑥ HAVING：集計後の絞り込み（WHERE との違いに注目）
--    出庫合計が 100 以上の部品だけ表示する
SELECT
    part_code,
    SUM(CASE WHEN type = 'OUT' THEN quantity ELSE 0 END) AS total_out
FROM stock_transactions
GROUP BY part_code
HAVING SUM(CASE WHEN type = 'OUT' THEN quantity ELSE 0 END) >= 100
ORDER BY total_out DESC;
```

> **WHERE と HAVING の違い：**
> - `WHERE`：集計前の行を絞り込む
> - `HAVING`：集計後の結果を絞り込む
> `HAVING` に集計関数を書けるが、`WHERE` には書けない。

### 課題 2：レポート出力

**レポート 03**（`report_03_summary.csv`）を出力すること。

| 出力カラム | 内容 |
|---|---|
| 部品コード | `part_code` |
| 入庫合計 | 入庫数量の合計（`SUM` + `CASE`） |
| 出庫合計 | 出庫数量の合計（`SUM` + `CASE`） |
| 差引数量 | 入庫合計 − 出庫合計 |
| 取引件数 | `COUNT(*)` |
| 平均数量 | `AVG(quantity)`（小数点第1位まで） |

---

## チャプター 3：JOIN

### 何を学ぶか

複数テーブルを結合して、関連するデータをまとめて取得する方法を学ぶ。
`INNER JOIN` / `LEFT JOIN` の違いを理解する。

### JOIN の構文

```sql
-- INNER JOIN：両方のテーブルに一致する行のみ返す
SELECT ...
FROM テーブルA
INNER JOIN テーブルB ON テーブルA.キー = テーブルB.キー;

-- LEFT JOIN：左テーブルの全行を返す（右に一致がなければ NULL）
SELECT ...
FROM テーブルA
LEFT JOIN テーブルB ON テーブルA.キー = テーブルB.キー;
```

### 動作確認クエリ（SSMS で試すこと）

```sql
-- ⑦ INNER JOIN：取引履歴に部品名を付ける
SELECT
    t.transaction_id,
    t.part_code,
    p.part_name,
    t.type,
    t.quantity,
    t.transaction_at
FROM stock_transactions t
INNER JOIN parts p ON t.part_code = p.part_code
ORDER BY t.transaction_at;

-- ⑧ INNER JOIN + CASE + GROUP BY：部品名付きで入出庫を集計する
SELECT
    t.part_code,
    p.part_name,
    SUM(CASE WHEN t.type = 'IN'  THEN t.quantity ELSE 0 END) AS total_in,
    SUM(CASE WHEN t.type = 'OUT' THEN t.quantity ELSE 0 END) AS total_out
FROM stock_transactions t
INNER JOIN parts p ON t.part_code = p.part_code
GROUP BY t.part_code, p.part_name
ORDER BY t.part_code;

-- ⑨ LEFT JOIN の確認：取引のない部品も含めて一覧表示
--    （まず取引のない部品を parts に追加してから実行すること）
INSERT INTO parts (part_code, part_name, stock, warehouse_code)
VALUES ('P099', 'テスト部品', 0, 'WH01');

SELECT
    p.part_code,
    p.part_name,
    p.stock,
    COUNT(t.transaction_id) AS tx_count   -- 取引がなければ 0 になる
FROM parts p
LEFT JOIN stock_transactions t ON p.part_code = t.part_code
GROUP BY p.part_code, p.part_name, p.stock
ORDER BY p.part_code;
```

> **INNER JOIN と LEFT JOIN の違いを目で確認すること。**
> ⑨ のクエリを `INNER JOIN` に変えて実行し、P099 が消えることを確認する。

### 課題 3：レポート出力

**レポート 04**（`report_04_joined.csv`）を出力すること。

| 出力カラム | 内容 |
|---|---|
| 部品コード | `part_code` |
| 部品名 | `part_name`（JOIN で取得） |
| 現在庫数 | `stock`（parts テーブル） |
| 在庫ステータス | CASE 文で判定（チャプター 1 と同じ基準） |
| 入庫合計 | 入庫数量の合計 |
| 出庫合計 | 出庫数量の合計 |
| 取引件数 | 取引件数（取引のない部品は `0`） |

> **ポイント：** 取引のない部品も含めるため `LEFT JOIN` を使うこと。

---

## チャプター 4：サブクエリ

### 何を学ぶか

クエリの中に別のクエリを埋め込む「サブクエリ」を学ぶ。
`WHERE` 句や `FROM` 句でサブクエリを使う方法を理解する。

### サブクエリの種類

| 種類 | 使う場所 | 用途 |
|---|---|---|
| スカラーサブクエリ | `SELECT` / `WHERE` 句 | 1つの値を返す |
| テーブルサブクエリ | `FROM` 句 | 仮想テーブルとして使う |
| `IN` サブクエリ | `WHERE` 句 | 複数値との一致チェック |
| `EXISTS` サブクエリ | `WHERE` 句 | 行の存在チェック |

### 動作確認クエリ（SSMS で試すこと）

```sql
-- ⑩ スカラーサブクエリ：全体平均在庫数と各部品の在庫数を比較する
SELECT
    part_code,
    part_name,
    stock,
    (SELECT AVG(CAST(stock AS DECIMAL(10,2))) FROM parts) AS avg_stock,
    CASE
        WHEN stock > (SELECT AVG(CAST(stock AS DECIMAL(10,2))) FROM parts)
        THEN '平均以上'
        ELSE '平均以下'
    END AS vs_avg
FROM parts
ORDER BY stock DESC;

-- ⑪ テーブルサブクエリ（FROM 句）：出庫合計を計算した結果を parts と結合する
SELECT
    p.part_code,
    p.part_name,
    p.stock,
    COALESCE(s.total_out, 0) AS total_out   -- NULL を 0 に変換
FROM parts p
LEFT JOIN (
    SELECT
        part_code,
        SUM(quantity) AS total_out
    FROM stock_transactions
    WHERE type = 'OUT'
    GROUP BY part_code
) AS s ON p.part_code = s.part_code
ORDER BY total_out DESC;

-- ⑫ IN サブクエリ：出庫取引が 3 件以上ある部品を取得する
SELECT
    part_code,
    part_name,
    stock
FROM parts
WHERE part_code IN (
    SELECT part_code
    FROM stock_transactions
    WHERE type = 'OUT'
    GROUP BY part_code
    HAVING COUNT(*) >= 3
)
ORDER BY part_code;

-- ⑬ EXISTS サブクエリ：1度も出庫されていない部品を取得する
SELECT
    part_code,
    part_name,
    stock
FROM parts p
WHERE NOT EXISTS (
    SELECT 1
    FROM stock_transactions t
    WHERE t.part_code = p.part_code
    AND   t.type = 'OUT'
)
ORDER BY part_code;
```

> **IN と EXISTS の違い：**
> - `IN`：サブクエリの結果リストと一致するか
> - `EXISTS`：サブクエリが 1 行でも返せば `true`
> データ量が多い場合 `EXISTS` の方が高速になることが多い。

### 課題 4：レポート出力

**レポート 05**（`report_05_subquery.csv`）を出力すること。

| 出力カラム | 内容 |
|---|---|
| 部品コード | `part_code` |
| 部品名 | `part_name` |
| 現在庫数 | `stock` |
| 全体平均在庫 | スカラーサブクエリで計算 |
| 平均比較 | 平均以上 / 平均以下（CASE 文で判定） |
| 出庫合計 | FROM 句サブクエリで計算（取引なしは `0`） |
| 出庫件数 | 出庫取引の件数（取引なしは `0`） |

---

## チャプター 5：バッチ INSERT（大量データの高速登録）

### 何を学ぶか

`PreparedStatement` の `addBatch()` / `executeBatch()` を使って、
複数件のデータを一括で INSERT する方法を学ぶ。

1 件ずつ INSERT を繰り返すと DB への往復通信が件数分発生するが、
バッチ INSERT はまとめて送信するため大量データの登録が高速になる。

```
【1件ずつの場合】                    【バッチ INSERTの場合】
Java → INSERT 1件 → DB              Java → INSERT 100件まとめて → DB
Java → INSERT 1件 → DB              （1回の通信で完了）
Java → INSERT 1件 → DB
...（100回繰り返す）
```

---

### PreparedStatement のバッチ機能

| メソッド | 説明 |
|---|---|
| `ps.addBatch()` | パラメータをセットした SQL をバッチに追加する（まだ実行しない） |
| `ps.executeBatch()` | バッチにたまった SQL をまとめて実行する |
| `ps.clearBatch()` | バッチをクリアする |
| `conn.setAutoCommit(false)` | バッチ実行前にトランザクションを開始する |

### 動作確認コード（SSMS で件数確認しながら試すこと）

```java
// ① 1 件ずつ INSERT（遅い方）
public void insertOneByOne(List<StockTransaction> list) throws SQLException {
    String sql = "INSERT INTO stock_transactions "
               + "(transaction_id, part_code, type, quantity) VALUES (?, ?, ?, ?)";

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        conn.setAutoCommit(false);
        for (StockTransaction tx : list) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getPartCode());
            ps.setString(3, tx.getType());
            ps.setInt   (4, tx.getQuantity());
            ps.executeUpdate();   // ← 1 件ごとに DB に送信
        }
        conn.commit();
    }
}

// ② バッチ INSERT（速い方）
public void insertBatch(List<StockTransaction> list) throws SQLException {
    String sql = "INSERT INTO stock_transactions "
               + "(transaction_id, part_code, type, quantity) VALUES (?, ?, ?, ?)";

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        conn.setAutoCommit(false);
        for (StockTransaction tx : list) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getPartCode());
            ps.setString(3, tx.getType());
            ps.setInt   (4, tx.getQuantity());
            ps.addBatch();        // ← バッチに追加するだけ（まだ送信しない）
        }
        ps.executeBatch();        // ← ここで全件まとめて送信
        conn.commit();
    }
}
```

### バッチサイズの分割

件数が多い場合（数千〜数万件）は、一定件数ごとに `executeBatch()` を呼ぶ方が安全。
一度に大量のデータを送りすぎるとメモリ不足やタイムアウトになることがある。

```java
public void insertBatchWithChunk(List<StockTransaction> list, int chunkSize)
        throws SQLException {
    String sql = "INSERT INTO stock_transactions "
               + "(transaction_id, part_code, type, quantity) VALUES (?, ?, ?, ?)";

    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        conn.setAutoCommit(false);

        for (int i = 0; i < list.size(); i++) {
            StockTransaction tx = list.get(i);
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getPartCode());
            ps.setString(3, tx.getType());
            ps.setInt   (4, tx.getQuantity());
            ps.addBatch();

            // chunkSize 件ごとに実行してバッチをクリア
            if ((i + 1) % chunkSize == 0) {
                ps.executeBatch();
                ps.clearBatch();
                System.out.println((i + 1) + " 件処理済み");
            }
        }

        // 残りを実行
        ps.executeBatch();
        conn.commit();
    }
}
```

> **ポイント：** `chunkSize` は 500〜1000 件程度が目安。
> DB や環境によって最適な値は異なるため、実務では計測しながら調整する。

---

### 課題 5：バッチ INSERT の実装と計測

#### 手順 1：テストデータの生成

`BatchInsertMain.java` を作成し、テスト用の入出庫データを 1000 件分メモリ上に生成する。

```java
List<StockTransaction> testData = new ArrayList<>();
for (int i = 1; i <= 1000; i++) {
    String txId    = String.format("TEST%04d", i);
    String type    = (i % 2 == 0) ? "IN" : "OUT";
    String partCode = "P00" + (i % 3 + 1);   // P001〜P003 をローテーション
    testData.add(new StockTransaction(txId, partCode, type, 1));
}
```

#### 手順 2：1件ずつ INSERT と バッチ INSERT の両方を実装して実行時間を計測する

```java
// 計測の例
long start = System.currentTimeMillis();
dao.insertOneByOne(testData);
long end = System.currentTimeMillis();
System.out.println("1件ずつ：" + (end - start) + " ms");

// テーブルをクリアして再計測
// ...

start = System.currentTimeMillis();
dao.insertBatch(testData);
end = System.currentTimeMillis();
System.out.println("バッチ  ：" + (end - start) + " ms");
```

#### 手順 3：チャンク分割バージョンも試す

`chunkSize` を 100 / 500 / 1000 と変えて計測し、違いを確認する。

#### 結果を以下の表にまとめて提出すること

| 実装方法 | 件数 | 実行時間（ms） |
|---|---|---|
| 1 件ずつ INSERT | 1000 件 | |
| バッチ INSERT（一括） | 1000 件 | |
| バッチ INSERT（chunk=100） | 1000 件 | |
| バッチ INSERT（chunk=500） | 1000 件 | |

---

## 実装クラス

### ReportRow クラス（`model/ReportRow.java`）

各レポートの 1 行を表す汎用クラス。カラム名と値を `Map` で保持する。

```java
public class ReportRow {
    private final Map<String, String> columns = new LinkedHashMap<>();

    public void put(String key, String value) {
        columns.put(key, value == null ? "" : value);
    }

    public Map<String, String> getColumns() {
        return columns;
    }
}
```

### ReportDao クラス（`dao/ReportDao.java`）

各レポート用のクエリを実行して `List<ReportRow>` を返すクラス。

| メソッド | 説明 |
|---|---|
| `List<ReportRow> fetchTypeLabel()` | レポート 01 用 |
| `List<ReportRow> fetchStockStatus()` | レポート 02 用 |
| `List<ReportRow> fetchSummary()` | レポート 03 用 |
| `List<ReportRow> fetchJoined()` | レポート 04 用 |
| `List<ReportRow> fetchSubquery()` | レポート 05 用 |

### ReportService クラス（`service/ReportService.java`）

`ReportDao` からデータを取得して CSV に書き出すクラス。

```java
public void exportCsv(String filePath, List<ReportRow> rows) {
    // 1行目にヘッダー（rows.get(0).getColumns().keySet()）を書く
    // 2行目以降にデータを書く
}
```

---

## 業務ルール・実装上の注意

| 項目 | 内容 |
|---|---|
| NULL の扱い | `COALESCE(値, 0)` で NULL を 0 に変換すること |
| 小数点 | `AVG` は `CAST(... AS DECIMAL(10,2))` で精度を指定すること |
| 文字コード | CSV は UTF-8 BOM 付きで出力すること（Excel で開いたとき文字化けしない） |
| LEFT JOIN | 取引のない部品が抜け落ちないよう注意すること |

---

## 動作確認項目

| # | 確認内容 |
|---|----------|
| 1 | 5 つの CSV がすべて生成される |
| 2 | レポート 01 の種別が「入庫」「出庫」と日本語で表示されている |
| 3 | レポート 02 の在庫ステータスが正しく判定されている |
| 4 | レポート 03 の入出庫合計が SSMS の手動集計と一致している |
| 5 | レポート 04 に取引のない部品（P099）が含まれている |
| 6 | レポート 05 の平均比較が正しく判定されている |
| 7 | NULL が含まれる行でエラーにならない |

---

## 提出・確認方法

1. 5 つの CSV が生成されることをデモする
2. SSMS で各クエリを直接実行し、CSV の値と一致していることを確認する
3. 以下を口頭で説明できること
   - `CASE` 文の単純 CASE と検索 CASE の使い分け
   - `WHERE` と `HAVING` の違い
   - `INNER JOIN` と `LEFT JOIN` の違い
   - `IN` と `EXISTS` の違い
   - `COALESCE` を使う理由
