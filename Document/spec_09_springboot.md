# 課題仕様書 ⑨ Web アプリ：Spring Boot へ移行・比較学習

**フェーズ：** Phase 2-B　**期間目安：** 5〜6 日　**担当：** 新人

---

## 目的

課題 ⑦⑧ で Jakarta EE（Servlet + JSP）で作った「部品在庫管理」の **同じ機能** を Spring Boot で再実装する。
2 つの実装を比較することで、「フレームワークが何を肩代わりしているか」を体感する。

---

## 背景

Jakarta EE では以下を自分で書く必要がありました：

- `web.xml` でのルーティング設定
- `request.getParameter()` でのパラメータ取得
- `DriverManager.getConnection()` での接続管理
- トランザクションの `commit()` / `rollback()` の明示的な呼び出し

Spring Boot では **これらの多くを自動化・省略** できます。
実装を終えたら、「何が消えたか」を整理して口頭で説明できるようにしてください。

---

## プロジェクト作成

[Spring Initializr](https://start.spring.io/) で以下の設定でプロジェクトを生成すること。

| 項目 | 設定値 |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.x.x（最新安定版） |
| Group | `com.example` |
| Artifact | `inventory-springboot` |
| Packaging | Jar |
| Java | 17 |
| Dependencies | Spring Web, Thymeleaf, JDBC API, MS SQL Server Driver |

---

## プロジェクト構成

```
05_project_inventory_system/          ← Spring Boot プロジェクトルート
├── src/main/java/com/example/inventory/
├── model/
│   ├── Part.java
│   └── StockTransaction.java
├── dao/
│   ├── PartDao.java          ← JdbcTemplate を使う版に書き換え
│   └── TransactionDao.java
├── service/
│   └── InventoryService.java ← トランザクション管理を Spring に委譲
├── exception/
│   ├── InventoryException.java
│   ├── StockShortageException.java
│   └── DuplicatePartException.java
└── controller/               ← Servlet の代替
    ├── PartController.java
    └── StockController.java

└── src/main/resources/
    ├── application.properties    ← DB 接続設定・アプリ設定
    └── templates/                ← JSP の代替（Thymeleaf）
        ├── part/
        │   ├── list.html
        │   └── add.html
        └── stock/
            ├── in.html
            ├── out.html
            └── history.html
```

---

## 各クラスの実装

### application.properties（DB 接続設定）

```properties
spring.datasource.url=jdbc:sqlserver://localhost;instanceName=MSSQLLocalDB;databaseName=InventoryTraining;integratedSecurity=true;encrypt=false
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

> LocalDB は Windows 認証で接続するため、`username` / `password` は不要。課題 ⑤ で作成した `InventoryTraining` データベース・テーブルをそのまま使う。

> `DbConnection.java` は不要になる。Spring がコネクションを管理してくれる。

---

### PartDao クラスの書き換え（JdbcTemplate 版）

Jakarta EE 版と Spring Boot 版の比較：

**Jakarta EE 版（今まで）：**
```java
public List<Part> findAll() {
    List<Part> list = new ArrayList<>();
    String sql = "SELECT * FROM parts";
    try (Connection conn = DbConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            list.add(new Part(rs.getString("part_code"), ...));
        }
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
    return list;
}
```

**Spring Boot 版（JdbcTemplate）：**
```java
@Repository
public class PartDao {

    private final JdbcTemplate jdbcTemplate;

    public PartDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Part> findAll() {
        String sql = "SELECT * FROM parts";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Part(
            rs.getString("part_code"),
            rs.getString("part_name"),
            rs.getInt("stock"),
            rs.getString("warehouse_code")
        ));
    }
}
```

> 接続・クローズ・例外変換を JdbcTemplate が代わりにやってくれる。

---

### InventoryService クラスのトランザクション管理

**Jakarta EE 版（今まで）：**
```java
conn.setAutoCommit(false);
try {
    partDao.updateStock(conn, partCode, newStock);
    transactionDao.insert(conn, tx);
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
}
```

**Spring Boot 版（@Transactional）：**
```java
@Service
public class InventoryService {

    @Transactional  // これだけでトランザクション制御してくれる
    public void stockIn(String partCode, int quantity) {
        partDao.updateStock(partCode, newStock);
        transactionDao.insert(tx);
        // 例外が発生したら自動でロールバックされる
    }
}
```

---

### PartController クラス（Servlet の代替）

```java
@Controller
@RequestMapping("/parts")
public class PartController {

    private final InventoryService inventoryService;

    public PartController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // 一覧表示
    @GetMapping
    public String list(Model model) {
        model.addAttribute("parts", inventoryService.findAll());
        return "part/list";  // → templates/part/list.html
    }

    // 登録フォーム表示
    @GetMapping("/add")
    public String addForm() {
        return "part/add";
    }

    // 登録処理
    @PostMapping("/add")
    public String add(@RequestParam String partCode,
                      @RequestParam String partName,
                      @RequestParam int stock,
                      @RequestParam String warehouseCode,
                      RedirectAttributes redirectAttributes) {
        inventoryService.add(new Part(partCode, partName, stock, warehouseCode));
        redirectAttributes.addFlashAttribute("message", "登録しました");
        return "redirect:/parts";
    }
}
```

---

### Thymeleaf テンプレート（JSP の代替）

**JSP 版（今まで）：**
```jsp
<c:forEach var="part" items="${parts}">
    <tr>
        <td><c:out value="${part.partCode}"/></td>
        <td><c:out value="${part.partName}"/></td>
    </tr>
</c:forEach>
```

**Thymeleaf 版：**
```html
<tr th:each="part : ${parts}">
    <td th:text="${part.partCode}"></td>
    <td th:text="${part.partName}"></td>
</tr>
```

---

## 実装する機能

Jakarta EE で作った以下の機能をすべて Spring Boot で再実装すること。

| # | 機能 |
|---|---|
| 1 | 部品一覧表示 |
| 2 | 部品登録（バリデーションあり） |
| 3 | 在庫数更新 |
| 4 | 部品削除 |
| 5 | 入庫処理 |
| 6 | 出庫処理（在庫不足エラー） |
| 7 | 入出庫履歴一覧（絞り込み） |

---

## 比較整理シート（提出必須）

実装完了後、以下の表を自分で埋めて担当者に提出すること。

| 比較項目 | Jakarta EE | Spring Boot | 変わったこと |
|---|---|---|---|
| ルーティング設定 | `web.xml` | | |
| リクエストパラメータ取得 | `request.getParameter()` | | |
| DB 接続管理 | `DbConnection.getConnection()` | | |
| トランザクション制御 | `conn.commit()` / `rollback()` | | |
| HTML テンプレート | JSP + JSTL | | |
| 接続クローズ | try-finally | | |
| サーバー起動 | Tomcat（外部） | | |

---

## 動作確認項目

| # | 確認内容 |
|---|----------|
| 1 | `mvn spring-boot:run` でアプリが起動する |
| 2 | Jakarta EE 版と同じ機能がすべて動作する |
| 3 | 在庫不足エラーが画面に表示される |
| 4 | `@Transactional` でロールバックが動作する（出庫失敗時に在庫変化なし） |

---

## 提出・確認方法

1. ブラウザでの動作デモをする
2. 比較整理シートを担当者に見せながら説明する
3. 担当者から「`@Transactional` は何をしているか」「`@Repository` / `@Service` / `@Controller` の違い」「JdbcTemplate を使うと何が嬉しいか」を口頭で説明できること