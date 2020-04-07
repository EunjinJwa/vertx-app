//package kassy;
//
//import io.vertx.core.AbstractVerticle;
//import io.vertx.core.Future;
//import io.vertx.core.http.HttpServerResponse;
//import io.vertx.core.json.Json;
//import io.vertx.ext.web.Router;
//import io.vertx.ext.web.RoutingContext;
//import io.vertx.ext.web.handler.BodyHandler;
//import io.vertx.ext.web.handler.StaticHandler;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
///**
// * Created by Kassy.
// * Date: 2020-03-23
// */
//
///**
// * Verticle : verticel은 컴포넌트로서, AbstractVerticle을 확장하여 버텍스 필드에 액세스 할 수 있다.
// *
// *  * Router Object : vertx Web의 초석이다. 이 객체는 http 객체를 올바른 핸들러로 발송하는 역할을 한.
// *  * Routes : 요청이 발송되는 방법을 정의한다.
// *  * Handlers : 요청을 처리하고 결과를 작성하는 실제 액션
// *
// * vertx 웹의 모든 처리작업은 핸들러로 구현되며, 핸들러를 생성하는 create 메서드를 호출해야한다.
// */
//public class MyFirstVerticle extends AbstractVerticle {
//
//    @Override
//    public void start(Future<Void> fut) {
//
//        Router router = Router.router(vertx);
//        createSomeData();
//
//        /**
//         * TEST 호출
//         * localhost:port 를 호출했을때, 아래 문구를 출력한 html 화면을 리턴한다.
//         * */
//        router.route("/").handler(routingContext -> {
//            HttpServerResponse response = routingContext.response();
//            response
//                    .putHeader("content-type", "text/html")
//                    .end("<h1>Hello from my first Vert.x 3 application !</h1>");
//        });
//
//        /**
//         * assets/index.html 페이지를 리턴
//         * "assets/*" 로 오는 http 요청의 리소스는 assets 디렉토리에 저장되어있다는 의미.
//          */
//        router.route("/assets/*").handler(StaticHandler.create("assets"));
//
//
//        /** REST API */
//        // /api/whiskies* 아래의 모든 경로에 대한 요청 본문(request body)를 읽어들이기 위한 핸들러 생성
//        router.route("/api/whiskies*").handler(BodyHandler.create());
//
//        router.get("/api/whiskies").handler(this::getAll);
//        router.post("/api/whiskies").handler(this::addOne);
//        router.delete("/api/whiskies/:id").handler(this::deleteOne);
//
//
//        vertx.createHttpServer()
//             .requestHandler(router::accept)
//             .listen(
//                config().getInteger("http.port", 8080),
//                result -> {
//                    if (result.succeeded()) {
//                        fut.complete();
//                    } else {
//                        fut.fail(result.cause());
//                    }
//                });
//
//    }
//
//    // Store our product
//    private Map<Integer, Whisky> products = new LinkedHashMap<>();
//    // Create some product
//    private void createSomeData() {
//        Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay");
//        products.put(bowmore.getId(), bowmore);
//        Whisky talisker = new Whisky("Talisker 57° North", "Scotland, Island");
//        products.put(talisker.getId(), talisker);
//    }
//
//    /**
//     * 모든 핸들러 메서드는 RoutingContext를 받는다.
//     */
//
//
//    private void getAll(RoutingContext routingContext) {
//        routingContext.response()
//                .putHeader("content-type", "application/json; charset=utf-8")
//                .end(Json.encodePrettily(products.values()));
//    }
//
//
//
//    private void addOne(RoutingContext routingContext) {
//        final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(),
//                Whisky.class);
//        products.put(whisky.getId(), whisky);
//        routingContext.response()
//                .setStatusCode(201)
//                .putHeader("content-type", "application/json; charset=utf-8")
//                .end(Json.encodePrettily(whisky));
//    }
//
//    private void deleteOne(RoutingContext routingContext) {
//        String id = routingContext.request().getParam("id");
//        if (id == null) {
//            routingContext.response().setStatusCode(400).end();
//        } else {
//            Integer idAsInteger = Integer.valueOf(id);
//            products.remove(idAsInteger);
//        }
//        routingContext.response().setStatusCode(204).end();
//    }
//
//}
