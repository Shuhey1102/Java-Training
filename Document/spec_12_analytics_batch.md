# 課題仕様書 ⑫ 分析バッチ：ウィンドウ関数を使った在庫分析

**フェーズ：** Phase 3　**期間目安：** 4〜5 日　**担当：** 新人

---

## 目的

SQL のウィンドウ関数（`ROW_NUMBER` / `RANK` / `LAG` / `SUM OVER` など）を使った
分析クエリを Java から実行し、結果を CSV に出力するバッチを実装する。

「データを取得して表示する」だけでなく、**DB 側で集計・分析を行い結果を活用する**
という実務に近いパターンを体験する。

---

## 背景（業務イメージ）

部品在庫管理システムが稼働し、入出庫データが蓄積されてきました。
経営層や現場から「在庫の動きを分析したい」という要望が来ています。

今回は以下の 3 つの分析レポートをバッチで自動生成します。

| レポート | 内容 |
|---|---|
| レポート A | 在庫推移分析：部品ごとの在庫増減トレンド |
| レポート B | 入出庫ランキング：期間内の入出庫量 TOP 分析 |
| レポート C | 在庫アラート分析：直近の在庫推移から枯渇リスクを検出 |

---

## 事前準備：分析用テストデータの投入

実際の分析が成り立つよう、`stock_transactions` に十分なデータを用意すること。
以下の SQL を SSMS で実行してテストデータを投入する。

```sql
USE InventoryTraining;

-- 過去 30 日分の入出庫データを投入
INSERT INTO stock_transactions (transaction_id, part_code, type, quantity, transaction_at) VALUES
('TX0001', 'P001', 'IN',  200, '2025-03-01 09:00:00'),
('TX0002', 'P001', 'OUT',  30, '2025-03-03 10:00:00'),
('TX0003', 'P001', 'OUT',  50, '2025-03-07 14:00:00'),
('TX0004', 'P001', 'IN',  100, '2025-03-10 09:00:00'),
('TX0005', 'P001', 'OUT',  80, '2025-03-15 11:00:00'),
('TX0006', 'P001', 'OUT',  60, '2025-03-20 13:00:00'),
('TX0007', 'P001', 'OUT',  40, '2025-03-25 16:00:00'),
('TX0008', 'P002', 'IN',  500, '2025-03-01 09:00:00'),
('TX0009', 'P002', 'OUT', 100, '2025-03-05 10:00:00'),
('TX0010', 'P002', 'OUT',  80, '2025-03-10 11:00:00'),
('TX0011', 'P002', 'IN',  200, '2025-03-15 09:00:00'),
('TX0012', 'P002', 'OUT',  60, '2025-03-20 14:00:00'),
('TX0013', 'P003', 'IN',  300, '2025-03-01 09:00:00'),
('TX0014', 'P003', 'OUT',  10, '2025-03-08 10:00:00'),
('TX0015', 'P003', 'OUT',   5, '2025-03-16 11:00:00'),
('TX0016', 'P003', 'OUT',   8, '2025-03-24 15:00:00');
```

---

## プロジェクト構成

課題 ⑤ のプロジェクトに分析バッチ用クラスを追加する。

```
05_project_inventory_system/
└── src/
    └── inventory/
        ├── dao/
        │   └── AnalyticsDao.java          ← 新規追加（分析クエリ専用）
        ├── model/
        │   ├── StockTrend.java            ← 新規追加（レポート A 用）
        │   ├── TransactionRanking.java    ← 新規追加（レポート B 用）
        │   └── StockAlert.java            ← 新規追加（レポート C 用）
        ├── service/
        │   └── AnalyticsBatchService.java ← 新規追加（分析バッチ処理）
        └── app/
            └── AnalyticsBatchMain.java    ← 新規追加（エントリーポイント）

analytics_output/                          ← プロジェクト直下に作成（自動生成）
├── report_a_stock_trend_yyyyMMdd.csv
├── report_b_transaction_ranking_yyyyMMdd.csv
└── report_c_stock_alert_yyyyMMdd.csv
```

---

## ウィンドウ関数の基礎知識

実装前に以下を SSMS で実行して動作を確認すること。

### OVER 句の基本構文

```sql
関数名() OVER (
    PARTITION BY グループ化するカラム   -- グループ単位を指定
    ORDER BY     並び順カラム           -- グループ内の並び順
    ROWS BETWEEN 開始位置 AND 終了位置  -- 集計対象の行範囲（省略可）
)
```

### 主なウィンドウ関数

| 関数 | 用途 |
|---|---|
| `ROW_NUMBER()` | グループ内の連番（同値でも異なる番号） |
| `RANK()` | グループ内の順位（同値は同順位・次の番号をスキップ） |
| `DENSE_RANK()` | グループ内の順位（同値は同順位・番号をスキップしない） |
| `LAG(col, n)` | n 行前の値を取得 |
| `LEAD(col, n)` | n 行後の値を取得 |
| `SUM() OVER` | 累計・グループ内合計 |
| `AVG() OVER` | 移動平均 |

### ROWS BETWEEN の指定一覧

`SUM() OVER` などの集計系ウィンドウ関数で「どの行を集計対象にするか」を指定する構文。

| 指定 | 意味 |
|---|---|
| `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` | 先頭行〜現在行（累計に使う） |
| `ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING` | 現在行〜末尾行（残余合計に使う） |
| `ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING` | 先頭行〜末尾行（全体合計・全体平均に使う） |
| `ROWS BETWEEN 1 PRECEDING AND CURRENT ROW` | 1 行前〜現在行（直前 2 行の合計） |
| `ROWS BETWEEN 2 PRECEDING AND CURRENT ROW` | 2 行前〜現在行（直前 3 行の移動平均などに使う） |
| `ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING` | 1 行前〜1 行後（前後を含む移動平均） |

> **ポイント：** `ORDER BY` を指定した場合、`ROWS BETWEEN` を省略すると
> `UNBOUNDED PRECEDING AND CURRENT ROW` と同じ動作になる。
> ただし意図を明示するため、累計を計算する際は明示的に書くことが多い。

### 動作確認クエリ（SSMS で試すこと）

各クエリを実行して結果を目で確認してから実装に入ること。
「SQL が何を返すか」が分かった状態で実装する方が格段に速い。

```sql
-- ① ROW_NUMBER：部品ごとに入出庫の連番を振る
SELECT
    part_code,
    type,
    quantity,
    transaction_at,
    ROW_NUMBER() OVER (PARTITION BY part_code ORDER BY transaction_at) AS row_num
FROM stock_transactions
ORDER BY part_code, transaction_at;

-- ② RANK / DENSE_RANK の違い：数量の多い順に順位を付ける（同数量で挙動の差を確認）
SELECT
    part_code,
    quantity,
    RANK()       OVER (ORDER BY quantity DESC) AS rank_num,
    DENSE_RANK() OVER (ORDER BY quantity DESC) AS dense_rank_num,
    ROW_NUMBER() OVER (ORDER BY quantity DESC) AS row_num
FROM stock_transactions
ORDER BY quantity DESC;

-- ③ LAG / LEAD：前後の行の値を取得する
SELECT
    part_code,
    quantity,
    transaction_at,
    LAG(quantity,  1) OVER (PARTITION BY part_code ORDER BY transaction_at) AS prev_quantity,
    LEAD(quantity, 1) OVER (PARTITION BY part_code ORDER BY transaction_at) AS next_quantity
FROM stock_transactions
ORDER BY part_code, transaction_at;

-- ④ SUM OVER（ROWS BETWEEN なし）：部品ごとの累積合計（ORDER BY あり・省略時の動作）
SELECT
    part_code,
    type,
    quantity,
    transaction_at,
    SUM(quantity) OVER (PARTITION BY part_code ORDER BY transaction_at) AS cumulative_qty
FROM stock_transactions
ORDER BY part_code, transaction_at;

-- ⑤ SUM OVER（ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW）：④と同じ結果になることを確認
SELECT
    part_code,
    type,
    quantity,
    transaction_at,
    SUM(quantity) OVER (
        PARTITION BY part_code
        ORDER BY transaction_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS cumulative_qty
FROM stock_transactions
ORDER BY part_code, transaction_at;

-- ⑥ SUM OVER（ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING）：グループ全体合計
SELECT
    part_code,
    type,
    quantity,
    transaction_at,
    SUM(quantity) OVER (
        PARTITION BY part_code
        ORDER BY transaction_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS total_qty_all   -- 全行が同じ値（部品ごとの合計数量）になることを確認
FROM stock_transactions
ORDER BY part_code, transaction_at;

-- ⑦ SUM OVER（ROWS BETWEEN 2 PRECEDING AND CURRENT ROW）：直前 3 行の移動合計
SELECT
    part_code,
    type,
    quantity,
    transaction_at,
    SUM(quantity) OVER (
        PARTITION BY part_code
        ORDER BY transaction_at
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) AS moving_sum_3   -- 直前 2 行＋現在行の合計
FROM stock_transactions
ORDER BY part_code, transaction_at;

-- ⑧ AVG OVER（移動平均）：直前 2 行＋現在行の 3 行移動平均
SELECT
    part_code,
    quantity,
    transaction_at,
    AVG(CAST(quantity AS DECIMAL(10,2))) OVER (
        PARTITION BY part_code
        ORDER BY transaction_at
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) AS moving_avg_3
FROM stock_transactions
ORDER BY part_code, transaction_at;
```

---

### CTE（WITH 句）ガイド

#### CTE とは

CTE（Common Table Expression）とは、SQL の中で一時的に名前付きの結果セットを定義する構文。
`WITH` キーワードで始まるため「WITH 句」とも呼ばれる。

**サブクエリとの違い：**

```sql
-- サブクエリ：FROM 句の中にクエリが埋め込まれて読みにくい
SELECT * FROM (
    SELECT part_code, SUM(quantity) AS total FROM stock_transactions GROUP BY part_code
) AS summary
WHERE total > 100;

-- CTE：処理を名前付きで分離して読みやすい
WITH summary AS (
    SELECT part_code, SUM(quantity) AS total FROM stock_transactions GROUP BY part_code
)
SELECT * FROM summary
WHERE total > 100;
```

どちらも同じ結果になるが、CTE の方が「何を計算しているか」が名前で伝わる。

---

#### 基本構文

```sql
WITH CTE名 AS (
    -- ここに SQL を書く
    SELECT ...
)
SELECT *
FROM CTE名
WHERE ...;
```

---

#### 複数の CTE を繋げる

カンマ区切りで複数の CTE を定義できる。後の CTE は前の CTE を参照できる。

```sql
WITH
-- 第 1 段階：集計
step1 AS (
    SELECT part_code, SUM(quantity) AS total
    FROM stock_transactions
    GROUP BY part_code
),
-- 第 2 段階：step1 の結果に順位を付ける
step2 AS (
    SELECT
        part_code,
        total,
        RANK() OVER (ORDER BY total DESC) AS rnk
    FROM step1
)
-- 最終的な SELECT
SELECT * FROM step2 WHERE rnk <= 3;
```

---

#### 動作確認クエリ（SSMS で試すこと）

```sql
-- ⑨ CTE（1段階）：部品ごとの出庫合計を計算して名前を付ける
WITH out_summary AS (
    SELECT
        part_code,
        SUM(quantity) AS total_out
    FROM stock_transactions
    WHERE type = 'OUT'
    GROUP BY part_code
)
SELECT
    p.part_name,
    o.total_out
FROM out_summary o
INNER JOIN parts p ON o.part_code = p.part_code
ORDER BY o.total_out DESC;

-- ⑩ CTE（2段階）：出庫合計を計算した後、順位を付ける
WITH out_summary AS (
    SELECT
        part_code,
        SUM(quantity) AS total_out
    FROM stock_transactions
    WHERE type = 'OUT'
    GROUP BY part_code
),
ranked AS (
    SELECT
        part_code,
        total_out,
        RANK() OVER (ORDER BY total_out DESC) AS out_rank
    FROM out_summary
)
SELECT
    p.part_name,
    r.total_out,
    r.out_rank
FROM ranked r
INNER JOIN parts p ON r.part_code = p.part_code
ORDER BY r.out_rank;

-- ⑪ CTE ＋ ウィンドウ関数：累積在庫を計算してから最新行だけ取り出す
--    （レポート C に向けた準備。何をしているか読み解くこと）
WITH cumulative AS (
    SELECT
        part_code,
        transaction_at,
        SUM(CASE WHEN type = 'IN' THEN quantity ELSE -quantity END)
            OVER (PARTITION BY part_code ORDER BY transaction_at) AS cumulative_stock,
        ROW_NUMBER() OVER (PARTITION BY part_code ORDER BY transaction_at DESC) AS recent_rank
    FROM stock_transactions
)
SELECT
    p.part_name,
    c.cumulative_stock AS latest_stock,
    c.transaction_at   AS latest_at
FROM cumulative c
INNER JOIN parts p ON c.part_code = p.part_code
WHERE c.recent_rank = 1
ORDER BY c.cumulative_stock ASC;
```

> **ポイント：** ⑪ のクエリはレポート C の核心部分に近い構造になっている。
> 「CTE でウィンドウ関数を計算し、外側の WHERE で絞り込む」という流れを
> ここで理解しておくと実装がスムーズになる。

---



### 目的

部品ごとに取引日時順で **累積在庫数** を計算し、在庫がどのように増減してきたかを時系列で出力する。

### 取得要件

- `stock_transactions` と `parts` を結合し、部品ごと・取引日時順に全取引を取得する
- 各行に「その時点までの累積在庫数」を計算して付与する
  - 入庫（`IN`）は在庫を増やす、出庫（`OUT`）は在庫を減らす
- 各行に「部品ごとの連番」を付与する

### 考え方のヒント

- 累積在庫数は `SUM() OVER` で計算できる。`PARTITION BY` で部品ごとに区切り、`ORDER BY` で時系列順に累計する
- 連番は `ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)` で振れる
- まず「基礎知識」セクションの動作確認クエリを参考に SSMS で試してから実装すること

### 出力 CSV：`report_a_stock_trend_yyyyMMdd.csv`

```csv
部品コード,部品名,種別,数量,取引日時,累積在庫数,連番
P001,ボルト M6,入庫,200,2025-03-01 09:00:00,200,1
P001,ボルト M6,出庫,30,2025-03-03 10:00:00,170,2
P001,ボルト M6,出庫,50,2025-03-07 14:00:00,120,3
P001,ボルト M6,入庫,100,2025-03-10 09:00:00,220,4
...
```

### 実装クラス：`StockTrend.java`

| フィールド | 型 | 説明 |
|---|---|---|
| `partCode` | `String` | 部品コード |
| `partName` | `String` | 部品名 |
| `type` | `String` | 種別（入庫 / 出庫） |
| `quantity` | `int` | 数量 |
| `transactionAt` | `String` | 取引日時 |
| `cumulativeStock` | `int` | 累積在庫数 |
| `seq` | `int` | 連番 |

---

## レポート B：入出庫ランキング

### 目的

集計期間内に **最も多く入庫・出庫された部品のランキング** を出力する。
同数量の場合は同順位になるよう `RANK()` を使う。

### 取得要件

- 指定期間内（`REPORT_FROM` 〜 `REPORT_TO`）の取引を集計する
- 部品ごとに入庫合計・出庫合計をそれぞれ計算する
- 入庫合計の多い順・出庫合計の多い順にそれぞれ順位を付ける
  - 同じ数量の部品は同順位になること（`ROW_NUMBER` ではなく `RANK` を使う理由）

### 考え方のヒント

- まず `GROUP BY` で部品ごとの入庫合計・出庫合計を集計するサブクエリを作る
- その結果に対して `RANK() OVER (ORDER BY 合計数量 DESC)` で順位を付ける
- ウィンドウ関数はサブクエリの結果に対して適用できる（サブクエリを外側からラップする形）
- 集計期間は `PreparedStatement` の `?` で Java から渡す

### 出力 CSV：`report_b_transaction_ranking_yyyyMMdd.csv`

```csv
部品コード,部品名,入庫合計,入庫ランク,出庫合計,出庫ランク
P002,ナット M6,700,1,240,1
P001,ボルト M6,300,2,260,2
P003,ワッシャー M6,300,2,23,3
```

### 実装クラス：`TransactionRanking.java`

| フィールド | 型 | 説明 |
|---|---|---|
| `partCode` | `String` | 部品コード |
| `partName` | `String` | 部品名 |
| `totalInQuantity` | `int` | 入庫合計数量 |
| `inRank` | `int` | 入庫ランキング |
| `totalOutQuantity` | `int` | 出庫合計数量 |
| `outRank` | `int` | 出庫ランキング |

### 集計期間の渡し方

`AnalyticsBatchMain` から開始日・終了日を定数で定義して `AnalyticsDao` に渡す。

```java
// AnalyticsBatchMain.java
private static final String REPORT_FROM = "2025-03-01 00:00:00";
private static final String REPORT_TO   = "2025-03-31 23:59:59";
```

---

## レポート C：在庫アラート分析

### 目的

部品ごとに **直近 3 回の取引** を取り出し、`LAG()` で前回・前々回の在庫数と比較して
「連続して在庫が減少しているか」を判定する。連続減少している部品をアラート対象とする。

### 取得要件

- 部品ごとの累積在庫数を時系列で計算する（レポート A と同様）
- 各部品の「最新（直近 1 件）」の累積在庫数を取得する
- 同時に「1 つ前の取引時点」「2 つ前の取引時点」の累積在庫数も取得する
- 在庫数が少ない順に並べて出力する

### 考え方のヒント

- まず累積在庫を計算する CTE（`WITH` 句）を作る。レポート A で作った累積計算がそのまま使える
- 次に `ROW_NUMBER() OVER (PARTITION BY part_code ORDER BY transaction_at DESC)` で
  「各部品の直近から何番目か」を番号付けし、`WHERE recent_rank = 1` で最新行だけ絞り込む
- 前回・前々回の累積在庫は `LAG(cumulative_stock, 1)` / `LAG(cumulative_stock, 2)` で取得できる
- 先頭行の `LAG()` 結果は `null` になる。Java 側で `null` を正しく扱うこと（実装のヒント参照）
- 複数の処理を CTE で段階的に分けて書くと可読性が上がる

### アラート判定ロジック（Java 側で実装）

SQL の結果を受け取った後、Java 側で以下の条件を判定する。

| 判定 | 条件 | アラートレベル |
|---|---|---|
| 危険 | `latest_stock < 20` かつ `latest_stock < prev_stock` | `CRITICAL` |
| 警告 | `latest_stock < 50` かつ `latest_stock < prev_stock` | `WARNING` |
| 正常 | 上記以外 | `OK` |

### 出力 CSV：`report_c_stock_alert_yyyyMMdd.csv`

```csv
部品コード,部品名,直近在庫数,前回在庫数,前々回在庫数,最終取引日時,アラートレベル
P001,ボルト M6,40,100,120,2025-03-25 16:00:00,WARNING
P003,ワッシャー M6,277,282,285,2025-03-24 15:00:00,OK
P002,ナット M6,460,400,320,2025-03-20 14:00:00,OK
```

### 実装クラス：`StockAlert.java`

| フィールド | 型 | 説明 |
|---|---|---|
| `partCode` | `String` | 部品コード |
| `partName` | `String` | 部品名 |
| `latestStock` | `int` | 直近の累積在庫数 |
| `prevStock` | `Integer` | 前回の累積在庫数（null あり） |
| `prev2Stock` | `Integer` | 前々回の累積在庫数（null あり） |
| `latestAt` | `String` | 最終取引日時 |
| `alertLevel` | `String` | `CRITICAL` / `WARNING` / `OK` |

---

## AnalyticsDao クラス（`dao/AnalyticsDao.java`）

分析クエリ専用の DAO クラス。既存の `PartDao` / `TransactionDao` とは分離する。

| メソッド | 説明 |
|---|---|
| `List<StockTrend> fetchStockTrend()` | レポート A 用クエリを実行して返す |
| `List<TransactionRanking> fetchTransactionRanking(String from, String to)` | レポート B 用クエリを実行して返す |
| `List<StockAlert> fetchStockAlertBase()` | レポート C 用クエリを実行して返す（アラート判定前の生データ） |

---

## AnalyticsBatchService クラス（`service/AnalyticsBatchService.java`）

3 つのレポートを順番に生成して CSV に出力するクラス。

```java
public class AnalyticsBatchService {

    private final AnalyticsDao analyticsDao;

    public AnalyticsBatchService(AnalyticsDao analyticsDao) {
        this.analyticsDao = analyticsDao;
    }

    public void execute(String outputDir, String reportFrom, String reportTo) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // レポート A
        List<StockTrend> trends = analyticsDao.fetchStockTrend();
        writeCsv(outputDir + "/report_a_stock_trend_" + date + ".csv", trends);

        // レポート B
        List<TransactionRanking> rankings = analyticsDao.fetchTransactionRanking(reportFrom, reportTo);
        writeCsv(outputDir + "/report_b_transaction_ranking_" + date + ".csv", rankings);

        // レポート C：DB から取得後 Java 側でアラート判定
        List<StockAlert> alerts = analyticsDao.fetchStockAlertBase();
        alerts.forEach(a -> a.setAlertLevel(judgeAlertLevel(a)));
        writeCsv(outputDir + "/report_c_stock_alert_" + date + ".csv", alerts);
    }

    private String judgeAlertLevel(StockAlert alert) {
        // アラート判定ロジックをここに実装する
    }

    private <T> void writeCsv(String filePath, List<T> records) {
        // CSV 書き出しロジックをここに実装する
    }
}
```

---

## AnalyticsBatchMain クラス（`app/AnalyticsBatchMain.java`）

```java
public class AnalyticsBatchMain {

    private static final String OUTPUT_DIR   = "analytics_output";
    private static final String REPORT_FROM  = "2025-03-01 00:00:00";
    private static final String REPORT_TO    = "2025-03-31 23:59:59";

    public static void main(String[] args) {
        System.out.println("===== 在庫分析バッチ 開始 =====");

        AnalyticsDao dao = new AnalyticsDao();
        AnalyticsBatchService service = new AnalyticsBatchService(dao);
        service.execute(OUTPUT_DIR, REPORT_FROM, REPORT_TO);

        System.out.println("===== 在庫分析バッチ 完了 =====");
        System.out.println("出力先：" + OUTPUT_DIR + "/");
    }
}
```

---

## 実装のヒント

### ResultSet から null を安全に取得する

`LAG()` の結果は先頭行では `null` になる。`getInt()` は `null` を `0` に変換してしまうため、
`getObject()` で受け取って `null` チェックすること。

```java
// NG：null が 0 になってしまう
int prevStock = rs.getInt("prev_stock");

// OK：null を正しく扱う
Integer prevStock = rs.getObject("prev_stock") != null
    ? rs.getInt("prev_stock")
    : null;
```

### CSV の1行目にヘッダーを書く

```java
writer.write("部品コード,部品名,種別,数量,取引日時,累積在庫数,連番");
writer.newLine();
for (StockTrend row : trends) {
    writer.write(String.join(",",
        row.getPartCode(),
        row.getPartName(),
        row.getType().equals("IN") ? "入庫" : "出庫",
        String.valueOf(row.getQuantity()),
        row.getTransactionAt(),
        String.valueOf(row.getCumulativeStock()),
        String.valueOf(row.getSeq())
    ));
    writer.newLine();
}
```

### CTE（WITH 句）を PreparedStatement で使う

```java
String sql = """
    WITH cumulative AS (
        ...
    ),
    recent AS (
        ...
    )
    SELECT ...
    FROM recent
    WHERE recent_rank = 1
    """;
PreparedStatement ps = conn.prepareStatement(sql);
```

---

## 動作確認項目

| # | 確認内容 |
|---|----------|
| 1 | バッチ実行後に `analytics_output/` に 3 つの CSV が生成される |
| 2 | レポート A の累積在庫数が正しく計算されている（SSMS で手動計算と照合） |
| 3 | レポート B の入庫・出庫ランキングが数量の多い順になっている |
| 4 | 同じ数量の部品が同じ順位になっている（RANK の動作確認） |
| 5 | レポート C でアラートレベルが正しく判定されている |
| 6 | `LAG()` の結果が `null` の行でエラーにならない |
| 7 | CSV の文字コードが正しく、Excel で開いて文字化けしない |

---

## 提出・確認方法

1. バッチを実行して 3 つの CSV が生成されることをデモする
2. SSMS でウィンドウ関数のクエリを直接実行し、CSV の数値と一致していることを確認する
3. 以下を口頭で説明できること
   - `PARTITION BY` と `GROUP BY` の違い
   - `RANK()` と `ROW_NUMBER()` の違い
   - `LAG()` で `null` になる行がどこか・なぜか
   - CTE（WITH 句）を使った理由（可読性・再利用性）
