package kassy;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Kassy.
 * Date: 2020-04-03
 */

/**
 * MyFirstVerticle 클래스의 JDBC 버
 */
public class MyFirstVerticle extends AbstractVerticle {

    JDBCClient jdbc;

    @Override
    public void start(Future<Void> fut) {

        // Create a JDBC client
        // JDBCClient.createShared 여기서 계속 빌드실패남. 이유를 모르겠음..!
        jdbc = JDBCClient.createShared(vertx, config());

        startBackend(
        (connection) -> createSomeData(connection,(nothing) -> startWebApp(
                        (http) -> completeStartup(http, fut)
                ), fut
        ), fut);


    }


    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        Router router = Router.router(vertx);

        /**
         * TEST 호출
         * localhost:port 를 호출했을때, 아래 문구를 출력한 html 화면을 리턴한다.
         * */
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert.x 3 application !</h1>");
        });

        /**
         * assets/index.html 페이지를 리턴
         * "assets/*" 로 오는 http 요청의 리소스는 assets 디렉토리에 저장되어있다는 의미.
         */
        router.route("/assets/*").handler(StaticHandler.create("assets"));


        /** REST API */
        // /api/whiskies* 아래의 모든 경로에 대한 요청 본문(request body)를 읽어들이기 위한 핸들러 생성
        router.route("/api/whiskies*").handler(BodyHandler.create());

        router.get("/api/whiskies").handler(this::getAll);
//        router.post("/api/whiskies").handler(this::addOne);
//        router.delete("/api/whiskies/:id").handler(this::deleteOne);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port", 8080),
                        next::handle);      // next::handle => completeStartup(http, fut)
    }


    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
        } else {
            fut.fail(http.cause());
        }
    }



    // Store our product
    private Map<Integer, Whisky> products = new LinkedHashMap<>();


    /**
     * 1. SQL connection
     * 2. Table 생성
     * 3. nextHandler인 insert 호출
     * @param result
     * @param next
     * @param fut
     */
    private void createSomeData(AsyncResult<SQLConnection> result,
                                Handler<AsyncResult<Void>> next, Future<Void> fut) {
        if (result.failed()) {
            fut.fail(result.cause());
        } else {
            SQLConnection connection = result.result();
            connection.execute(
                    "CREATE TABLE IF NOT EXISTS Whisky (id INTEGER IDENTITY, name varchar(100), " +
                            "origin varchar(100))",
                    ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                            connection.close();
                            return;
                        }
                        connection.query("SELECT * FROM Whisky", select -> {
                            if (select.failed()) {
                                fut.fail(ar.cause());
                                connection.close();
                                return;
                            }
                            if (select.result().getNumRows() == 0) {
                                insert(
                                        new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay"),
                                        connection,
                                        (v) -> insert(new Whisky("Talisker 57° North", "Scotland, Island"),
                                                connection,
                                                (r) -> {
                                                    next.handle(Future.<Void>succeededFuture());
                                                    connection.close();
                                                }));
                            } else {
                                next.handle(Future.<Void>succeededFuture());
                                connection.close();
                            }
                        });
                    });
        }
    }

    /**
     * 모든 핸들러 메서드는 RoutingContext를 받는다.
     */


    private void insert(Whisky whisky, SQLConnection connection, Handler<AsyncResult<Whisky>> next) {
        String sql = "INSERT INTO Whisky (name, origin) VALUES ?, ?";
        connection.updateWithParams(sql,
                new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),
                (ar) -> {
                    if (ar.failed()) {
                        next.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    UpdateResult result = ar.result();
                    // Build a new whisky instance with the generated id.
                    Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
                    next.handle(Future.succeededFuture(w));
                });
    }

    private void getAll(RoutingContext routingContext) {
        jdbc.getConnection(ar -> {
            SQLConnection connection = ar.result();
            connection.query("SELECT * FROM Whisky", result -> {
                List<Whisky> whiskies = result.result().getRows().stream().map(Whisky::new).collect(Collectors.toList());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(whiskies));
                connection.close(); // Close the connection
            });
        });
    }


    private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
        Handler<AsyncResult<SQLConnection>> test = (jdbc) -> {
            if (jdbc.failed()) {
                fut.fail(jdbc.cause());
            } else {
                next.handle(Future.succeededFuture(jdbc.result()));
            }
        };

        jdbc.getConnection(test);
    }



}
