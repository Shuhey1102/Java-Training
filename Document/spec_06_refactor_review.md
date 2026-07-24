# 課題仕様書 ⑥ コンソールアプリ：設計整理（パッケージ・ログ・レビュー）

**フェーズ：** Phase 2-A　**期間目安：** 2 日　**担当：** 新人

---

## 目的

課題 ③〜⑤ で動くようになったアプリのコードを「チームで読める・保守できる品質」に整える。
Git を使った PR → レビュー → マージの一連フローを体験する。

---

## 背景

コードが「動く」ことと「良いコード」であることは別物です。
今回の課題は機能追加ではなく、既存コードの品質を上げることが目的です。

チーム開発では以下が重要です：
- 誰が読んでも理解できるパッケージ構成
- 問題が起きたときに追跡できるログ
- レビューを通じた品質確認

---

## 課題内容

### 課題 6-1：パッケージ構成の整理

現在のパッケージ構成を以下の形に整理すること。

```
inventory/
├── model/          ← データを表すクラス（Part, StockTransaction）
├── dao/            ← DB アクセスのみを担当（PartDao, TransactionDao）
├── service/        ← 業務ロジックを担当（InventoryService）
├── util/           ← 汎用ユーティリティ（DbConnection, etc.）
├── exception/      ← 独自例外クラス
└── app/            ← エントリーポイント（Main）
```

各パッケージの役割：

| パッケージ | 役割 | 置くべきもの |
|---|---|---|
| `model` | データ構造 | DB やファイルと無関係な純粋なデータクラス |
| `dao` | DB アクセス | SQL を書くのはここだけ |
| `service` | 業務ロジック | 「在庫を引いて履歴を残す」などのルール |
| `util` | 汎用機能 | DB 接続など、複数箇所から使うもの |
| `exception` | 独自例外 | 業務上のエラーを表す例外クラス |
| `app` | 起動 | `main` メソッドとメニュー処理のみ |

---

### 課題 6-2：独自例外クラスの整理

現在、例外処理が統一されていない場合は以下の形に整理すること。

```java
// exception/InventoryException.java（基底例外）
public class InventoryException extends RuntimeException {
    public InventoryException(String message) {
        super(message);
    }
}

// exception/StockShortageException.java（在庫不足）
public class StockShortageException extends InventoryException {
    private final int requested;
    private final int current;

    public StockShortageException(int requested, int current) {
        super("在庫不足：要求数=" + requested + ", 現在庫=" + current);
        this.requested = requested;
        this.current = current;
    }
    // getter...
}

// exception/DuplicatePartException.java（部品コード重複）
public class DuplicatePartException extends InventoryException {
    public DuplicatePartException(String partCode) {
        super("部品コード " + partCode + " はすでに登録されています");
    }
}
```

---

### 課題 6-3：ログ出力の追加

SLF4J + Logback でログを出力するように変更すること。

**pom.xml への追加（Maven の場合）：**
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>
```

**ログ出力の方針：**

| レベル | 出力する内容 |
|---|---|
| `INFO` | 入庫・出庫・登録・更新・削除の操作完了時 |
| `WARN` | 検索して見つからなかった場合 |
| `ERROR` | 例外が発生した場合（スタックトレースも出力） |

**実装例：**
```java
// InventoryService.java の例
private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

public void stockIn(String partCode, int quantity) {
    logger.info("入庫処理開始 partCode={}, quantity={}", partCode, quantity);
    // ...処理...
    logger.info("入庫処理完了 partCode={}, newStock={}", partCode, newStock);
}
```

---

### 課題 6-4：Git で PR を作成してレビューを受ける

以下の手順で Git 操作を行うこと。

1. `main` ブランチから作業ブランチを切る
   ```
   git checkout -b feature/phase2a-refactor
   ```

2. 課題 6-1〜6-3 の変更を **意味のある単位でコミット** する
   - コミットメッセージは日本語でも英語でも良いが、「何をしたか」が分かるように書くこと
   - 例：`パッケージ構成を整理（model/dao/service/util/exception/app）`
   - NG 例：`修正`、`変更`、`update`

3. リモートリポジトリ（担当者から URL を受け取ること）に push する
   ```
   git push origin feature/phase2a-refactor
   ```

4. PR（プルリクエスト）を作成する
   - タイトル：`[Phase2-A] コンソールアプリ 設計整理`
   - 本文に以下を記載すること：
     - 変更内容の概要
     - 動作確認したこと

5. 担当者のレビューを受け、コメントがあれば修正して再 push する

6. 担当者が Approve したらマージする

---

## チェックリスト（自己確認用）

提出前に以下を自分で確認すること。

- [ ] 全パッケージが正しい役割のクラスのみを含んでいる
- [ ] `Main.java` に業務ロジックが混入していない
- [ ] `PartDao` に業務ロジック（在庫チェックなど）が混入していない
- [ ] 独自例外が適切な場所でスローされている
- [ ] ログが INFO / WARN / ERROR の適切なレベルで出力されている
- [ ] コミットメッセージが意味のある内容になっている
- [ ] アプリが正常に動作する（リファクタリング後に動かなくなっていない）

---

## 提出・確認方法

1. PR のリンクを担当者に共有する
2. レビューコメントに対して、理由を聞いても良い。「なぜそう直すのか」を理解した上で修正すること
3. マージ後、担当者から「service と dao の役割の違い」「ログレベルの使い分け」を口頭で説明できること
