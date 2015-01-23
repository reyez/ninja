/**
 * Copyright (C) 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ninja;

import com.google.common.base.Optional;
import com.google.inject.Injector;
import com.google.inject.Provider;
import ninja.utils.NinjaProperties;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * => Most tests are done via class RoutesTest in project
 * ninja-servlet-integration-test.
 */
@RunWith(MockitoJUnitRunner.class)
public class RouterImplTest {

    Router router;

    @Mock
    NinjaProperties ninjaProperties;

    @Mock
    Injector injector;

    @Mock
    Provider<TestController> testControllerProvider;

    @Before
    public void before() {

        when(testControllerProvider.get()).thenReturn(new TestController());
        when(injector.getProvider(TestController.class)).thenReturn(testControllerProvider);
        router = new RouterImpl(injector, ninjaProperties);

        // add route:
        router.GET().route("/testroute").with(TestController.class, "index");
        router.GET().route("/user/{email}/{id: .*}").with(TestController.class, "user");
        router.GET().route("/user/profile/?").with(TestController.class, "userProfileWithWildChar");
        router.GET().route("/user/profile/").with(TestController.class, "userProfileWithoutWildChar");
        router.compileRoutes();
    }

    @Test
    public void testGetReverseRouteWithNoContextPathWorks() {

        String contextPath = "";
        when(ninjaProperties.getContextPath()).thenReturn(contextPath);

        String route = router.getReverseRoute(TestController.class, "index");

        assertThat(route, CoreMatchers.equalTo("/testroute"));

    }

    @Test
    public void testGetReverseRouteWhenPathContainsWildCharacter() {

        String contextPath = "";
        when(ninjaProperties.getContextPath()).thenReturn(contextPath);

        String route = router.getReverseRoute(TestController.class, "userProfileWithWildChar");

        assertThat(route, CoreMatchers.equalTo("/user/profile/"));

        route = router.getReverseRoute(TestController.class, "userProfileWithoutWildChar");

        assertThat(route, CoreMatchers.equalTo("/user/profile/"));
    }


    @Test
    public void testGetReverseRouteContextPathWorks() {

        String contextPath = "/myappcontext";
        when(ninjaProperties.getContextPath()).thenReturn(contextPath);

        String route = router.getReverseRoute(TestController.class, "index");

        assertThat(route, equalTo("/myappcontext/testroute"));

    }

    @Test
    public void testGetReverseRouteWithRegexWorks() {

        String contextPath = "";
        when(ninjaProperties.getContextPath()).thenReturn(contextPath);

        String route = router.getReverseRoute(
                TestController.class,
                "user",
                "email",
                "me@me.com",
                "id",
                10000);

        assertThat(route, equalTo("/user/me@me.com/10000"));

    }

    @Test
    public void testGetReverseRouteWithRegexAndQueryParametersWorks() {

        String contextPath = "";
        when(ninjaProperties.getContextPath()).thenReturn(contextPath);

        String route = router.getReverseRoute(
                TestController.class,
                "user",
                "email",
                "me@me.com",
                "id",
                10000,
                "q",
                "froglegs");

        assertThat(route, equalTo("/user/me@me.com/10000?q=froglegs"));

    }

    // Just a dummy TestController for mocking...
    public static class TestController {

        public Result index() {

            return Results.ok();

        }

        public Result user() {

            return Results.ok();

        }

        public Result userProfileWithoutWildChar() {

            return Results.ok();

        }

        public Result userProfileWithWildChar(){

            return Results.ok();

        }
    }

}
