package kassy;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by Kassy.
 * Date: 2020-03-23
 */
@RunWith(VertxUnitRunner.class)
public class MyFirstVerticleTest {

    private Vertx vertx;
    int port = 8080;

    /**
     * Vertx 인스턴스를 생성하고 MyFirstVerticle를 디플로이한다.
     * @param context
     */
    @Before
    public void setUp(TestContext context) throws IOException {

        vertx = Vertx.vertx();

        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", port)
                        .put("url", "jdbc:mysql://localhost:3306/db_kassy")
                        .put("driver_class", "com.mysql.jdbc.jdbc2.optional.MysqlDataSource")
                );

        // We pass the options as the second parameter of the deployVerticle method.
        vertx.deployVerticle(MyFirstVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }


    /**
     * vertx Web 서버 호출 TEST
     * @param context
     */
    @Test
    public void testMyApplication(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(port, "localhost", "/",
                response -> {
                    response.handler(body -> {
                        context.assertTrue(body.toString().contains("Hello"));
                        async.complete();
                    });
                });
    }

    /**
     * index.html 페이지 호출에 대한 TEST
     * @param context
     */
    @Test
    public void checkThatTheIndexPageIsServed(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/assets/index.html", response -> {
            context.assertEquals(response.statusCode(), 200);       // 응답 코드 체크
            context.assertEquals(response.headers().get("content-type"), "text/html;charset=UTF-8");    // header값 체크
            response.bodyHandler(body -> {      // body 내용 체크 => 본문을 매개변수로 받을 수 있는 bodyHandler 사용.
                context.assertTrue(body.toString().contains("My Whisky Collection"));
                async.complete();           // 비동기 해제
                });
        });
    }


    /**
     * API TEST
     * addOne (POST /api/whiskies)
     * @param context
     */
    @Test
    public void checkThatWeCanAdd(TestContext context) {
        Async async = context.async();
        final String json = Json.encodePrettily(new Whisky("Jameson", "Ireland"));      // Whisky 객체를 jsonString 값으로 변환.
        System.out.println(json);
        final String length = Integer.toString(json.length());
        vertx.createHttpClient().post(port, "localhost", "/api/whiskies")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", length)
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        System.out.println(body.toString());
                        final Whisky whisky = Json.decodeValue(body.toString(), Whisky.class);      // Json.decodeValue : response를 통해 받은 json을 Whisky 객체로 rebuild 할 수 있다.
                        context.assertEquals(whisky.getName(),"Jameson");           // 데이터 체크
                        context.assertEquals(whisky.getOrigin(), "Ireland");        // 데이터 체크
                        context.assertNotNull(whisky.getId());                          // 데이터 체크
                        async.complete();
                    });
                })
                .write(json).end();    // response handler 가 구성되기 전에는 데이터를 write할 수 없고, 마지막으로 end() 메서드를 통해 요청을 호출을 할 수 있다.

    }








}
