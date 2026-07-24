# 課題仕様書 ④ コンソールアプリ：入出庫トランザクション + ファイル I/O

**フェーズ：** Phase 2-A　**期間目安：** 3 日　**担当：** 新人

---

## 目的

課題 ③ で作ったアプリに「入出庫の記録」と「ファイル永続化」を追加する。
毎回起動するたびにデータがリセットされる問題を解消し、CSV ファイルで状態を保持する。

---

## 背景（業務イメージ）

前回の課題でメモリ上のデータ管理ができるようになりました。
しかし、アプリを終了するたびにデータが消えてしまうのは実務では使えません。

今回は：
- 部品マスタを CSV ファイルから読み込む
- 入庫・出庫の操作をするたびに履歴を CSV に記録する

を追加します。

---

## 追加・変更するファイル

```
training/
└── src/
    └── inventory/
        ├── model/
        │   ├── Part.java                  ← 変更なし（または微修正）
        │   └── StockTransaction.java      ← 新規追加
        ├── service/
        │   ├── InventoryService.java      ← 変更（入出庫メソッド追加）
        │   └── FileService.java           ← 新規追加（CSV 読み書き）
        └── app/
            └── Main.java                  ← 変更（メニュー追加）

data/                                      ← プロジェクト直下に作成
├── parts.csv                              ← 部品マスタ
└── transactions.csv                       ← 入出庫履歴
```

---

## 追加するクラス・機能

### StockTransaction クラス（`model/StockTransaction.java`）

入出庫の 1 件を表すクラス。

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `transactionId` | `String` | 取引 ID（自動採番） | `TX0001` |
| `partCode` | `String` | 部品コード | `P001` |
| `type` | `String` | 種別（`"IN"` または `"OUT"`） | `IN` |
| `quantity` | `int` | 数量（1 以上） | `50` |
| `dateTime` | `String` | 日時（`yyyy-MM-dd HH:mm:ss`） | `2025-04-01 09:00:00` |

実装すること：
- 全フィールドを引数に持つコンストラクタ
- 各フィールドの getter
- `toString()` メソッド

---

### FileService クラス（`service/FileService.java`）

CSV ファイルの読み書きを担当するクラス。

| メソッド | 説明 |
|---|---|
| `List<Part> loadParts(String filePath)` | CSV から部品一覧を読み込んで返す |
| `void saveParts(String filePath, List<Part> parts)` | 部品一覧を CSV に書き出す（上書き） |
| `void appendTransaction(String filePath, StockTransaction tx)` | 入出庫履歴を CSV に追記する |
| `List<StockTransaction> loadTransactions(String filePath)` | CSV から入出庫履歴一覧を読み込んで返す |

---

### InventoryService クラスへの追加

以下のメソッドを追加すること。

| メソッド | 説明 |
|---|---|
| `void stockIn(String partCode, int quantity)` | 入庫処理。在庫数を増やし、`StockTransaction`（type=`IN`）を生成して `FileService` 経由で CSV に追記する |
| `void stockOut(String partCode, int quantity)` | 出庫処理。在庫数を減らし、`StockTransaction`（type=`OUT`）を生成して CSV に追記する。在庫不足の場合は例外をスロー |
| `List<StockTransaction> getTransactionHistory()` | 入出庫履歴一覧を返す |

---

### Main クラスへの追加メニュー

```
===== 部品在庫管理システム =====
1. 部品一覧表示
2. 部品登録
3. 在庫数更新
4. 部品削除
5. 部品検索
6. 入庫処理        ← 追加
7. 出庫処理        ← 追加
8. 入出庫履歴表示  ← 追加
0. 終了
>
```

#### 6. 入庫処理
```
--- 入庫処理 ---
部品コード: P001
入庫数量: 50
入庫しました。（P001 在庫：100 → 150）
```

#### 7. 出庫処理
```
--- 出庫処理 ---
部品コード: P001
出庫数量: 30
出庫しました。（P001 在庫：150 → 120）
```

エラー例（在庫不足の場合）：
```
エラー：在庫不足です。（要求数：200, 現在庫：120）
```

#### 8. 入出庫履歴表示
```
===== 入出庫履歴 =====
取引ID    部品コード  種別  数量  日時
TX0001   P001       IN    50   2025-04-01 09:00:00
TX0002   P001       OUT   30   2025-04-01 10:30:00
TX0003   P002       IN   100   2025-04-01 11:00:00
```

---

## CSV フォーマット

### parts.csv（部品マスタ）

```csv
partCode,partName,stock,warehouseCode
P001,ボルト M6,100,WH01
P002,ナット M6,50,WH01
P003,ワッシャー M6,200,WH02
```

- 1 行目はヘッダー行（読み込み時はスキップすること）
- アプリ起動時に読み込み、終了時または更新時に書き出す

### transactions.csv（入出庫履歴）

```csv
transactionId,partCode,type,quantity,dateTime
TX0001,P001,IN,50,2025-04-01 09:00:00
TX0002,P001,OUT,30,2025-04-01 10:30:00
```

- 追記モードで書き出すこと（過去の履歴を消さない）

---

## 業務ルール

| ルール | 内容 |
|---|---|
| 出庫は在庫以上の数量は不可 | 出庫数 > 在庫数の場合は例外をスロー |
| 数量は 1 以上 | 0 以下の入出庫数はエラー |
| ファイルが存在しない場合 | 部品マスタは空リストとして扱う（初回起動を想定）。履歴は新規作成する |
| CSV のフォーマット不正 | 読み込み中にフォーマット不正な行があった場合、その行をスキップしてログに出力する |

---

## 提出・確認方法

1. アプリを起動→入出庫操作→終了→再起動してデータが残っていることをデモする
2. 在庫不足エラーのデモをする
3. `transactions.csv` が追記されていることをテキストエディタで確認する
4. 担当者から「入庫と出庫で在庫数がどのように変わるか」「CSV のどのタイミングで書き込まれるか」を口頭で説明できること
